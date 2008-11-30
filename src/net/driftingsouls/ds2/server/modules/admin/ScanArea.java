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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * UI zur Eingabe eines Scanbereichs. Der eigendliche Scan wird dann vom
 * Modul 'scan' durchgefuehrt 
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Objekte", name="Bereich scannen")
public class ScanArea implements AdminPlugin {

	public void output(AdminController controller, String page, int action) throws IOException {
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		
		echo.append("<form action=\"./ds\" method=\"post\">\n");
		echo.append("<table class=\"noBorder\">\n");
		echo.append("<tr><td class=\"noBorderS\" width=\"60\">Pos:</td><td class=\"noBorderS\">\n");
		echo.append("<input type=\"text\" name=\"baseloc\" value=\"1:100/100\" size=\"13\" />:\n");
		echo.append("</td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">radius:</td><td class=\"noBorderS\"><input type=\"text\" name=\"range\" value=\"5\" size=\"3\" /></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\" colspan=\"2\" align=\"center\">\n");
		echo.append("<input type=\"hidden\" name=\"admin\" value=\"1\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"scan\" />\n");
		echo.append("<input type=\"submit\" value=\"scan\" style=\"width:100px\"/></td></tr>");
		echo.append("</form>");
	}
}
