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
			shiplist = new Integer[] { db.first("SELECT id FROM ships WHERE owner=",user.getID()," AND system=",sectorRow.getInt("system")," AND x=",sectorRow.getInt("x")," AND y='",sectorRow.getInt("y")," AND type=",type," AND docked=''").getInt("id")};	
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
		
		if( (fleet != null) && fleet.isEmpty() && !action.equals("createFromSRSGroup") && !action.equals("create") && action.equals("create2") ) {
			addError("Die angegebene Flotte existiert nicht");
			
			return false;
		}

		int shipid = 0;
		// Nun brauchen wir die ID eines der Schiffe aus der Flotte fuer den javascript-code....
		if( (shiplist == null || shiplist.length == 0) && (fleetID != 0) ) {
			shipid = db.first("SELECT id FROM ships WHERE id>0 AND owner=",this.getUser().getID()," AND fleet=",fleetID).getInt("id");
		} 
		else {
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
		
		int id = db.first("SELECT id FROM ships WHERE id IN (",Common.implode(",",shiplist),") AND owner!='",user.getID(),"'").getInt("id");
		if( id != 0 ) {
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
		if( shiplist.charAt(0) != 'g' ) {
			shiplistInt = Common.explodeToInteger("|", shiplist);
			if( (getString("shiplist").length() == 0) || shiplistInt.length == 0 ) {
				t.set_var("fleetmgnt.message", "Sie haben keine Schiffe angegeben" );
				return;
			}
		
			int id = db.first("SELECT id FROM ships WHERE id IN (",Common.implode(",",shiplistInt),") AND owner!='",user.getID(),"'").getInt("id");
			if( id != 0 ) {
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
			db.update("INSERT INTO ship_fleets (name) VALUES ('",fleetname,"')");

			int fleetID = db.insertID();
			
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
			SQLResultRow shiptype = Ships.getShipType( shipRow );
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
		
		SQLQuery shiptype = db.query("SELECT nickname,id FROM ship_types WHERE LOCATE('"+Ships.SF_JAEGER+"',flags) "+(user.getAccessLevel() < 10 ? "AND hide=0" : ""));
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
