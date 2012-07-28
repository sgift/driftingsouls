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
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Loeschtool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Systeme", name = "System loeschen")
public class DeleteSystem implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int systemid = context.getRequest().getParameterInt("systemid");

		// Update values?
		boolean update = context.getRequest().getParameterString("delete").equals("Ok");
		List<?> systems = db.createQuery("from StarSystem").list();

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"systemid\">");
		for (Iterator<?> iter = systems.iterator(); iter.hasNext();)
		{
			StarSystem system = (StarSystem) iter.next();

			echo.append("<option value=\"" + system.getID() + "\" " + (system.getID() == systemid ? "selected=\"selected\"" : "") + ">" + system.getName() + " (" + system.getID() + ")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");

		if (update && systemid > 0)
		{
			StarSystem system = (StarSystem) db.get(StarSystem.class, systemid);

			db.delete(system);

			echo.append("<p>System geloescht.</p>");
		}
	}
}
