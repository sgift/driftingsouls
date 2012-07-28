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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
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
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Laesst alle Tanker (solchen Schiffen mit einem <code>deutfactor</code> &gt; 0) Deuterium
 * sammeln, sofern diese in dem Moment in der Lage dazu sind.
 * @author Christopher Jung
 *
 */
@Module(name="deutall")
public class DeutAllController extends TemplateGenerator {

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public DeutAllController(Context context) {
		super(context);

		setTemplate("deutall.html");

		setPageTitle("Deut. sammeln");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}

	/**
	 * Sammelt das Deuterium auf den Tankern.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();
		org.hibernate.Session db = getDB();

		Location lastcoords = null;

		t.setBlock("_DEUTALL", "ships.listitem", "ships.list");

		List<?> ships = db.createQuery("from Ship as s left join s.modules m " +
				"where s.id>0 and s.owner=:user and (s.shiptype.deutFactor>0 or m.deutFactor>0) " +
				"order by s.system,s.x,s.y")
				.setEntity("user", user)
				.list();

		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			t.start_record();
			Ship ship = (Ship)iter.next();

			ShipTypeData shiptype = ship.getTypeData();
			if( shiptype.getDeutFactor() == 0 ) {
				continue;
			}

			if( (lastcoords == null) || !lastcoords.sameSector(0, ship.getLocation(), 0) ) {
				t.setVar(	"ship.newcoords",		1,
							"ship.location",		ship.getLocation().displayCoordinates(false),
							"ship.newcoords.break",	lastcoords != null );

				lastcoords = ship.getLocation();
			}

			t.setVar(	"ship.id",		ship.getId(),
						"ship.name",	Common._plaintitle(ship.getName()) );

			long e = ship.getEnergy();
			if( e <= 0 ) {
				t.setVar(	"ship.message",			"Keine Energie",
							"ship.message.color",	"red" );
			}
			else if( ship.getCrew() < (shiptype.getCrew()/2) ) {
				t.setVar(	"ship.message",			"Nicht genug Crew",
							"ship.message.color",	"red" );
			}
			else {
				Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(ship));

				if( (nebel != null) && (nebel.getType() < 3) ) {
					Cargo shipCargo = ship.getCargo();
					long cargo = shipCargo.getMass();

					int deutfactor = shiptype.getDeutFactor();
					if( nebel.getType() == 1 ) {
						deutfactor--;
					}
					else if( nebel.getType() == 2 ) {
						deutfactor++;
					}

					if( (e * deutfactor)*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ) > (shiptype.getCargo() - cargo) ) {
						e = (shiptype.getCargo()-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));

						t.setVar(	"ship.message",			"Kein Platz mehr im Frachtraum",
									"ship.message.color",	"#FF4444" );
					}

					long saugdeut = e * deutfactor;

					t.setVar(	"ship.saugdeut",	saugdeut,
								"deuterium.image",	Cargo.getResourceImage(Resources.DEUTERIUM) );
					shipCargo.addResource( Resources.DEUTERIUM, saugdeut );

					ship.setEnergy((int)(ship.getEnergy()-e));
					ship.setCargo(shipCargo);

					ship.recalculateShipStatus();
				}
				else {
					t.setVar(	"ship.message",			"Kein Nebel",
								"ship.message.color",	"red" );
				}
			}
			t.parse("ships.list", "ships.listitem", true);

			t.stop_record();
			t.clear_record();
		}
	}
}
