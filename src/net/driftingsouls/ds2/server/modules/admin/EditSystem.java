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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Aktualisierungstool fuer die Systeme.
 * 
 */
@AdminMenuEntry(category = "Systeme", name = "System editieren")
public class EditSystem implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int systemid = context.getRequest().getParameterInt("systemid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
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
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");

		if (update && systemid > 0)
		{
			Request request = context.getRequest();
			
			String name = request.getParameterString("name");
			int width = request.getParameterInt("width");
			int height = request.getParameterInt("height");
			boolean military = request.getParameterString("military").equals("true") ? true : false; 
			int maxcolonies = request.getParameterInt("maxcolonies");
			boolean starmap = request.getParameterString("starmap").equals("true") ? true : false;
			String orderloc = request.getParameterString("orderloc");
			String gtuDropZoneString = request.getParameterString("gtuDropZone");
			int access = request.getParameterInt("access");
			String descrip = request.getParameterString("descrip");
			String spawnableress = request.getParameterString("spawnableress");
			
			StarSystem system = (StarSystem) db.createQuery("from StarSystem where id=?").setInteger(0, systemid).uniqueResult();
			
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
			
			// Update ships
			int count = 0;

			ScrollableResults ships = db.createQuery("from Ship where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while (ships.next())
			{
				Ship ship = (Ship) ships.get(0);
				if(ship.getX() > system.getWidth()) {
					ship.setX(system.getWidth());
				}
				if(ship.getY() > system.getHeight()) {
					ship.setY(system.getHeight());
				}
				count++;
				if (count % 20 == 0)
				{
					db.flush();
					HibernateFacade.evictAll(db, Ship.class);
				}
			}
			
			ScrollableResults battles = db.createQuery("from Battle where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while (battles.next())
			{
				Battle battle = (Battle) battles.get(0);
				if(battle.getX() > system.getWidth()) 
				{
					battle.setX(system.getWidth());
				}
				if(battle.getY() > system.getHeight())
				{
					battle.setY(system.getHeight());
				}
				db.flush();
				HibernateFacade.evictAll(db, Battle.class);
			}
			
			ScrollableResults bases = db.createQuery("from Base where system = :system").setInteger("system", system.getID()).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while(bases.next())
			{
				Base base = (Base) bases.get(0);
				if(base.getX() > system.getWidth())
				{
					base.setX(system.getWidth());
				}
				if(base.getY() > system.getHeight())
				{
					base.setY(system.getHeight());
				}
				db.flush();
				HibernateFacade.evictAll(db, Base.class);
			}
			
			echo.append("<p>Update abgeschlossen.</p>");
		}

		// Ship choosen - get the values
		if (systemid > 0)
		{
			StarSystem system = (StarSystem) db.createQuery("from StarSystem where id=?").setInteger(0, systemid).uniqueResult();

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"systemid\" value=\"" + systemid + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + system.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Breite: </td><td><input type=\"text\" name=\"width\" value=\"" + system.getWidth() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">H&ouml;he: </td><td><input type=\"text\" name=\"height\" value=\"" + system.getHeight() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Allow Military: </td><td><input type=\"text\" name=\"military\" value=\"" + system.isMilitaryAllowed() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Max Colonies (-1 = keine Begrenzung): </td><td><input type=\"text\" name=\"maxcolonies\" value=\"" + system.getMaxColonies() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">In Sternenkarte sichtbar: </td><td><input type=\"text\" name=\"starmap\" value=\"" + system.isStarmapVisible() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">OrderLocations(Form: x/y|x/y): </td><td><input type=\"text\" name=\"orderloc\" value=\"" + system.getOrderLocationString() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">GTU Dropzone(Form: x/y): </td><td><input type=\"text\" name=\"gtuDropZone\" value=\"" + (system.getDropZone() != null ? (system.getDropZone().equals(new Location( 0, 0, 0 )) ? "" : system.getDropZoneString()) : "") + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Zugriffsrechte(1=Jeder;2=NPC;3=Admin): </td><td><input type=\"text\" name=\"access\" value=\"" + system.getAccess() + "\"></td></tr>\n");		
			echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><textarea cols=\"50\" rows=\"10\" name=\"descrip\">" + system.getDescription() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ressourcenvorkommen: </td><td><input type=\"text\" name=\"spawnableress\" value=\"" + system.getSpawnableRess() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
