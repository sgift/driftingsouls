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

import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;
import java.util.List;

/**
 * Loeschtool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Systeme", name = "System loeschen")
public class DeleteSystem implements AdminPlugin
{
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int systemid = context.getRequest().getParameterInt("systemid");

		// Update values?
		boolean update = context.getRequest().getParameterString("delete").equals("Ok");
		List<?> systems = db.createQuery("from StarSystem").list();

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"systemid\">");
		for (Object system1 : systems)
		{
			StarSystem system = (StarSystem) system1;

			echo.append("<option value=\"").append(system.getID()).append("\" ").append(system.getID() == systemid ? "selected=\"selected\"" : "").append(">").append(system.getName()).append(" (").append(system.getID()).append(")</option>");
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
