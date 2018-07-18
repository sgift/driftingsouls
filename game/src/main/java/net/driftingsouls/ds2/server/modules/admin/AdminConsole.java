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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.driftingsouls.ds2.server.AdminCommands;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;

/**
 * Ermoeglicht das Absetzen von Admin-Kommandos.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Sonstiges", name="Admin-Konsole", permission = WellKnownAdminPermission.CONSOLE)
public class AdminConsole implements AdminPlugin {
	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		String cmd = context.getRequest().getParameterString("cmd");
		int responseOnly = context.getRequest().getParameterInt("responseOnly");

		if( responseOnly == 1 ) {
			int autoComplete = context.getRequest().getParameterInt("autoComplete");
			if( autoComplete == 1 ) {
				JsonArray result = new JsonArray();
				for( String ac : new AdminCommands().autoComplete(cmd) ) {
					result.add(new JsonPrimitive(ac));
				}
				echo.append(result.toString());
				return;
			}
			
			echo.append(new Gson().toJson(new AdminCommands().executeCommand(cmd)));
			return;
		}
		
		if( !cmd.isEmpty() ) {
			echo.append("<div class='gfxbox' style='width:440px'>");
			echo.append("<table class=\"noBorderX\">\n");
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\">\n");
			echo.append(new AdminCommands().executeCommand(cmd).message.replace("\n", "<br />" ));
			echo.append("</td>\n");
			echo.append("</tr>\n");
			echo.append("</table>\n");
			echo.append("</div>");
			echo.append("<br /><br />\n");
		}
		echo.append("<table class=\"noBorderS\">\n");
		echo.append("<tr>\n");
		echo.append("<td class=\"noBorderS\">\n");

		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("Command: <input type=\"text\" name=\"cmd\" value=\"").append(cmd).append("\" size=\"60\" />\n");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"submit\" value=\"ausf&uuml;hren\" />\n");
		echo.append("</form>\n");
	}

}
