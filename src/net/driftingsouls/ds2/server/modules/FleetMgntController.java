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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Die Flottenverwaltung
 * @author Christopher Jung
 * 
 * @urlparam Integer fleet Die ID der Flotte, falls schon eine existiert
 * @urlparam String shiplist Eine mit | separierte Liste von Schiffs-IDs oder eine mit , separierte Liste mit Koordinaten, Schiffstyp und  Mengenangabe
 * @urlparam Integer sector Die ID eines Schiffs, dessen Sektor "geparst" werden soll
 * @urlparam Integer type Die Typen-ID der zu "parsenden" Schiffe
 * @urlparam Integer count Die Anzahl der maximal zu ermittelnden Schiffe
 *
 */
public class FleetMgntController extends DSGenerator {
	private SQLResultRow fleet = null;

	/**
	 * Konstruktor
	 * @param context Der zu verwendende Kontext
	 */
	public FleetMgntController(Context context) {
		super(context);
		
		setTemplate("fleetmgnt.html");
		
		addOnLoadFunction("reloadMainpage('"+this.getString("sess")+"')");
		
		parameterNumber("fleet");
		parameterString("shiplist");
		parameterNumber("sector");
		parameterNumber("type");
		parameterNumber("count");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		Integer[] shiplist = null;
		
		// Zuerst shiplist verarbeiten
		String shiplistStr = getString("shiplist");
		if( (shiplistStr.length() == 0) || (shiplistStr.charAt(0) != 'g') ) {
			shiplist = Common.explodeToInteger("|", shiplistStr);
		}
		else {
			String[] tmp = StringUtils.split(shiplistStr, ",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			
			SQLResultRow sectorRow = db.first("SELECT x,y,system FROM ships WHERE id=",sector," AND owner=",user.getID());
			shiplist = new Integer[] { 
					db.first("SELECT id FROM ships " +
							"WHERE owner=",user.getID()," AND system=",sectorRow.getInt("system")," AND " +
									"x=",sectorRow.getInt("x")," AND y=",sectorRow.getInt("y")," AND " +
									"type=",type," AND docked=''")
						.getInt("id")};	
		}
		
		// Evt haben wir bereits eine Flotten-ID uebergeben bekommen -> checken
		int fleetID = getInteger("fleet");
		if( fleetID != 0 ) {
			fleet = db.first("SELECT * FROM ship_fleets WHERE id=",fleetID);
			SQLResultRow owner = db.first("SELECT owner FROM ships WHERE id>0 AND fleet=",fleetID);

			if( fleet.isEmpty() || owner.isEmpty() || (user.getID() != owner.getInt("owner")) ) {
				fleet = null;
				addError("Diese Flotte geh&ouml;rt einem anderen Spieler");
			
				return false;
			}
			
			// Falls sich doch ein Schiff eines anderen Spielers eingeschlichen hat
			db.update("UPDATE ships SET fleet='0' WHERE fleet=",fleetID," AND owner!=",user.getID());
		}
		
		if( ((fleet == null) || fleet.isEmpty()) && !action.equals("createFromSRSGroup") && 
			!action.equals("create") && !action.equals("create2") ) {
			addError("Die angegebene Flotte existiert nicht");
			
			return false;
		}

		int shipid = 0;
		// Nun brauchen wir die ID eines der Schiffe aus der Flotte fuer den javascript-code....
		if( (shiplist == null || shiplist.length == 0) && (fleetID != 0) ) {
			shipid = db.first("SELECT id FROM ships WHERE id>0 AND owner=",this.getUser().getID()," AND fleet=",fleetID).getInt("id");
		} 
		else if( (shiplist != null) && (shiplist.length > 0) ){
			shipid = shiplist[0];
		}
		
		t.set_var(	"jscript.reloadmain.ship",	shipid,
					"fleet.id",					(fleet != null ? fleet.getInt("id") : 0) );
		
		return true;	
	}
	
	/**
	 * Zeigt den Flottenerstelldialog fuer eine
	 * Flotte aus einer Koordinaten-, Mengen- und Schiffstypenangabe
	 * an
	 *
	 */
	public void createFromSRSGroupAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		int sector = getInteger("sector");
		int type = getInteger("type");
		int count = getInteger("count");
		
		SQLResultRow sectorcoord = db.first("SELECT x,y,system FROM ships WHERE id='",sector,"' AND owner='",user.getID(),"'");
		
		int shipcount = db.first("SELECT count(*) count FROM ships WHERE owner='",user.getID(),"' AND system='",sectorcoord.getInt("system"),"' AND x='",sectorcoord.getInt("x"),"' AND y='",sectorcoord.getInt("y"),"' AND type='",type,"' AND docked=''").getInt("count");
		
		if( (count < 1) || (shipcount < count) ) {
			t.set_var("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor" );
			return;
		}
			
		t.set_var(	"show.create",		1,
					"create.shiplist",	"g,"+sector+","+type+","+count );
	}
	
