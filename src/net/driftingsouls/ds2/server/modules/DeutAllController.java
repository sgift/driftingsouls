package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Laesst alle Tanker (solchen Schiffen mit einem <code>deutfactor</code> &gt; 0) Deuterium
 * sammeln, sofern diese in dem Moment in der Lage dazu sind
 * @author Christopher Jung
 *
 */
public class DeutAllController extends DSGenerator {

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public DeutAllController(Context context) {
		super(context);
		
		setTemplate("deutall.html");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		return true;
	}
	
	/**
	 * Sammelt das Deuterium auf den Tankern
	 */
	@Override
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		Location lastcoords = null;
		
		t.set_block("_DEUTALL", "ships.listitem", "ships.list");

		SQLQuery shipRow = db.query("SELECT t1.id,t1.x,t1.y,t1.system,t1.e,t1.name,t1.crew,t1.cargo,t1.type,t1.status " ,
								"FROM ships AS t1,ship_types AS t2 " ,
								"WHERE t1.id>0 AND t1.owner=",user.getID()," AND t1.type=t2.id AND (t2.deutfactor>0 OR LOCATE('tblmodules',t1.status)) ORDER BY t1.system,t1.x,t1.y");
		
		while( shipRow.next() ) {
			t.start_record();
			SQLResultRow ship = shipRow.getRow();
			
			SQLResultRow shiptype = Ships.getShipType( ship );
			if( shiptype.getInt("deutfactor") == 0 ) {
				continue;	
			}
	
			if( (lastcoords == null) || !lastcoords.sameSector(0, Location.fromResult(ship), 0) ) {
				t.set_var(	"ship.newcoords",		1,
							"ship.location",		Ships.getLocationText(ship, false),
							"ship.newcoords.break",	lastcoords != null );

				lastcoords = Location.fromResult(ship);
			}
			
			t.set_var(	"ship.id",		ship.getInt("id"),
						"ship.name",	Common._plaintitle(ship.getString("name")) );
			
			long e = ship.getInt("e");
			if( e <= 0 ) {
				t.set_var(	"ship.message",			"Keine Energie",
							"ship.message.color",	"red" );
			}
			else if( ship.getInt("crew") < (shiptype.getInt("crew")/2) ) {
				t.set_var(	"ship.message",			"Nicht genug Crew",
							"ship.message.color",	"red" );
			}
			else {
				SQLResultRow nebel = db.first("SELECT id,type FROM nebel WHERE x=",ship.getInt("x")," AND y=",ship.getInt("y")," AND system=",ship.getInt("system")," AND type<3" );
	
				if( !nebel.isEmpty() ) {
					Cargo shipCargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
					long cargo = shipCargo.getMass();
					
					int deutfactor = shiptype.getInt("deutfactor");
					if( nebel.getInt("type") == 1 ) {
						deutfactor--;
					}
					else if( nebel.getInt("type") == 2 ) {
						deutfactor++;
					}
	
					if( (e * deutfactor)*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ) > (shiptype.getLong("cargo") - cargo) ) {
						e = (shiptype.getLong("cargo")-cargo)/(deutfactor*Cargo.getResourceMass( Resources.DEUTERIUM, 1 ));
							
						t.set_var(	"ship.message",			"Kein Platz mehr im Frachtraum",
									"ship.message.color",	"#FF4444" );
					}
						
					long saugdeut = e * deutfactor;
						
					t.set_var(	"ship.saugdeut",	saugdeut,
								"deuterium.image",	Cargo.getResourceImage(Resources.DEUTERIUM) );
					shipCargo.addResource( Resources.DEUTERIUM, saugdeut );
						
					db.update("UPDATE ships SET e=",ship.getInt("e")-e,",cargo='",shipCargo.save(),"' WHERE id>0 AND id=",ship.getInt("id")," AND cargo='",shipCargo.save(true),"' AND e=",ship.getInt("e"));
					
					Ships.recalculateShipStatus(ship.getInt("id"));
				} 
				else {
					t.set_var(	"ship.message",			"Kein Nebel",
								"ship.message.color",	"red" );
				}
				
				t.parse("ships.list", "ships.listitem", true);
				
				t.stop_record();
				t.clear_record();
			}
		}
		shipRow.free();
	}
}
