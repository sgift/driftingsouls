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

import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Schiffsmodul fuer die Anzeige der shivanischen Sprungantriebe
 * @author Christopher Jung
 *
 */
public class JumpdriveShivan implements SchiffPlugin {

	public String action(Parameters caller) {
		SchiffController controller = caller.controller;
		User user = controller.getUser();
		
		String output = "";
		
		Database db = controller.getDatabase();
		
		controller.parameterNumber("system");
		int system = controller.getInteger("system");
		
		if( caller.ship.getInt("owner") < 0 ) {
			controller.parameterNumber("x");
			controller.parameterNumber("y");
			int x = controller.getInteger("x");
			int y = controller.getInteger("y");
			controller.parameterString("subaction");
			String subaction = controller.getString("subaction");
			
			
			if( subaction.equals("set") && (system != 0) && (system < 99) ) {
				output += caller.ship.getString("name")+" aktiviert den Sprungantrieb<br />\n";
				db.update("INSERT INTO jumps (shipid,system,x,y) VALUES ("+caller.ship.getInt("id")+","+system+","+x+","+y+")");
				if( caller.ship.getInt("fleet") != 0 ) {
					output += "<table class=\"noBorder\">\n";
	  	
					SQLQuery s = db.query("SELECT id,name,type,status FROM ships WHERE id>0 AND fleet='"+caller.ship.getInt("fleet")+"' AND owner='"+user.getID()+"' AND docked='' AND id!='"+caller.ship.getInt("id")+"'");
					while( s.next() ) {
						SQLResultRow st = ShipTypes.getShipType(s.getRow());
						if( !ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_JUMPDRIVE_SHIVAN) ) {
							continue;	
						}
						
						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+s.getString("name")+" ("+s.getInt("id")+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff aktiviert den Sprungantrieb";
	  	
						db.update("INSERT INTO jumps (shipid,system,x,y) VALUES ("+s.getInt("id")+","+system+","+x+","+y+")");
	  	
						output += "</span></td></tr>\n";
					}
					s.free();
				}
			}
			else if ( subaction.equals("newtarget") && (system != 0) && (system < 99) ) {
				output += caller.ship.getString("name")+" &auml;ndert das Sprungziel.<br />\n";
				db.update("UPDATE jumps SET system = "+system+", x = "+x+", y = "+y+" WHERE shipid = "+caller.ship.getInt("id")+"");
				if( caller.ship.getInt("fleet") != 0 ) {
					output += "<table class=\"noBorder\">\n";
	  	
					SQLQuery s = db.query("SELECT id,name,type,status FROM ships WHERE id>0 AND fleet='"+caller.ship.getInt("fleet")+"' AND owner='"+user.getID()+"' AND docked='' AND id!='"+caller.ship.getInt("id")+"'");
					while( s.next() ) {
						SQLResultRow st = ShipTypes.getShipType(s.getRow());
						if( !ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_JUMPDRIVE_SHIVAN) ) {
							continue;	
						}
						
						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+s.getString("name")+" ("+s.getInt("id")+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff &auml;ndert das Sprungziel";
	  	
						db.update("UPDATE jumps SET sytem = "+system+", x = "+x+", y = "+y+" WHERE shipid = "+s.getInt("id")+"");
	  	
						output += "</span></td></tr>\n";
					}
					s.free();
				}
			}	
			else if ( subaction.equals("cancel") ) {
				output += caller.ship.getString("name")+" stoppt den Sprungantrieb<br />\n";
				db.update("DELETE FROM jumps WHERE shipid = "+caller.ship.getInt("id")+"");
				if( caller.ship.getInt("fleet") != 0 ) {
					output += "<table class=\"noBorder\">\n";
	  	
					SQLQuery s = db.query("SELECT id,name,type,status FROM ships WHERE id>0 AND fleet='"+caller.ship.getInt("fleet")+"' AND owner='"+user.getID()+"' AND docked='' AND id!='"+caller.ship.getInt("id")+"'");
					while( s.next() ) {
						SQLResultRow st = ShipTypes.getShipType(s.getRow());
						if( !ShipTypes.hasShipTypeFlag(st, ShipTypes.SF_JUMPDRIVE_SHIVAN) ) {
							continue;	
						}
						
						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+s.getString("name")+" ("+s.getInt("id")+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff stoppt den Sprungantrieb";
	  	
						db.update("DELETE FROM jumps WHERE shipid = "+s.getInt("id")+"");
	  	
						output += "</span></td></tr>\n";
					}
					s.free();
				}
			}
		}
		
		return output;
	}

	public void output(Parameters caller) {
		SchiffController controller = caller.controller;
		String pluginid = caller.pluginId;
		SQLResultRow ship = caller.ship;
		
		Database db = controller.getDatabase();

		TemplateEngine t = controller.getTemplateEngine();
		t.set_file("_PLUGIN_"+pluginid, "schiff.jumpdrive.shivan.html");

		SQLResultRow jump = db.first("SELECT x,y,system FROM jumps WHERE shipid=",ship.getInt("id"));

		t.set_var(	"global.pluginid",				pluginid,
					"ship.id",						ship.getInt("id"),
					"schiff.jumpdrive.jumping",		jump.isEmpty() ? 0 : jump.getInt("system"),
					"schiff.jumpdrive.jumpingx",	jump.isEmpty() ? 0 : jump.getInt("x"),
					"schiff.jumpdrive.jumpingy",	jump.isEmpty() ? 0 : jump.getInt("y"),
					"schiff.jumpdrive.subaction",	"set" );
		
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