	/**
	 * Zeigt den Erstelldialog fuer eine neue Flotte an
	 *
	 */
	public void createAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		Integer[] shiplist = Common.explodeToInteger("|", getString("shiplist"));
		
		if( (getString("shiplist").length() == 0) || (shiplist.length == 0) ) {
			t.set_var("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
			return;
		}
		
		SQLResultRow id = db.first("SELECT id FROM ships WHERE id IN (",Common.implode(",",shiplist),") AND owner!=",user.getID());
		if( !id.isEmpty() ) {
			t.set_var("fleetmgnt.message", "Alle Schiffe m&uuml;ssen ihrem Kommando unterstehen" );
		}
		else {		
			t.set_var(	"show.create",			1,
						"create.shiplist",		Common.implode("|",shiplist) );
		}
	}
	
	/**
	 * Erstellt eine Flotte aus einer Schiffsliste oder einer Koordinaten/Typen-Angabe
	 * @urlparam String fleetname der Name der neuen Flotte
	 *
	 */
	public void create2Action() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		parameterString("fleetname");
		String fleetname = getString("fleetname");
		
		Integer[] shiplistInt = null;
		
		String shiplist = getString("shiplist");
		if( shiplist.length() == 0 ) {
			t.set_var("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
			return;
		}
		
		if( shiplist.charAt(0) != 'g' ) {
			shiplistInt = Common.explodeToInteger("|", shiplist);
			if( (getString("shiplist").length() == 0) || shiplistInt.length == 0 ) {
				t.set_var("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
				return;
			}
		
			SQLResultRow id = db.first("SELECT id FROM ships WHERE id IN (",Common.implode(",",shiplistInt),") AND owner!='",user.getID(),"'");
			if( !id.isEmpty() ) {
				t.set_var("fleetmgnt.message", "Alle Schiffe m&uuml;ssen ihrem Kommando unterstehen" );
				return;
			}
		}
		else {
			String[] tmp = StringUtils.split(shiplist,",");
			int sector = Integer.parseInt(tmp[1]);
			int type = Integer.parseInt(tmp[2]);
			int count = Integer.parseInt(tmp[3]);
			SQLResultRow sectorRow = db.first("SELECT x,y,system FROM ships WHERE id='",sector,"' AND owner='",user.getID(),"'");
			
			SQLQuery s = db.query("SELECT id,fleet FROM ships WHERE owner='",user.getID(),"' AND system='",sectorRow.getInt("system"),"' AND x='",sectorRow.getInt("x"),"' AND y='",sectorRow.getInt("y"),"' AND type='",type,"' AND docked='' ORDER BY fleet,id ASC LIMIT ",count);
			shiplistInt = new Integer[s.numRows()];
			int i=0;
			while( s.next() ) {
				if( s.getInt("fleet") != 0 ) {
					Ships.removeFromFleet(s.getRow());	
				}
				shiplistInt[i++] = s.getInt("id");
			}
			s.free();
		}
		
		if( fleetname.length() > 0 ) {
			PreparedQuery pq = db.prepare("INSERT INTO ship_fleets (name) VALUES ( ? )");
			pq.update(fleetname);

			int fleetID = pq.insertID();
			
			pq.close();
			
			db.update("UPDATE ships SET fleet=",fleetID," WHERE id>0 AND id IN (",Common.implode(",",shiplistInt)+")");

			t.set_var(	"fleetmgnt.message",	"Flotte "+Common._plaintitle(fleetname)+" erstellt",
						"jscript.reloadmain",	1,
						"fleet.id",				fleetID );
			
			fleet = db.first("SELECT * FROM ship_fleets WHERE id='",fleetID,"'");
			
			this.redirect();
		} 
		else {
			t.set_var("fleetmgnt.message", "Sie m&uuml;ssen einen Namen angeben" );
			redirect("create");
		}
	}
	
	/**
	 * Fuegt eine definierte Anzahl an Schiffen eines Typs aus einem Sektor zur
	 * Flotte hinzu
	 *
	 */
	public void addFromSRSGroupAction() {
		TemplateEngine t = getTemplateEngine();
		User user = getUser();
		Database db = getDatabase();
		
		int sector = getInteger("sector");
		int type = getInteger("type");
		int count = getInteger("count");
		
		SQLResultRow sectorRow = db.first("SELECT x,y,system FROM ships WHERE id='",sector,"' AND owner='",user.getID(),"'");
		
		int shipcount = db.first("SELECT count(*) count FROM ships WHERE owner='",user.getID(),"' AND system='",sectorRow.getInt("system"),"' AND x='",sectorRow.getInt("x"),"' AND y='",sectorRow.getInt("y"),"' AND type='",type,"' AND docked=''").getInt("count");
		
		if( (count < 1) || (shipcount < count) ) {
			t.set_var("fleetmgnt.message", "Es gibt nicht genug Schiffe im Sektor" );
			return;
		}
		
		List<Integer> shiplist = new ArrayList<Integer>();
		SQLQuery s = db.query("SELECT id,fleet FROM ships WHERE owner='",user.getID(),"' AND system='",sectorRow.getInt("system"),"' AND x='",sectorRow.getInt("x"),"' AND y='",sectorRow.getInt("y"),"' AND type='",type,"' AND docked='' AND fleet!='",fleet.getInt("id"),"' ORDER BY fleet,id ASC LIMIT ",count);
		while( s.next() ) {
			if( s.getInt("fleet") != 0 ) {
				Ships.removeFromFleet(s.getRow());	
			}
			shiplist.add(s.getInt("id"));
		}
		s.free();
		
		db.update("UPDATE ships SET fleet='",fleet.getInt("id"),"' WHERE id IN (",Common.implode(",",shiplist),")");
		
		t.set_var(	"fleetmgnt.message",	count+" Schiffe der Flotte hinzugef&uuml;gt",
					"jscript.reloadmain",	1 );
	}
	
	/**
	 * Zeigt die Seite zum Umbenennen von Flotten an
	 *
	 */
	public void renameAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.set_var(	"show.rename",	1,
					"fleet.id",		fleet.getInt("id"),
					"fleet.name",	Common._plaintitle(fleet.getString("name")) );
	}
	
