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
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

import org.apache.commons.lang.StringUtils;

/**
 * Ermoeglicht das Absetzen von Admin-Kommandos
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Sonstiges", name="Admin-Konsole")
public class AdminConsole implements AdminPlugin {

	public void output(AdminController controller, String page, int action) {
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		
		String cmd = context.getRequest().getParameterString("cmd");
		int cleanpage = context.getRequest().getParameterInt("cleanpage");
		int responseOnly = context.getRequest().getParameterInt("responseOnly");

		if( responseOnly == 1 ) {
			echo.append(StringUtils.replace( AdminCommands.executeCommand(cmd), "\n", "<br />" ));
			return;
		}
		
		if( (cmd.length() > 0) && (cleanpage == 0) ) {
			echo.append(Common.tableBegin(400,"left"));
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append(StringUtils.replace( AdminCommands.executeCommand(cmd), "\n", "<br />" ));
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("</table>\n");
			echo.append(Common.tableEnd());
			echo.append("<br /><br />\n");
		}
		if( cleanpage == 0 ) {
			echo.append("<table class=\"noBorderS\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderS\">\n");
		}
		
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("Command: <input type=\"text\" name=\"cmd\" value=\""+cmd+"\" size=\"60\" />\n");
		echo.append("<input type=\"hidden\" name=\"sess\" value=\""+context.getSession()+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"page\" value=\""+page+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\""+action+"\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		if( cleanpage != 0 ) {
			echo.append("<input type=\"hidden\" name=\"cleanpage\" value=\"1\" />\n");	
		}
		echo.append("<input type=\"submit\" value=\"ausf&uuml;hren\" />\n");
		echo.append("</form>\n");
		
		if( (cmd.length() > 0) && (cleanpage != 0) ) {
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\" style=\"width:20px;text-align:center\">\n");
			echo.append("-\n");
			echo.append("</td>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append(StringUtils.replace( AdminCommands.executeCommand(cmd), "\n", "<br />" ));
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("</table>\n");
		}
	}

}
