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
import net.driftingsouls.ds2.server.cargo.ResourceList;
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
		// TODO
		throw new RuntimeException("STUB");
	}

	private void out_buildShipList(WerftObject werft) {
		// TODO
		throw new RuntimeException("STUB");
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
