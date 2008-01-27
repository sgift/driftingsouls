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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Waypoint;

/**
 * Schiffsmodul fuer die Anzeige der Navigation
 * @author Christopher Jung
 *
 */
public class NavigationDefault implements SchiffPlugin, Loggable {

	public String action(Parameters caller) {
		SQLResultRow ship = caller.ship;
		SchiffController controller = caller.controller;
		
		Database db = controller.getDatabase();
		User user = controller.getUser();
		
		String output = "";

		if( !ship.getString("lock").equals("") ) {
			return output;	
		}
		
		controller.parameterString("setdest");
		String setdest = controller.getString("setdest");

		//Wird eine neue Beschreibung gesetzt?
		if( setdest.length() > 0 ) {
			controller.parameterNumber("system");
			controller.parameterNumber("x");
			controller.parameterNumber("y");
			controller.parameterString("com");
			controller.parameterNumber("bookmark");
			
			int system = controller.getInteger("system");
			int x = controller.getInteger("x");
			int y = controller.getInteger("y");
			String com = controller.getString("com");
			int bookmark = controller.getInteger("bookmark");
		
			db.update(	"UPDATE ships ",
						"SET destsystem=",system,", destx=",x,", desty=",y,", destcom='",db.prepareString(com),"',bookmark=",bookmark," " ,
						"WHERE id='",ship.getInt("id"),"'");
						
			output += "Neues Ziel: "+system+":"+x+"/"+y+"<br />Beschreibung: "+Common._plaintitle(com);
			if( bookmark != 0 ) {
				output += "<br />[Bookmark]\n";
			}
			return output;
		}
		
		controller.parameterNumber("act");
		controller.parameterNumber("count");
		controller.parameterNumber("targetx");
		controller.parameterNumber("targety");
		int act = controller.getInteger("act");
		int count = controller.getInteger("count");
		int targetx = controller.getInteger("targetx");
		int targety = controller.getInteger("targety");
		
		if( (act > 9) || (act < 1) || count <= 0 ) {
			return "Ung&uuml;ltige Flugparameter<br />\n";
		}
		
		if( (ship.getString("onmove").length() > 0) && 
			((targetx != 0) && (targety != 0)) || ((act != 5) && (act >= 1) && (act <= 9)) ) {	
			ScriptParser scriptparser = ContextMap.getContext().get(ContextCommon.class).getScriptParser( ScriptParser.NameSpace.QUEST );
			scriptparser.setShip(ship);
			scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
			scriptparser.setRegister("DIRECTION",Integer.toString(act));
			scriptparser.setRegister("MOVEMENTCOUNT",Integer.toString(count));
			scriptparser.setRegister("TARGETX",Integer.toString(targetx));
			scriptparser.setRegister("TARGETY",Integer.toString(targety));
			scriptparser.setRegister("SECTOR", Location.fromResult(ship).toString());
		
			Quests.currentEventURL.set("&action=onmove");

			Quests.executeEvent( scriptparser, ship.getString("onmove"), user.getID(), "0" );
			try {
				act = Integer.parseInt(scriptparser.getRegister("DIRECTION"));
				count = Integer.parseInt(scriptparser.getRegister("MOVEMENTCOUNT"));
				targetx = Integer.parseInt(scriptparser.getRegister("TARGETX"));
				targety = Integer.parseInt(scriptparser.getRegister("TARGETY"));
			}
			catch( NumberFormatException e ) {
				LOG.warn("Illegales Zahlenformat nach Ausfuehrung von 'onmove'", e);
			}
			

			if( (act > 9) || (act < 1) || count <= 0 ) {
				return "Ung&uuml;ltige Flugparameter<br />\n";
			}
		}
		
		//Wir wollen nur beim Autopiloten lowheat erzwingen
		boolean forceLowHeat = false;
		RouteFactory router = new RouteFactory();
		List<Waypoint> route = null;
		if( targetx == 0 || targety == 0 ) {
			route = router.getMovementRoute(act, count);
		}
		else {
			forceLowHeat = true;
			Location from = Location.fromResult(ship);
			route = router.findRoute(from, new Location(from.getSystem(), targetx, targety));
		}
		
		//Das Schiff soll sich offenbar bewegen
		Ships.move(ship.getInt("id"), route, forceLowHeat, false);
		output += Ships.MESSAGE.getMessage();
		
		return output;
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		SQLResultRow data = caller.ship;
		SQLResultRow datatype = caller.shiptype;
		SchiffController controller = caller.controller;
		
		User user = controller.getUser();
		Database db = controller.getDatabase();
		
		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.navigation.default.html");
		
		t.setVar(	"global.pluginid",					pluginid,
					"ship.id",							data.getInt("id"),
					"schiff.navigation.docked",			data.getString("docked"),
					"schiff.navigation.docked.extern",	data.getString("docked").equals("") || (data.getString("docked").charAt(0) != 'l'),
					"schiff.navigation.bookmarked",		data.getBoolean("bookmark"),
					"schiff.navigation.dest.x",			data.getInt("destx"),
					"schiff.navigation.dest.y",			data.getInt("desty"),
					"schiff.navigation.dest.system",	data.getInt("destsystem"),
					"schiff.navigation.dest.text",		data.getString("destcom") );
		
		if( !data.getString("docked").equals("") ) {
			String mastershipid = null;
			
			if( data.getString("docked").charAt(0) == 'l' ) {
				String[] docked = data.getString("docked").split(" ");
				mastershipid = docked[1];
			} else {
				mastershipid = data.getString("docked");
			}
			SQLResultRow mastership = db.first("SELECT name FROM ships WHERE id>0 AND id=",mastershipid);
			
			t.setVar(	"schiff.navigation.docked.master.name",	mastership.getString("name"),
						"schiff.navigation.docked.master.id",	mastershipid );
		} 
		else if( datatype.getInt("cost") == 0 ) {
			t.setVar("schiff.navigation.showmessage","Dieses Objekt hat keinen Antrieb");
		} 
		else if( !data.getString("lock").equals("") ) {
			t.setVar("schiff.navigation.showmessage","Fahren sie im Quest fort<br />um das Schiff wieder bewegen<br />zu k&ouml;nnen");	
		}
		else {
			int x = data.getInt("x");
			int y = data.getInt("y");
			int sys = data.getInt("system");
			
			String[][] sectorimgs = new String[3][3];
			SQLQuery aJN = db.query("SELECT x,y FROM jumpnodes WHERE system='",sys,"' AND (x BETWEEN ",(x-1)," AND ",(x+1),") AND (y BETWEEN ",(y-1)," AND ",(y+1),")");
			while( aJN.next() ) {
				sectorimgs[aJN.getInt("x")-x+1][aJN.getInt("y")-y+1] = "data/starmap/jumpnode/jumpnode.png";
			}
			aJN.free();
			
			
			SQLQuery aBase = db.query("SELECT DISTINCT x,y,owner,klasse,system,size FROM bases WHERE system=",sys," AND FLOOR(SQRT(POW(",x,"-x,2)+POW(",y,"-y,2)))-CAST(size AS SIGNED) <= 1");
			while( aBase.next() ) {
				if( (aBase.getInt("size") == 0) && (sectorimgs[aBase.getInt("x")-x+1][aBase.getInt("y")-y+1] == null) ) {
					if( aBase.getInt("owner") == user.getID() ) {
						sectorimgs[aBase.getInt("x")-x+1][aBase.getInt("y")-y+1] = "data/starmap/asti_own/asti_own.png";
					}
					else {
						sectorimgs[aBase.getInt("x")-x+1][aBase.getInt("y")-y+1] = "data/starmap/kolonie"+aBase.getString("klasse")+"_lrs/kolonie"+aBase.getInt("klasse")+"_lrs.png";
					}
				}
				else if( aBase.getInt("size") > 0 ) {
					Location loc = new Location(aBase.getInt("system"), aBase.getInt("x"), aBase.getInt("y"));
					int imgcount = 0;
					for( int by=aBase.getInt("y")-aBase.getInt("size"); by <= aBase.getInt("y")+aBase.getInt("size"); by++ ) {
						for( int bx=aBase.getInt("x")-aBase.getInt("size"); bx <= aBase.getInt("x")+aBase.getInt("size"); bx++ ) {
							if( !loc.sameSector( aBase.getInt("size"), new Location(aBase.getInt("system"), bx, by), 0 ) ) {
								continue;	
							}
							if( Math.abs(x - bx) > 1 || Math.abs(y - by) > 1 ) {
								imgcount++;
								continue;
							}
							sectorimgs[bx-x+1][by-y+1] = "data/starmap/kolonie"+aBase.getInt("klasse")+"_lrs/kolonie"+aBase.getInt("klasse")+"_lrs"+imgcount+".png";
							imgcount++;
						}
					}
				}
			}
			aBase.free();
			
			SQLQuery aNebel = db.query("SELECT system,x,y,type FROM nebel WHERE system='",sys,"' AND (x BETWEEN ",(x-1)," AND ",(x+1),") AND (y BETWEEN ",(y-1)," AND ",(y+1),")");
			while( aNebel.next() ) {
				Ships.cacheNebula(aNebel.getRow());
				sectorimgs[aNebel.getInt("x")-x+1][aNebel.getInt("y")-y+1] = "data/starmap/fog"+aNebel.getInt("type")+"/fog"+aNebel.getInt("type")+".png";
			}
			aNebel.free();
			
			int tmp = 0;
			boolean newrow = false;
			final String url = Configuration.getSetting("URL"); 
			
			t.setVar("schiff.navigation.size",37);
			
			t.setBlock("_NAVIGATION","schiff.navigation.nav.listitem","schiff.navigation.nav.list");
			for( int ny = 0; ny <= 2; ny++ ) {
				newrow = true;
				for( int nx = 0; nx <= 2; nx++ ) {
					tmp++;
					
					t.setVar(	"schiff.navigation.nav.direction",		tmp,
								"schiff.navigation.nav.location",		Ships.getLocationText(sys, x+nx-1, y+ny-1, true),
								"schiff.navigation.nav.sectorimage",	url + (sectorimgs[nx][ny] != null ? sectorimgs[nx][ny] : "data/starmap/space/space.png"),
								"schiff.navigation.nav.newrow",			newrow,
								"schiff.navigation.nav.warn",			(1 != nx || 1 != ny ? Ships.getRedAlertStatus(user.getID(),sys,x+nx-1,y+ny-1) : false) );
					
					t.parse( "schiff.navigation.nav.list", "schiff.navigation.nav.listitem", true );
					newrow = false;
				}
			}
		}
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
