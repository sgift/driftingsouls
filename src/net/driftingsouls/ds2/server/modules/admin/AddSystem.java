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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer die Systeme.
 * 
 */
@AdminMenuEntry(category = "Systeme", name = "System hinzuf&uuml;gen")
public class AddSystem implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Hinzufuegen");

		if (update)
		{
			Request request = context.getRequest();
			
			int id = request.getParameterInt("id");
			String name = request.getParameterString("name");
			int width = request.getParameterInt("width");
			int height = request.getParameterInt("height");
			boolean military = request.getParameterString("military").equals("true");
			int maxcolonies = request.getParameterInt("maxcolonies");
			boolean starmap = request.getParameterString("starmap").equals("true");
			String orderloc = request.getParameterString("orderloc");
			String gtuDropZoneString = request.getParameterString("gtuDropZone");
			int access = request.getParameterInt("access");
			String descrip = request.getParameterString("descrip");
			String spawnableress = request.getParameterString("spawnableress");
			
			StarSystem oldsystem = (StarSystem)db.get(StarSystem.class, id);
			if( oldsystem != null) {
				echo.append("System-id bereits belegt.");
				return;
			}
			
			StarSystem system = new StarSystem(id);
			
			system.setName(name);
			system.setWidth(width);
			system.setHeight(height);
			system.setMilitaryAllowed(military);
			system.setMaxColonies(maxcolonies);
			system.setStarmapVisible(starmap);
			system.setOrderLocations(orderloc);
			if(gtuDropZoneString != "") {
				system.setDropZone(Location.fromString(gtuDropZoneString));
			}
			else
			{
				system.setDropZone(new Location(0, 0, 0));
			}
			system.setAccess(access);
			system.setDescription(descrip);
			system.setSpawnableRess(spawnableress);
			
			db.save(system);
			
			echo.append("System hinzugefuegt");
		}

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<table class=\"noBorder\" width=\"100%\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<tr><td class=\"noBorderS\">ID: </td><td><input type=\"text\" name=\"id\" value=\"0\" ></td></tr>");
		echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Breite: </td><td><input type=\"text\" name=\"width\" value=\"200\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">H&ouml;he: </td><td><input type=\"text\" name=\"height\" value=\"200\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Allow Military: </td><td><input type=\"text\" name=\"military\" value=\"true\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Max Colonies (-1 = keine Begrenzung): </td><td><input type=\"text\" name=\"maxcolonies\" value=\"-1\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">In Sternenkarte sichtbar: </td><td><input type=\"text\" name=\"starmap\" value=\"false\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">OrderLocations(Form: x/y|x/y): </td><td><input type=\"text\" name=\"orderloc\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">GTU Dropzone(Form: x/y): </td><td><input type=\"text\" name=\"gtuDropZone\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Zugriffsrechte(1=Jeder;2=NPC;3=Admin): </td><td><input type=\"text\" name=\"access\" value=\"" + StarSystem.AC_NORMAL + "\"></td></tr>\n");		
		echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><textarea cols=\"50\" rows=\"10\" name=\"descrip\"></textarea></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Ressourcenvorkommen: </td><td><input type=\"text\" name=\"spawnableress\" ></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Hinzufuegen\"></td></tr>\n");
		echo.append("</table>");
		echo.append("</form>\n");
	}
}
