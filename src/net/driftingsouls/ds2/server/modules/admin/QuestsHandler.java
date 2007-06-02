/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Bearbeiten von Quest-Handlern
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Handler")
public class QuestsHandler implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		int save = context.getRequest().getParameterInt("save");
		String event = context.getRequest().getParameterString("event");
		String oid = context.getRequest().getParameterString("oid");
		String handler = context.getRequest().getParameterString("handler");
		
		Database db = context.getDatabase();
		
		final String URLBASE = "./main.php?module=admin&sess="+context.getSession()+"&page="+page+"&act="+action;
		
		if( event.length() == 0 ) {
			echo.append(Common.tableBegin(700, "center"));
			echo.append("<form action=\"./main.php\" method=\"post\">\n");
			echo.append("<select size=\"1\" name=\"event\">\n");
			echo.append("<option value=\"oncommunicate\">oncommunicate</option>\n");
			echo.append("<option value=\"onenter\">onenter</option>\n");
			echo.append("<option value=\"onmove\">onmove</option>\n");
			echo.append("<option value=\"ontick_rquest\">ontick (rQuest)</option>\n");
			echo.append("<option value=\"onendbattle\">onendbattle</option>\n");
			echo.append("</select>\n");
			echo.append("<input type=\"text\" name=\"oid\" value=\"object-id\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");
			
			echo.append(Common.tableEnd());	
			
			echo.append("<br /><br />\n");
			
			echo.append(Common.tableBegin(700,"left"));
			
			echo.append("oncommunicate:<br />\n");
			SQLQuery ship = db.query("SELECT id,name,owner " +
					"FROM ships " +
					"WHERE id>0 AND oncommunicate IS NOT NULL " +
					"ORDER BY id");
			while( ship.next() ) {
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=oncommunicate&oid="+ship.getInt("id")+"\">");
				
				User owner = context.createUserObject(ship.getInt("owner"));
				
				echo.append(ship.getString("name")+" ("+ship.getInt("id")+") ["+Common._title(owner.getName())+"]");
				
				echo.append("</a><br />\n");
			}
			ship.free();
			
			echo.append("<br />onenter:<br />\n");
			SQLQuery sector = db.query("SELECT system,x,y FROM sectors WHERE onenter IS NOT NULL ORDER BY system,x,y");
			while( sector.next() ) {
				String sys = sector.getInt("system")+":"+sector.getInt("x")+"/"+sector.getInt("y");
				
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=onenter&oid="+sys+"\">");
				
				echo.append(sys);
				
				echo.append("</a><br />\n");
			}
			sector.free();
			
			echo.append("<br />onmove:<br />\n");
			ship = db.query("SELECT id,name,owner " +
					"FROM ships " +
					"WHERE id>0 AND onmove IS NOT NULL " +
					"ORDER BY id");
			while( ship.next() ) {
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=onmove&oid="+ship.getInt("id")+"\">");
				
				User owner = context.createUserObject(ship.getInt("owner"));
				
				echo.append(ship.getString("name")+" ("+ship.getInt("id")+") ["+Common._title(owner.getName())+"]");
				
				echo.append("</a><br />\n");
			}
			ship.free();
			
			echo.append("<br />ontick (rQuest):<br />\n");
			SQLQuery rquest = db.query("SELECT qr.id,q.name,qr.userid " +
					"FROM quests_running qr JOIN quests q ON qr.questid=q.id " +
					"WHERE qr.ontick IS NOT NULL " +
					"ORDER BY qr.userid");
			while( rquest.next() ) {
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=ontick_rquest&oid="+rquest.getInt("id")+"\">");
				
				User owner = context.createUserObject(rquest.getInt("userid"));
				
				echo.append(rquest.getString("name")+" ("+rquest.getInt("id")+") ["+Common._title(owner.getName())+"]");
				
				echo.append("</a><br />\n");
			}
			rquest.free();
			
			echo.append("<br />onendbattle:<br />\n");
			SQLQuery battle = db.query("SELECT id,x,y,system,commander1,commander2 " +
					"FROM battles " +
					"WHERE onend IS NOT NULL " +
					"ORDER BY system,x,y");
			while( battle.next() ) {
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=onendbattle&oid="+battle.getInt("id")+"\">");
				
				User com1 = context.createUserObject(battle.getInt("commander1"));
				User com2 = context.createUserObject(battle.getInt("commander2"));
				
				echo.append(battle.getInt("system")+":"+battle.getInt("x")+"/"+battle.getInt("y")+" ("+battle.getInt("id")+") ["+Common._title(com1.getName())+" vs "+Common._title(com2.getName())+"]");
				
				echo.append("</a><br />\n");
			}
			battle.free();
			
			echo.append(Common.tableEnd());
		}
		else if( save == 0 ) {
			if( event.equals("oncommunicate") ) {
				handler = db.first("SELECT oncommunicate FROM ships WHERE id>0 AND id="+Integer.parseInt(oid)).getString("oncommunicate");	
			}
			else if( event.equals("onmove") ) {
				handler = db.first("SELECT onmove FROM ships WHERE id>0 AND id="+Integer.parseInt(oid)).getString("onmove");	
			}
			else if( event.equals("onenter") ) {
				Location loc = Location.fromString(oid);

				handler = db.first("SELECT onenter FROM sectors WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY()).getString("onenter");	
			}
			else if( event.equals("ontick_rquest") ) {
				handler = db.first("SELECT ontick FROM quests_running WHERE id="+Integer.parseInt(oid)).getString("ontick");	
			}
			else if( event.equals("onendbattle") ) {
				handler = db.first("SELECT onend FROM battles WHERE id="+Integer.parseInt(oid)).getString("onend");	
			}
			else {
				echo.append("WARNUNG: Ung&uuml;ltiges Event &gt;"+event+"&lt; <br />\n");
				handler = "";
				event = "";
				oid = "";	
			}
		
			echo.append(Common.tableBegin(700,"left"));
		
			echo.append("<form action=\"./main.php\" method=\"post\">\n");
			echo.append("<input type=\"text\" name=\"handler\" size=\"50\" value=\""+handler+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"event\" value=\""+event+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"oid\" value=\""+oid+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"save\" value=\"1\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");
			
			echo.append(Common.tableEnd());
		}
		else {
			if( event.equals("oncommunicate") ) {
				if( handler.length() != 0 ) {
					db.update("UPDATE ships SET oncommunicate='"+handler+"' WHERE id>0 AND id="+Integer.parseInt(oid));
				}
				else {
					db.update("UPDATE ships SET oncommunicate=NULL WHERE id>0 AND id="+Integer.parseInt(oid));
				}	
			}
			else if( event.equals("onmove") ) {
				if( handler.length() != 0 ) {
					db.update("UPDATE ships SET onmove='"+handler+"' WHERE id>0 AND id="+Integer.parseInt(oid));
				}
				else {
					db.update("UPDATE ships SET onmove=NULL WHERE id>0 AND id="+Integer.parseInt(oid));
				}	
			}
			else if( event.equals("onenter") ) {
				Location loc = Location.fromString(oid);
				
				if( handler.length() != 0 ) {
					SQLResultRow myx = db.first("SELECT x FROM sectors WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY());
					if( myx.isEmpty() ) {
						db.query("INSERT INTO sectors (system,x,y,onenter) VALUES ("+loc.getSystem()+","+loc.getX()+","+loc.getY()+",'"+handler+"')");
					}
					else {
						db.update("UPDATE sectors SET onenter='"+handler+"' WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY());
					}
				}
				else {
					db.update("DELETE FROM sectors WHERE system="+loc.getSystem()+" AND x="+loc.getX()+" AND y="+loc.getY());
				}	
			}
			else if( event.equals("ontick_rquest") ) {
				if( handler.length() != 0 ) {
					db.update("UPDATE quests_running SET ontick='"+handler+"' WHERE id="+Integer.parseInt(oid));
				}
				else {
					db.update("UPDATE quests_running SET ontick=NULL WHERE id="+Integer.parseInt(oid));
				}
			}
			else if( event.equals("onendbattle") ) {
				if( handler.length() != 0 ) {
					db.update("UPDATE battles SET onend='"+handler+"' WHERE id="+Integer.parseInt(oid));
				}
				else {
					db.update("UPDATE battles SET onend=NULL WHERE id="+Integer.parseInt(oid));
				}	
			}
			else {
				echo.append("WARNUNG: Ung&uuml;ltiges Event &gt;"+event+"&lt; <br />\n");	
			}
			echo.append("&Auml;nderungen durchgef&uuml;hrt<br />");
		}
	}
}
