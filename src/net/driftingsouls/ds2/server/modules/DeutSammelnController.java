/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sammelt mit einem Tanker in einem Nebel Deuterium.
 *
 * @author Christopher Jung
 * @urlparam Integer ship Die ID des Tankers
 */
@Module(name="deutsammeln")
public class DeutSammelnController extends TemplateGenerator {
	private List<Ship> ships = null;
	private Nebel nebel = null;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public DeutSammelnController(Context context) {
		super(context);

		setTemplate("deutsammeln.html");

		parameterNumber("ship");
		parameterNumber("fleet");

		setPageTitle("Deut. sammeln");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();

		List<Ship> ships = new ArrayList<Ship>();
		int shipID = getInteger("ship");
		int fleetId = getInteger("fleet");

		if( fleetId == 0 )
		{
			Ship ship = (Ship)db.get(Ship.class, shipID);
			if( (ship == null) || (ship.getOwner() != user) ) {
				addError("Das angegebene Schiff existiert nicht", Common.buildUrl("default", "module", "schiffe") );

				return false;
			}

			ships.add(ship);
		}
		else
		{
			ShipFleet fleet = (ShipFleet)db.get(ShipFleet.class, fleetId);
			if( fleet == null || fleet.getOwner() != user )
			{
				addError("Die angegebene Flotte existiert nicht", Common.buildUrl("default", "module", "schiffe") );

				return false;
			}
			ships.addAll(fleet.getShips());
		}

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ships.get(0).getId());

		Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(ships.get(0)));
		if( nebel == null ) {
			addError("Der Nebel befindet sich nicht im selben Sektor wie das Schiff", errorurl );

			return false;
		}
		if( !nebel.getType().isDeuteriumNebel() )  {
			addError("In diesem Nebel k&ouml;nnen sie kein Deuterium sammeln", errorurl );

			return false;
		}

		String lastError = null;
		for( Iterator<Ship> iter=ships.iterator(); iter.hasNext();)
		{
			Ship ship = iter.next();
			if( !nebel.getLocation().sameSector(0, ship, 0) ) {
				lastError = "Der Nebel befindet sich nicht im selben Sektor wie das Schiff";
				iter.remove();
				continue;
			}

			ShipTypeData shiptype = ship.getTypeData();
			if( shiptype.getDeutFactor() <= 0 )  {
				lastError = "Dieser Schiffstyp kann kein Deuterium sammeln";
				iter.remove();
				continue;
			}

			if( ship.getCrew() < (shiptype.getCrew()/2) ) {
				lastError = "Sie haben nicht genug Crew um Deuterium zu sammeln";
				iter.remove();
			}
		}

		if( ships.isEmpty() )
		{
			addError(lastError, errorurl);
			return false;
		}

		this.ships = ships;
		this.nebel = nebel;

		return true;
	}

	/**
	 * Sammelnt fuer eine angegebene Menge Energie Deuterium aus einem Nebel.
	 * @urlparam Integer e Die Menge Energie, fuer die Deuterium gesammelt werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void sammelnAction() {
		TemplateEngine t = getTemplateEngine();

		parameterNumber("e");
		long e = getInteger("e");

		String message = "";

		for( Ship ship : this.ships )
		{
			message += Common._plaintitle(ship.getName())+" ("+ship.getId()+"): ";

			long saugdeut = ship.sammelDeuterium(nebel, e);
			if( saugdeut <= 0 )
			{
				message += "Es konnte kein weiteres Deuterium gesammelt werden<br />";
			}
			else {
				message += "<img src=\""+Cargo.getResourceImage(Resources.DEUTERIUM)+"\" alt=\"\" />"+saugdeut+
					" f&uuml;r <img src=\"./data/interface/energie.gif\" alt=\"Energie\" />"+e+
					" gesammelt<br />";
			}
		}

		t.setVar("deutsammeln.message", message);

		redirect();
	}

	/**
	 * Zeigt eine Eingabemaske an, in der angegeben werden kann,
	 * fuer wieviel Energie Deuterium gesammelt werden soll.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();

		int deutfactorSum = 0;
		int maxE = 0;
		for( Ship ship : ships )
		{
			long deutfactor = ship.getTypeData().getDeutFactor();
			deutfactor = nebel.getType().modifiziereDeutFaktor(deutfactor);
			deutfactorSum += deutfactor;
			if( maxE  < ship.getEnergy() )
			{
				maxE = ship.getEnergy();
			}
		}

		t.setVar(	"deuterium.image",		Cargo.getResourceImage(Resources.DEUTERIUM),
					"ship.type.deutfactor",	Common.ln(deutfactorSum/(double)ships.size()),
					"ship.id",				this.ships.get(0).getId(),
					"fleet.id",				this.ships.size() > 1 ? this.ships.get(0).getFleet().getId() : 0,
					"ship.e",				maxE );
	}
}
