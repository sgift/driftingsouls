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
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Sammelt mit einem Tanker in einem Nebel Deuterium.
 *
 * @author Christopher Jung
 * @urlparam Integer ship Die ID des Tankers
 */
@Configurable
@Module(name="deutsammeln")
public class DeutSammelnController extends TemplateGenerator {
	private Ship ship = null;
	private Nebel nebel = null;
	private ShipTypeData shiptype = null;

	private Configuration config;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public DeutSammelnController(Context context) {
		super(context);

		setTemplate("deutsammeln.html");

		parameterNumber("ship");

		setPageTitle("Deut. sammeln");
	}

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

	@Override
	protected boolean validateAndPrepare(String action) {
		org.hibernate.Session db = getDB();
		User user = (User)getUser();

		int shipID = getInteger("ship");

		Ship ship = (Ship)db.get(Ship.class, shipID);
		if( (ship == null) || (ship.getOwner() != user) ) {
			addError("Das angegebene Schiff existiert nicht", Common.buildUrl("default", "module", "schiffe") );

			return false;
		}

		ShipTypeData shiptype = ship.getTypeData();

		Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(ship));

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", shipID);

		if( nebel == null || !nebel.getLocation().sameSector(0, ship, 0) ) {
			addError("Der Nebel befindet sich nicht im selben Sektor wie das Schiff", errorurl );

			return false;
		}

		if( nebel.getType() > 2 )  {
			addError("In diesem Nebel k&ouml;nnen sie kein Deuterium sammeln", errorurl );

			return false;
		}

		if( shiptype.getDeutFactor() <= 0 )  {
			addError("Dieser Schiffstyp kann kein Deuterium sammeln", errorurl );

			return false;
		}

		if( ship.getCrew() < (shiptype.getCrew()/2) ) {
			addError("Sie haben nicht genug Crew um Deuterium zu sammeln", errorurl );

			return false;
		}

		this.ship = ship;
		this.nebel = nebel;
		this.shiptype = shiptype;

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

		if( e > ship.getEnergy() ) {
			e = ship.getEnergy();
		}
		Cargo shipCargo = ship.getCargo();
		long cargo = shipCargo.getMass();

		long deutfactor = shiptype.getDeutFactor();
		if( nebel.getType() == 1 ) {
			deutfactor--;
		}
		else if( nebel.getType() == 2 ) {
			deutfactor++;
		}

		String message = "";

		if( (e * deutfactor)*Cargo.getResourceMass(Resources.DEUTERIUM, 1) > (shiptype.getCargo() - cargo) ) {
			e = (shiptype.getCargo()-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));
			message += "Kein Platz mehr im Frachtraum<br />";
		}

		long saugdeut = e * deutfactor;

		message += "<img src=\""+Cargo.getResourceImage(Resources.DEUTERIUM)+"\" alt=\"\" />"+saugdeut+" f&uuml;r <img src=\""+config.get("URL")+"data/interface/energie.gif\" alt=\"Energie\" />"+e+" gesammelt<br />";

		if( saugdeut > 0 ) {
			shipCargo.addResource( Resources.DEUTERIUM, saugdeut );

			ship.setEnergy((int)(ship.getEnergy()-e));
			ship.setCargo(shipCargo);
			ship.recalculateShipStatus();
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

		int deutfactor = shiptype.getDeutFactor();
		if( nebel.getType() == 1 ) {
			deutfactor--;
		}
		else if( nebel.getType() == 2 ) {
			deutfactor++;
		}

		t.setVar(	"deuterium.image",		Cargo.getResourceImage(Resources.DEUTERIUM),
					"ship.type.deutfactor",	deutfactor,
					"ship.id",				ship.getId(),
					"ship.e",				ship.getEnergy() );
	}
}
