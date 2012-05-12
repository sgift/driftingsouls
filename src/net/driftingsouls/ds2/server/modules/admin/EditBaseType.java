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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer die Basis-Klassen.
 * 
 */
@AdminMenuEntry(category = "Asteroiden", name = "Basis-Klasse editieren")
public class EditBaseType implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		
		int typeid = context.getRequest().getParameterInt("typeid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append(Common.tableBegin(350,"left"));
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("Basis: <input type=\"text\" name=\"typeid\" value=\""+ typeid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		echo.append(Common.tableEnd());
		
		if(update && typeid != 0)
		{
			BaseType type = (BaseType)db.get(BaseType.class, typeid);
			
			type.setName(context.getRequest().getParameterString("name"));
			type.setEnergy(context.getRequest().getParameterInt("energie"));
			type.setCargo(context.getRequest().getParameterInt("cargo"));
			type.setWidth(context.getRequest().getParameterInt("width"));
			type.setHeight(context.getRequest().getParameterInt("height"));
			type.setMaxTiles(context.getRequest().getParameterInt("maxtiles"));
			type.setTerrain(Common.explodeToInteger(";", context.getRequest().getParameterString("terrain")));
			type.setSpawnableRess(context.getRequest().getParameterString("spawnableress"));
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(typeid != 0)
		{
			BaseType type = (BaseType)db.get(BaseType.class, typeid);
			
			if(type == null)
			{
				return;
			}

			echo.append(Common.tableBegin(650,"left"));
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorderX\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"typeid\" value=\"" + typeid + "\" />\n");
			echo.append("<tr><td class=\"noBorderX\">Name: </td><td><input type=\"text\" size=\"40\" name=\"name\" value=\"" + type.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Energie: </td><td><input type=\"text\" size=\"40\" name=\"energie\" value=\"" + type.getEnergy() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Cargo: </td><td><input type=\"text\" size=\"40\" name=\"cargo\" value=\"" + type.getCargo() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Breite: </td><td><input type=\"text\" size=\"40\" name=\"width\" value=\"" + type.getWidth() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">H&ouml;he: </td><td><input type=\"text\" size=\"40\" name=\"height\" value=\"" + type.getHeight() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Feldergr&ouml;&szlig;e: </td><td><input type=\"text\" size=\"40\" name=\"maxtiles\" value=\"" + type.getMaxTiles() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Terrain: </td><td><input type=\"text\" size=\"40\" name=\"terrain\" value=\"" + (type.getTerrain() == null ? "" : Common.implode(";", type.getTerrain()))  + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Zum Spawn freigegebene Ressourcen: </td><td><input type=\"text\" size=\"40\" name=\"spawnableress\" value=\"" + type.getSpawnableRess() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append(Common.tableEnd());
		}
	}
}
