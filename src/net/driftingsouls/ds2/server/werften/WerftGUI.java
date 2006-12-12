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
package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Die GUI einer Werft
 * @author bktheg
 *
 */
public class WerftGUI {
	private Context context;
	private TemplateEngine t;
	
	/**
	 * Erstellt eine neue Instanz einer Werftgui auf Basis des Kontexts
	 * @param context Der Kontext
	 * @param t Das zu verwendende TemplateEngine
	 */
	public WerftGUI( Context context,TemplateEngine t ) {
		this.context = context;
		this.t = t;
	}
	
	/**
	 * Generiert die Werft-GUI fuer das angegebene Werftobjekt
	 * @param werft Das Werftobjekt
	 * @return Die GUI als String
	 */
	public String execute( WerftObject werft ) {
		Database db = context.getDatabase();
	
		int build = context.getRequest().getParameterInt("build");
		String conf = context.getRequest().getParameterString("conf");
		int ws = context.getRequest().getParameterInt("ws");
		int item = context.getRequest().getParameterInt("item");
		
		if( !t.set_file( "_WERFT.WERFTGUI", "werft.werftgui.html" ) ) {
			context.addError("Konnte das Template-Engine nicht initialisieren");
			return "";
		}
		t.set_var(	"werftgui.formhidden",	werft.getFormHidden(),
					"werftgui.urlbase",		werft.getUrlBase() );
		
		if( build != 0 ) {
			if( werft.isBuilding() ) {
				t.set_var("werftgui.msg", "Sie k&ouml;nnen nur ein Schiff zur selben Zeit bauen");
			} 
			else {
				this.out_buildShip(build, item, werft, conf);
			}
		}
		else if( ws != 0 ) {
			if( werft.isBuilding() ) {
				t.set_var("werftgui.msg", "Sie k&ouml;nnen nicht gleichzeitig ein Schiff der Werkstatt bearbeiten und eines bauen");
			}
			else {
				this.out_ws(werft, ws);
			}
		}
		else if( werft.isBuilding() ) {
			this.out_werftbuilding( werft, conf );
		} 
		else if( !werft.isBuilding() ) {
			//Resourcenliste
			this.out_ResourceList( werft );
		
			//Schiffsliste
			this.out_buildShipList( werft );
		
			this.out_wsShipList(werft);
		
			// Verbindung Base <-> Werft
			if( werft.getWerftType() == WerftObject.SHIP ) {
				int shipid = ((ShipWerft)werft).getShipID();
				int linkedbaseID = ((ShipWerft)werft).getLinkedBase();
				
				SQLResultRow shiptype = Ships.getShipType( shipid, true );
				if( shiptype.getInt("cost") == 0 ) {
					t.set_block("_WERFT.WERFTGUI", "werftgui.linkedbase.listitem", "werftgui.linkedbase.list");
		
					t.set_var(	"linkedbase.selected",	linkedbaseID == 0,
								"linkedbase.value",		"-1",
								"linkedbase.name",		(linkedbaseID != 0 ? "kein " : "")+"Ziel" );
										
					t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
		
					SQLQuery base = db.query("SELECT id,name FROM bases ",
								"WHERE x=",werft.getX()," AND y=",werft.getY()," AND system=",werft.getSystem()," AND owner=",werft.getOwner()," ORDER BY id");
					while( base.next() ) {
						t.set_var(	"linkedbase.selected",	(linkedbaseID == base.getInt("id")),
									"linkedbase.value",		base.getInt("id"),
									"linkedbase.name",		base.getString("name")+" ("+base.getInt("id")+")" );
						t.parse("werftgui.linkedbase.list", "werftgui.linkedbase.listitem", true);
					}
					base.free();
				}
			}
		}
		t.parse( "OUT", "_WERFT.WERFTGUI" );	
		return t.getVar("OUT");
	}

	private void out_wsShipList(WerftObject werft) {
		Database db = context.getDatabase();

		t.set_var("werftgui.wsshiplist", 1);
		t.set_block("_WERFT.WERFTGUI", "wsshiplist.listitem", "wsshiplist.list");
		
		SQLQuery ship = db.query("SELECT t1.id,t1.name,t1.type,t1.status,t1.engine,t1.sensors,t1.comm,t1.weapons,t1.hull,t1.owner,t2.name AS ownername,t2.id AS userid ",
								"FROM ships t1 JOIN users t2 ON t1.owner=t2.id ",
								"WHERE t1.id>0 AND t1.x BETWEEN ",(werft.getX()-werft.getSize())," AND ",(werft.getX()+werft.getSize())," AND t1.y BETWEEN ",(werft.getY()-werft.getSize())," AND ",(werft.getY()+werft.getSize())," AND t1.system=",werft.getSystem()," AND !LOCATE('l ',t1.docked) AND t1.battle=0 ORDER BY t2.id,t1.id");
	
		while( ship.next() ) {
			if( (werft.getWerftType() == WerftObject.SHIP) && (((ShipWerft)werft).getShipID() == ship.getInt("id")) ) {
				continue;	
			}
			
			SQLResultRow shiptype = Ships.getShipType( ship.getRow() );
			
			if( (ship.getInt("hull") < shiptype.getInt("hull")) || (ship.getInt("engine") < 100) ||
				(ship.getInt("sensors") < 100) || (ship.getInt("comm") < 100) || (ship.getInt("weapons") < 100) ) {
				t.set_var("ship.needsrepair", 1);
			}
			else {
				t.set_var("ship.needsrepair", 0);
			}
			
			String ownername = Common._title(ship.getString("ownername"));
			if( ownername.length() == 0 ) {
				ownername = "Unbekannter Spieler ("+ship.getInt("owner")+")"; 
			}
						
			t.set_var(	"ship.id",			ship.getInt("id"),
						"ship.name",		ship.getString("name"),
						"ship.owner.name",	ownername );
								
			t.parse("wsshiplist.list", "wsshiplist.listitem", true);
		}
		ship.free();
	}