	/**
	 * Benennt eine Flotte um
	 * @urlparam String fleetname Der neue Name der Flotte
	 *
	 */
	public void rename2Action() {
		Database db = getDatabase();
		TemplateEngine t = getTemplateEngine();
		
		parameterString("fleetname");
		String fleetname = getString("fleetname");
		
		if( fleetname.length() > 0 ) {
			db.prepare("UPDATE ship_fleets SET name= ? WHERE id= ?")
				.update(fleetname, fleet.getInt("id"));

			t.set_var(	"fleetmgnt.message",	"Flotte "+Common._plaintitle(fleetname)+" umbenannt",
						"jscript.reloadmain",	1 );
		
			fleet.put("name", fleetname);
		
			redirect();
		}
		else {
			t.set_var("fleetmgnt.message", "Sie m&uuml;ssen einen Namen angeben" );
			
			redirect("rename");
		}
	}
	
	/**
	 * Zeigt die Abfrage an, ob eine Flotte aufgeloest werden soll
	 *
	 */
	public void killAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.set_var(	"fleet.name",	Common._plaintitle(fleet.getString("name")),
					"fleet.id",		fleet.getInt("id"),
					"show.kill",	1 );
	}
	
	/**
	 * Loest eine Flotte auf
	 *
	 */
	public void kill2Action() {	
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
			
		db.update("UPDATE ships SET fleet=0 WHERE fleet='",fleet.getInt("id"),"'");
		db.update("DELETE FROM ship_fleets WHERE id='",fleet.getInt("id"),"'");

		t.set_var(	"fleetmgnt.message",	"Die Flotte '"+fleet.getString("name")+"' wurde aufgel&ouml;st",
					"jscript.reloadmain",	1 );
	}

	/**
	 * Zeigt das Eingabefeld fuer die Uebergabe von Flotten an
	 *
	 */
	public void newownerAction() {
		TemplateEngine t = getTemplateEngine();
		
		t.set_var(	"show.newowner",	1,
					"fleet.id",			this.fleet.getInt("id"),
					"fleet.name",		Common._plaintitle(this.fleet.getString("name")) );
	}
	
	/**
	 * Zeigt die Bestaetigung fuer die Uebergabe der Flotte an
	 * @urlparam Integer ownerid Die ID des Users, an den die Flotte uebergeben werden soll
	 *
	 */
	public void newowner2Action() {
		TemplateEngine t = getTemplateEngine();
		
		parameterNumber("ownerid");
		int ownerid = getInteger("ownerid");
		
		User newowner = getContext().createUserObject(ownerid );
		
		if( newowner.getID() != 0 ) {
			t.set_var(	"show.newowner2",	1,
						"newowner.name",	Common._title(newowner.getName()),
						"newowner.id",		newowner.getID(),
						"fleet.id",			this.fleet.getInt("id"),
						"fleet.name",		Common._plaintitle(this.fleet.getString("name")) );
		}
		else {
			t.set_var( "fleetmgnt.message", "Der angegebene Spieler existiert nicht");
			
			redirect("newowner");	
		}	
	}
	
	/**
	 * Uebergibt die Flotte an einen neuen Spieler
	 * @urlparam Integer ownerid Die ID des neuen Besitzers
	 *
	 */
	public void newowner3Action() {
		TemplateEngine t = getTemplateEngine();
		User user = this.getUser();
		Database db = getDatabase();
		
		parameterNumber("ownerid");
		int ownerid = getInteger("ownerid");
		
		User newowner = getContext().createUserObject(ownerid);
		
		if( newowner.getID() != 0 ) {
			StringBuilder message = new StringBuilder(100);
			int count = 0;
			
			List<Integer> idlist = new ArrayList<Integer>();
			
			SQLQuery s = db.query("SELECT * FROM ships WHERE fleet='",this.fleet.getInt("id"),"' AND battle=0" );
			while( s.next() ) {
				boolean tmp = Ships.consign(user, s.getRow(), newowner, false );
			
				String msg = Ships.MESSAGE.getMessage();
				if( msg.length() > 0 ) {
					message.append(msg+"<br />");	
				}
				if( !tmp ) {
					count++;
					idlist.add(s.getInt("id"));
				}
			}
			s.free();

			if( count != 0 ) {
				// Da die Schiffe beim uebergeben aus der Flotte geschmissen werden, muessen wir sie nun wieder hinein tun
				db.update("UPDATE ships SET fleet='",this.fleet.getInt("id"),"' WHERE id IN ("+Common.implode(",",idlist),")");
				
				SQLResultRow coords = db.first("SELECT x,y,system FROM ships WHERE owner='",newowner.getID(),"' AND fleet='",this.fleet.getInt("id"),"'");
				
				PM.send(getContext(), user.getID(), newowner.getID(), "Flotte &uuml;bergeben", "Ich habe dir die Flotte "+Common._plaintitle(this.fleet.getString("name"))+" &uuml;bergeben. Sie steht bei "+coords.getInt("system")+":"+coords.getInt("x")+"/"+coords.getInt("y"));
		
				t.set_var("fleetmgnt.message", message+"Die Flotte wurde &uuml;bergeben");
			}
			else {
				t.set_var("fleetmgnt.message", message+"Flotten&uuml;bergabe gescheitert");
			}
		}
		else {
			t.set_var( "fleetmgnt.message", "Der angegebene Spieler existiert nicht");
			
			redirect("newowner");	
		}
	}
	
	/**
	 * Laedt die Schilde aller Schiffe in der Flotte auf
	 *
	 */
	public void shupAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		StringBuilder message = new StringBuilder(100);
		
		db.tBegin();
		SQLQuery s = db.query("SELECT t1.id,t1.name,t1.shields,t1.e,t1.status,t1.type FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id WHERE t1.fleet='",this.fleet.getInt("id"),"' AND (t1.shields < t2.shields OR LOCATE('tblmodules',t1.status)) AND t1.battle=0");
		while( s.next() ) {
			SQLResultRow sRow = s.getRow();
			SQLResultRow stype = ShipTypes.getShipType( sRow );
			
			int shieldfactor = 100;
			if( stype.getInt("shields") < 1000 ) {
				shieldfactor = 10;
			}

			int shup = (int)Math.ceil((stype.getInt("shields") - sRow.getInt("shields"))/(double)shieldfactor);
			if( shup > sRow.getInt("e") ) {
				shup = sRow.getInt("e");
				message.append(sRow.getString("name")+" ("+sRow.getInt("id")+") - <span style=\"color:orange\">Schilde bei "+Math.round((sRow.getInt("shields")+shup*shieldfactor)/(double)stype.getInt("shields")*100)+"%</span><br />");
			}
			sRow.put("shields", sRow.getInt("shields") + shup*shieldfactor);
			if( sRow.getInt("shields") > stype.getInt("shields") ) {
				sRow.put("shields", stype.getInt("shields"));
			}
			db.tUpdate(1,"UPDATE ships SET shields='",sRow.getInt("shields"),"',e='",(sRow.getInt("e")-shup)+"' WHERE id>0 AND id='"+sRow.getInt("id")+"' AND e=",s.getInt("e")," AND shields=",s.getInt("shields"));
		}
		s.free();
		db.tCommit();

		t.set_var( "fleetmgnt.message", message+" Die Schilde wurden aufgeladen" );
		
		redirect();			
	}
	
	/**
	 * Entlaedt die Batterien auf den Schiffen der Flotte, um die EPS wieder aufzuladen
	 *
	 */
	public void dischargeBatteriesAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		StringBuilder message = new StringBuilder(100);
		
		db.tBegin();
		
		SQLQuery sRow = db.query("SELECT t1.* FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id WHERE t1.fleet='",this.fleet.getInt("id"),"' AND (t1.e < t2.eps OR LOCATE('tblmodules',t1.status)) AND t1.battle=0 AND t1.type=t2.id");
		while( sRow.next() ) {
			SQLResultRow s = sRow.getRow();
			SQLResultRow stype = ShipTypes.getShipType( s );
			int olde = s.getInt("e");			
			
			if( s.getInt("e") >= stype.getInt("eps") ) {
				continue;
			}
			
			Cargo cargo = new Cargo( Cargo.Type.STRING, s.getString("cargo") );
		
			long unload = stype.getInt("eps") - s.getInt("e");
			if( unload > cargo.getResourceCount( Resources.BATTERIEN ) ) {
				unload = cargo.getResourceCount( Resources.BATTERIEN ) ;
				
				message.append(s.getString("name")+" ("+s.getInt("id")+") - <span style=\"color:orange\">Energie bei "+Math.round((s.getInt("e")+unload)/(double)stype.getInt("eps")*100)+"%</span><br />");
			}
			cargo.substractResource( Resources.BATTERIEN, unload );
			cargo.addResource( Resources.LBATTERIEN, unload );
		
			s.put("e", s.getInt("e")+unload);
			
			db.tUpdate(1, "UPDATE ships SET e='",s.getInt("e"),"',cargo='",cargo.save(),"' WHERE id>0 AND id='",s.getInt("id"),"' AND cargo='",cargo.save(true),"' AND e='",olde,"'");
		}
		sRow.free();
		
		db.tCommit();

		t.set_var( "fleetmgnt.message", message+"Batterien wurden entladen" );
		
		redirect();			
	}
	
	/**
	 * Exportiert die Schiffsliste der Flotte
	 *
	 */
	public void exportAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		
		t.set_var(	"fleet.name",	Common._plaintitle(this.fleet.getString("name")),
					"show.export",	1 );
							
		t.set_block("_FLEETMGNT", "exportships.listitem", "exportships.list" );
		
		SQLQuery s = db.query("SELECT id,name FROM ships WHERE id>0 AND fleet=",this.fleet.getInt("id") );
		while( s.next() ) {
			t.set_var(	"ship.id",		s.getInt("id"),
						"ship.name",	Common._plaintitle(s.getString("name")) );
				
			t.parse("exportships.list", "exportships.listitem", true);
		}
		s.free();
	}
	
	/**
	 * Dockt alle Schiffe der Flotte ab
	 *
	 */
	public void undockAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		SQLQuery s = db.query("SELECT id FROM ships WHERE id>0 AND fleet='",this.fleet.getInt("id"),"' AND battle=0" );
		while( s.next() ) {
			Ships.dock(Ships.DockMode.UNDOCK, user.getID(), s.getInt("id"), null);
		}
		s.free();
		
		t.set_var(	"fleetmgnt.message",	"Alle gedockten Schiffe wurden gestartet",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Sammelt alle nicht gedockten eigenen Container im Sektor auf (sofern genug Platz vorhanden ist)
	 *
	 */
	public void redockAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		SQLQuery ship = db.query("SELECT * FROM ships WHERE id>0 AND fleet='",this.fleet.getInt("id"),"' AND battle=0" );
		while( ship.next() ) {
			SQLResultRow shiptype = ShipTypes.getShipType(ship.getRow());
			
			if( shiptype.getInt("adocks") == 0 ) {
				continue;
			}

			int free = shiptype.getInt("adocks");
			free -= db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='",ship.getInt("id"),"'").getInt("count");
			List<Integer> containerlist = new ArrayList<Integer>();
			
			SQLQuery container = db.query("SELECT s.id FROM ships s JOIN ship_types st ON s.type=st.id " +
					"WHERE s.owner='",user.getID(),"' AND s.system='",ship.getInt("system"),"' AND" +
							" s.x='",ship.getInt("x"),"' AND s.y='",ship.getInt("y"),"' AND s.docked='' AND " +
							"st.class='",ShipClasses.CONTAINER,"' AND s.battle=0 " +
					"ORDER BY fleet,type ");
			while( container.next() ) {
				containerlist.add(container.getInt("id"));
				free--;
				if( free == 0 ) {
					break;
				}
			}
			container.free();
			
			int[] list = new int[containerlist.size()];
			for( int i=0; i < containerlist.size(); i++ ) {
				list[i] = containerlist.get(i);
			}
			
			Ships.dock(Ships.DockMode.DOCK, user.getID(), ship.getInt("id"), list);
		}
		ship.free();

		t.set_var(	"fleetmgnt.message",	"Container wurden aufgesammelt",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Startet alle Jaeger der Flotte
	 *
	 */
	public void jstartAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		SQLQuery s = db.query("SELECT id FROM ships WHERE id>0 AND fleet='",this.fleet.getInt("id"),"' AND battle=0" );
		while( s.next() ) {
			Ships.dock(Ships.DockMode.START, user.getID(), s.getInt("id"), null);
		}
		s.free();
		
		t.set_var(	"fleetmgnt.message",	"Alle J&auml;ger sind gestartet",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Sammelt alle nicht gelandeten eigenen Jaeger im Sektor auf (sofern genug Platz vorhanden ist)
	 *
	 */
	public void jlandAction() {	
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		parameterNumber("jaegertype");
		int jaegertypeID = getInteger("jaegertype");
		
		SQLQuery ship = db.query("SELECT * FROM ships WHERE id>0 AND fleet='",this.fleet.getInt("id"),"' AND battle=0" );
		while( ship.next() ) {
			SQLResultRow shiptype = ShipTypes.getShipType(ship.getRow());
			
			if( shiptype.getInt("jdocks") == 0 ) {
				continue;
			}
			int free = shiptype.getInt("jdocks");
			free -= db.first("SELECT count(*) count FROM ships WHERE id>0 AND docked='l ",ship.getInt("id"),"'").getInt("count");

			List<Integer >jaegerlist = new ArrayList<Integer>();
			
			SQLQuery jaeger = db.query("SELECT s.* FROM ships s JOIN ship_types st ON s.type=st.id " +
					"WHERE "+(jaegertypeID > 0 ? "s.type="+jaegertypeID+" AND " : "")+"s.owner='",user.getID(),"' AND " +
							"s.system='",ship.getInt("system"),"' AND s.x='",ship.getInt("x"),"' AND s.y='",ship.getInt("y"),"' AND " +
							"s.docked='' AND (LOCATE('tblmodules',s.status) OR LOCATE('",ShipTypes.SF_JAEGER,"',st.flags)) AND s.battle=0 " +
					"ORDER BY fleet,type");
			while( jaeger.next() ) {
				SQLResultRow jaegertype = ShipTypes.getShipType(jaeger.getRow());
				if( ShipTypes.hasShipTypeFlag(jaegertype, ShipTypes.SF_JAEGER) ) {
					jaegerlist.add(jaeger.getInt("id"));
					free--;
					if( free == 0 ) {
						break;
					}
				}
			}
			jaeger.free();
			
			int[] list = new int[jaegerlist.size()];
			for( int i=0; i < jaegerlist.size(); i++ ) {
				list[i] = jaegerlist.get(i);
			}
			
			Ships.dock(Ships.DockMode.LAND, user.getID(), ship.getInt("id"), list);
		}
		ship.free();

		t.set_var(	"fleetmgnt.message",	"J&auml;ger wurden aufgesammelt",
					"jscript.reloadmain",	1 );

		redirect();
	}
	
	/**
	 * Fuegt die Schiffe einer anderen Flotte der aktiven Flotte hinzu
	 * @urlparam Integer fleetcombine Die ID der Flotte, deren Schiffe zur aktiven Flotte hinzugefuegt werden sollen
	 *
	 */
	public void fleetcombineAction() {
		TemplateEngine t = getTemplateEngine();
		Database db = getDatabase();
		User user = getUser();
		
		parameterNumber("fleetcombine");
		int fleetID = getInteger("fleetcombine");
		
		SQLResultRow aowner = db.first("SELECT owner FROM ships WHERE id>0 AND fleet='",fleetID,"'");
		if( aowner.isEmpty() || (aowner.getInt("owner") != user.getID()) ) {
			addError("Die angegebene Flotte geh&ouml;rt nicht ihnen!");
			this.redirect();
			return;
		}
		
		SQLResultRow fleetname = db.first("SELECT name FROM ship_fleets WHERE id='",fleetID,"'");
		
		db.update("UPDATE ships SET fleet='",fleet.getInt("id"),"' WHERE id>0 AND fleet='",fleetID,"'");
		int count = db.first("SELECT count(*) count FROM ships WHERE fleet='",fleetID,"'").getInt("count");
		if( count == 0 ) {
			db.update("DELETE FROM ship_fleets WHERE id='",fleetID,"'");
		}
		
		t.set_var(	"fleetmgnt.message",	"Alle Schiffe der Flotte '"+Common._plaintitle(fleetname.getString("name"))+"' sind beigetreten",
					"jscript.reloadmain",	1 );
							
		this.redirect();
	}
	
	@Override
	public void defaultAction() {
		Database db = getDatabase();
		User user = getUser();
		TemplateEngine t = getTemplateEngine();
		
		List<String> sectors = new ArrayList<String>();
	
		SQLResultRow aship = db.first("SELECT x,y,system FROM ships WHERE id>0 AND fleet=",this.fleet.getInt("id"));
		
		sectors.add("(x='"+aship.getInt("x")+"' AND y='"+aship.getInt("y")+"' AND system='"+aship.getInt("system")+"')");

		t.set_var(	"show.view",	1,
					"fleet.name",	Common._plaintitle(this.fleet.getString("name")),
					"fleet.id",		this.fleet.getInt("id") );
								
		t.set_block("_FLEETMGNT", "ships.listitem", "ships.list");
	
		Location aloc = Location.fromResult(aship);
		
		SQLQuery ship = db.query("SELECT name,id,battle,system,x,y,type,status FROM ships WHERE id>0 AND owner=",user.getID()," AND fleet=",this.fleet.getInt("id")," ORDER BY id");
		while( ship.next() ) {
			SQLResultRow shipRow = ship.getRow();
			SQLResultRow shiptype = ShipTypes.getShipType( shipRow );
			Location loc = Location.fromResult(shipRow);
			
			t.set_var(	"ship.id",			ship.getInt("id"),
						"ship.name",		Common._plaintitle(ship.getString("name")),
						"ship.type.name",	shiptype.getString("nickname"),
						"ship.showbattle",	ship.getInt("battle"),
						"ship.showwarning",	!aloc.sameSector(0, loc, 0) );
			
			if( !aloc.sameSector(0, loc, 0) ) {
				sectors.add("(x='"+ship.getInt("x")+"' AND y='"+ship.getInt("y")+"' AND system='"+ship.getInt("system")+"')");
			}
			
			t.parse("ships.list", "ships.listitem", true);
		}	
		ship.free();
		
		// Jaegerliste bauen
		String sectorstring = Common.implode(" OR ", sectors);
		
		t.set_block("_FLEETMGNT", "jaegertypes.listitem", "jaegertypes.list");
		
		SQLQuery shiptype = db.query("SELECT nickname,id FROM ship_types WHERE LOCATE('"+ShipTypes.SF_JAEGER+"',flags) "+(user.getAccessLevel() < 10 ? "AND hide=0" : ""));
		while( shiptype.next() ) {
			int count = db.first("SELECT count(*) count FROM ships WHERE owner=",user.getID()," AND type=",shiptype.getInt("id")," AND docked='' AND (",sectorstring,")").getInt("count");
			if( count == 0 ) {
				continue;
			}
			
			t.set_var(	"jaegertype.id",	shiptype.getInt("id"),
						"jaegertype.name",	shiptype.getString("nickname") );
								
			t.parse("jaegertypes.list", "jaegertypes.listitem", true);
		}
		shiptype.free();
		
		// Flottenliste bauen
		t.set_block("_FLEETMGNT", "fleetcombine.listitem", "fleetcombine.list");
		SQLQuery afleet = db.query("SELECT t2.id,t2.name FROM ships t1 JOIN ship_fleets t2 ON t1.fleet=t2.id WHERE t1.system='",aship.getInt("system"),"' AND t1.x='",aship.getInt("x"),"' AND t1.y='",aship.getInt("y"),"' AND docked='' AND owner='",user.getID(),"' AND t1.fleet!='",this.fleet.getInt("id"),"' GROUP BY t2.id");
		while( afleet.next() ) {
			int count = db.first("SELECT count(*) count FROM ships WHERE fleet=",afleet.getInt("id")).getInt("count");
			
			t.set_var(	"fleetcombine.id",			afleet.getInt("id"),
						"fleetcombine.name",		Common._plaintitle(afleet.getString("name")),
						"fleetcombine.shipcount",	count );
								
			t.parse("fleetcombine.list", "fleetcombine.listitem", true);
		}
		afleet.free();
	}
}
