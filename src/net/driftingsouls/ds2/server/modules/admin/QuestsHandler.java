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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Ermoeglicht das Bearbeiten von Quest-Handlern.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Quests", name="Handler")
public class QuestsHandler implements AdminPlugin {
	@Override
	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();

		int save = context.getRequest().getParameterInt("save");
		String event = context.getRequest().getParameterString("event");
		String oid = context.getRequest().getParameterString("oid");
		String handler = context.getRequest().getParameterString("handler");

		Database db = context.getDatabase();

		final String URLBASE = "./ds?module=admin&page="+page+"&act="+action;

		if( event.length() == 0 ) {
			echo.append("<div class='gfxbox' style='width:740px;text-align:center'>");
			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<select size=\"1\" name=\"event\">\n");
			echo.append("<option value=\"oncommunicate\">oncommunicate</option>\n");
			echo.append("<option value=\"ontick_rquest\">ontick (rQuest)</option>\n");
			echo.append("<option value=\"onendbattle\">onendbattle</option>\n");
			echo.append("</select>\n");
			echo.append("<input type=\"text\" name=\"oid\" value=\"object-id\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");

			echo.append("</div>");

			echo.append("<br /><br />\n");

			echo.append("<div class='gfxbox' style='width:740px'>");

			echo.append("oncommunicate:<br />\n");
			SQLQuery ship = db.query("SELECT id,name,owner " +
					"FROM ships " +
					"WHERE id>0 AND oncommunicate IS NOT NULL " +
					"ORDER BY id");
			while( ship.next() ) {
				echo.append("* <a class=\"forschinfo\" href=\""+URLBASE+"&event=oncommunicate&oid="+ship.getInt("id")+"\">");

				User owner = (User)context.getDB().get(User.class, ship.getInt("owner"));

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

				User owner = (User)context.getDB().get(User.class, rquest.getInt("userid"));

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

				User com1 = (User)context.getDB().get(User.class, battle.getInt("commander1"));
				User com2 = (User)context.getDB().get(User.class, battle.getInt("commander2"));

				echo.append(battle.getInt("system")+":"+battle.getInt("x")+"/"+battle.getInt("y")+" ("+battle.getInt("id")+") ["+Common._title(com1.getName())+" vs "+Common._title(com2.getName())+"]");

				echo.append("</a><br />\n");
			}
			battle.free();

			echo.append("</div>");
		}
		else if( save == 0 ) {
			if( event.equals("oncommunicate") ) {
				handler = db.first("SELECT oncommunicate FROM ships WHERE id>0 AND id="+Integer.parseInt(oid)).getString("oncommunicate");
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

			echo.append("<div class='gfxbox' style='width:740px'>");

			echo.append("<form action=\"./ds\" method=\"post\">\n");
			echo.append("<input type=\"text\" name=\"handler\" size=\"50\" value=\""+handler+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"event\" value=\""+event+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"oid\" value=\""+oid+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"save\" value=\"1\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"bearbeiten\" />\n");
			echo.append("</form>\n");

			echo.append("</div>");
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