	private void out_buildShipList(WerftObject werft) {		
		t.set_var("werftgui.buildshiplist", 1);
		t.set_block("_WERFT.WERFTGUI", "buildshiplist.listitem", "buildshiplist.list");
		t.set_block("buildshiplist.listitem", "buildship.res.listitem", "buildship.res.list");
		
		SQLResultRow[] shipdata = werft.getBuildShipList();

		Cargo availablecargo = werft.getCargo(false);
	
		int energy = werft.getEnergy();
		int crew = werft.getCrew();
		
		for( int i=0; i < shipdata.length; i++ ) {
			SQLResultRow ashipdata = shipdata[i];
			t.start_record();
			
			Cargo costs = (Cargo)ashipdata.get("costs");
			costs.setOption( Cargo.Option.SHOWMASS, false );
	
			if( !(ashipdata.get("_item") instanceof Boolean) ) {
				Object[] data = (Object[])ashipdata.get("_item");
				ResourceID itemdata = (ResourceID)data[1];
				
				t.set_var(	"buildship.item.id",	itemdata.getItemID(),
							"buildship.item.color",	(data[0].toString().equals("local") ? "#EECC44" : "#44EE44"),
							"buildship.item.uses",	itemdata.getUses() );
			}
			
			SQLResultRow tmptype = Ships.getShipType( ashipdata.getInt("type"), false );
			
			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/time.gif",
						"res.count",		ashipdata.getInt("dauer"),
						"res.plainname",	"Dauer",
						"res.mangel",		0 );
			t.parse("buildship.res.list", "buildship.res.listitem", false);
				
			ResourceList reslist = costs.compare( availablecargo, false );
			for( ResourceEntry res : reslist ) {
				t.set_var(	"res.image",		res.getImage(),
							"res.count",		res.getCargo1(),
							"res.plainname",	res.getPlainName(),
							"res.mangel",		res.getDiff() > 0 );
				t.parse("buildship.res.list", "buildship.res.listitem", true);
			}

			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
						"res.count",		ashipdata.getInt("ekosten"),
						"res.plainname",	"Energie",
						"res.mangel",		energy < ashipdata.getInt("ekosten") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);
	
			t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/besatzung.gif",
						"res.count",		ashipdata.getInt("crew"),
						"res.plainname",	"Besatzung",
						"res.mangel",		crew < ashipdata.getInt("crew") );
			t.parse("buildship.res.list", "buildship.res.listitem", true);

			t.set_var(	"buildship.id",			ashipdata.getInt("id"),
						"buildship.type.id",	ashipdata.getInt("type"),
						"buildship.flagschiff",	ashipdata.getBoolean("flagschiff"),
						"buildship.type.name",	tmptype.getString("nickname") );
			t.parse("buildshiplist.list", "buildshiplist.listitem", true);
			
			t.stop_record();
			t.clear_record();
		}
	}

	private void out_ResourceList(WerftObject werft) {		
		t.set_var("werftgui.reslist", 1);
		t.set_block("_WERFT.WERFTGUI", "reslist.res.listitem", "reslist.res.list");
		
		Cargo cargo = werft.getCargo(false);
		int frei = werft.getCrew();
	
		ResourceList reslist = cargo.getResourceList();
		for( ResourceEntry res : reslist ) {
			t.set_var(	"res.image",		res.getImage(),
						"res.plainname",	res.getPlainName(),
						"res.cargo",		res.getCargo1() );
			t.parse("reslist.res.list", "reslist.res.listitem", true);
		}
		t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/energie.gif",
					"res.plainname",	"Energie",
					"res.cargo",		werft.getEnergy() );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
		
		t.set_var(	"res.image",		Configuration.getSetting("URL")+"data/interface/arbeitslos.gif",
					"res.plainname",	"Crew",
					"res.cargo",		frei );
		t.parse("reslist.res.list", "reslist.res.listitem", true);
	}

	private void out_werftbuilding(WerftObject werft, String conf) {
		// TODO
		throw new RuntimeException("STUB");
	}

	private void out_ws(WerftObject werft, int ws) {
		// TODO
		throw new RuntimeException("STUB");
	}

	private void out_buildShip(int build, int item, WerftObject werft, String conf) {
		// TODO
		throw new RuntimeException("STUB");
	}
}
