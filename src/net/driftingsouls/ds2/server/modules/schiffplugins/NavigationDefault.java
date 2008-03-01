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

import java.util.Iterator;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.Quests;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Waypoint;

/**
 * Schiffsmodul fuer die Anzeige der Navigation
 * @author Christopher Jung
 *
 */
public class NavigationDefault implements SchiffPlugin, Loggable {

	public String action(Parameters caller) {
		Ship ship = caller.ship;
		SchiffController controller = caller.controller;
		
		User user = (User)controller.getUser();
		
		String output = "";

		if( (ship.getLock() != null) && !ship.getLock().equals("") ) {
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
		
			ship.setDestSystem(system);
			ship.setDestX(x);
			ship.setDestY(y);
			ship.setDestCom(com);
			ship.setBookmark(bookmark != 0 ? true : false);
						
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
		
		if( (ship.getOnMove() != null) && (ship.getOnMove().length() > 0) && 
			((targetx != 0) && (targety != 0)) || ((act != 5) && (act >= 1) && (act <= 9)) ) {	
			ScriptEngine scriptparser = ContextMap.getContext().get(ContextCommon.class).getScriptParser( "DSQuestScript" );
			
			final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
			
			engineBindings.put("_SHIP", ship);
			scriptparser.getContext().setErrorWriter(new NullLogger());
			engineBindings.put("DIRECTION",Integer.toString(act));
			engineBindings.put("MOVEMENTCOUNT",Integer.toString(count));
			engineBindings.put("TARGETX",Integer.toString(targetx));
			engineBindings.put("TARGETY",Integer.toString(targety));
			engineBindings.put("SECTOR", ship.getLocation().toString());
		
			Quests.currentEventURL.set("&action=onmove");

			Quests.executeEvent( scriptparser, ship.getOnMove(), user, "0", false );
			try {
				act = Integer.parseInt((String)engineBindings.get("DIRECTION"));
				count = Integer.parseInt((String)engineBindings.get("MOVEMENTCOUNT"));
				targetx = Integer.parseInt((String)engineBindings.get("TARGETX"));
				targety = Integer.parseInt((String)engineBindings.get("TARGETY"));
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
			Location from = ship.getLocation();
			route = router.findRoute(from, new Location(from.getSystem(), targetx, targety));
		}
		
		//Das Schiff soll sich offenbar bewegen
		ship.move(route, forceLowHeat, false);
		output += Ship.MESSAGE.getMessage();
		
		return output;
	}

	public void output(Parameters caller) {
		String pluginid = caller.pluginId;
		Ship data = caller.ship;
		ShipTypeData datatype = caller.shiptype;
		SchiffController controller = caller.controller;
		User user = (User)controller.getUser();
		org.hibernate.Session db = controller.getDB();
		
		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.navigation.default.html");
		
		t.setVar(	"global.pluginid",					pluginid,
					"ship.id",							data.getId(),
					"schiff.navigation.docked",			data.getDocked(),
					"schiff.navigation.docked.extern",	data.getDocked().equals("") || (data.getDocked().charAt(0) != 'l'),
					"schiff.navigation.bookmarked",		data.isBookmark(),
					"schiff.navigation.dest.x",			data.getDestX(),
					"schiff.navigation.dest.y",			data.getDestY(),
					"schiff.navigation.dest.system",	data.getDestSystem(),
					"schiff.navigation.dest.text",		data.getDestCom() );
		
		if( !data.getDocked().equals("") ) {
			String mastershipid = null;
			
			if( data.getDocked().charAt(0) == 'l' ) {
				String[] docked = data.getDocked().split(" ");
				mastershipid = docked[1];
			} else {
				mastershipid = data.getDocked();
			}
			Ship mastership = (Ship)db.get(Ship.class, Integer.parseInt(mastershipid));
			
			t.setVar(	"schiff.navigation.docked.master.name",	mastership.getName(),
						"schiff.navigation.docked.master.id",	mastershipid );
		} 
		else if( datatype.getCost() == 0 ) {
			t.setVar("schiff.navigation.showmessage","Dieses Objekt hat keinen Antrieb");
		} 
		else if( (data.getLock() != null) && !data.getLock().equals("") ) {
			t.setVar("schiff.navigation.showmessage","Fahren sie im Quest fort<br />um das Schiff wieder bewegen<br />zu k&ouml;nnen");	
		}
		else {
			int x = data.getX();
			int y = data.getY();
			int sys = data.getSystem();
			
			String[][] sectorimgs = new String[3][3];
			List jnlist = db.createQuery("from JumpNode where system=? and (x between ? and ?) and (y between ? and ?)")
				.setInteger(0, sys)
				.setInteger(1, x-1)
				.setInteger(2, x+1)
				.setInteger(3, y-1)
				.setInteger(4, y+1)
				.list();
			
			for( Iterator iter=jnlist.iterator(); iter.hasNext(); ) {
				JumpNode jn = (JumpNode)iter.next();
				sectorimgs[jn.getX()-x+1][jn.getY()-y+1] = "data/starmap/jumpnode/jumpnode.png";
			}
			
			List baselist = db.createQuery("select distinct b from Base b where b.system=? and floor(sqrt(pow(?-b.x,2)+pow(?-b.y,2))) <= b.size+1")
				.setInteger(0, sys)
				.setInteger(1, x)
				.setInteger(2, y)
				.list();
			for( Iterator iter=baselist.iterator(); iter.hasNext(); ) {
				Base aBase = (Base)iter.next();
				
				if( (aBase.getSize() == 0) && (sectorimgs[aBase.getX()-x+1][aBase.getY()-y+1] == null) ) {
					if( aBase.getOwner() == user ) {
						sectorimgs[aBase.getX()-x+1][aBase.getY()-y+1] = "data/starmap/asti_own/asti_own.png";
					}
					else {
						sectorimgs[aBase.getX()-x+1][aBase.getY()-y+1] = "data/starmap/kolonie"+aBase.getKlasse()+"_lrs/kolonie"+aBase.getKlasse()+"_lrs.png";
					}
				}
				else if( aBase.getSize() > 0 ) {
					Location loc = aBase.getLocation();
					int imgcount = 0;
					for( int by=aBase.getY()-aBase.getSize(); by <= aBase.getY()+aBase.getSize(); by++ ) {
						for( int bx=aBase.getX()-aBase.getSize(); bx <= aBase.getX()+aBase.getSize(); bx++ ) {
							if( !loc.sameSector( aBase.getSize(), new Location(aBase.getSystem(), bx, by), 0 ) ) {
								continue;	
							}
							if( Math.abs(x - bx) > 1 || Math.abs(y - by) > 1 ) {
								imgcount++;
								continue;
							}
							sectorimgs[bx-x+1][by-y+1] = "data/starmap/kolonie"+aBase.getKlasse()+"_lrs/kolonie"+aBase.getKlasse()+"_lrs"+imgcount+".png";
							imgcount++;
						}
					}
				}
			}
			
			List nebellist = db.createQuery("from Nebel where loc.system=? and (loc.x between ? and ?) and (loc.y between ? and ?)")
				.setInteger(0, sys)
				.setInteger(1, x-1)
				.setInteger(2, x+1)
				.setInteger(3, y-1)
				.setInteger(4, y+1)
				.list();
			for( Iterator iter=nebellist.iterator(); iter.hasNext(); ) {
				Nebel nebel = (Nebel)iter.next();
				sectorimgs[nebel.getX()-x+1][nebel.getY()-y+1] = "data/starmap/fog"+nebel.getType()+"/fog"+nebel.getType()+".png";
			}
			
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
								"schiff.navigation.nav.warn",			(1 != nx || 1 != ny ? Ship.getRedAlertStatus(user.getId(),sys,x+nx-1,y+ny-1) : false) );
					
					t.parse( "schiff.navigation.nav.list", "schiff.navigation.nav.listitem", true );
					newrow = false;
				}
			}
		}
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
