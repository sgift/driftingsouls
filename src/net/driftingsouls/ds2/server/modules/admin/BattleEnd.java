/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.modules.admin.AdminMenuEntry;
import net.driftingsouls.ds2.server.modules.admin.AdminPlugin;

/**
 * Ermoeglicht das Beenden von Schlachten
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Sonstiges", name="Schlacht beenden")
public class BattleEnd implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		int battleid = context.getRequest().getParameterInt("battleid");
				
		if( battleid == 0 ) {
			echo.append(Common.tableBegin(500, "center"));
			
			Database db = context.getDatabase();
			
			echo.append("<table class=\"noBorderX\" width=\"100%\">\n");
			SQLQuery abattle = db.query("SELECT * FROM battles");
			while( abattle.next() ) {
				echo.append("<tr>\n");
				echo.append("<td class=\"noBorderX\">ID "+abattle.getInt("id")+"&nbsp;&nbsp;</td>\n");
				echo.append("<td class=\"noBorderX\">"+abattle.getInt("system")+":"+abattle.getInt("x")+"/"+abattle.getInt("y")+"</td>\n");
				
				String commander1 = null;
				String commander2 = null;
				
				if( abattle.getInt("ally1") != 0 ) {
					commander1 = Common._title(db.first("SELECT name FROM ally " +
							"WHERE id="+abattle.getInt("ally1")).getString("name"));
				}
				else {
					final User commander1Obj = (User)context.getDB().get(User.class, abattle.getInt("commander1"));
					commander1 = Common._title(commander1Obj.getName());
				}
				if( abattle.getInt("ally2") != 0 ) {
					commander2 = Common._title(db.first("SELECT name FROM ally " +
							"WHERE id="+abattle.getInt("ally2")).getString("name"));
				}
				else {
					final User commander2Obj = (User)context.getDB().get(User.class, abattle.getInt("commander2"));
					commander2 = Common._title(commander2Obj.getName());
				} 
				echo.append("<td class=\"noBorderX\" style=\"text-align:center\">"+commander1+"<br />vs<br />"+commander2+"</td>\n");
				echo.append("</tr>\n");
			}
			abattle.free();
			echo.append("</table>\n");
			echo.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />\n");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("BattleID: <input type=\"text\" name=\"battleid\" value=\"0\" />\n");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />");
			echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />");
			echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");		
			echo.append("<input type=\"submit\" value=\"beenden\" style=\"width:100px\"/>");
			echo.append("</form>");
			
			echo.append(Common.tableEnd());
		}
		else {
			AdminCommands.executeCommand("battle end "+battleid);
					
			echo.append("Schlacht beendet<br />");
		}
	}
}
