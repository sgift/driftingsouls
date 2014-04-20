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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Quality;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffectFactory;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.io.IOException;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Items", name = "Item editieren", permission = WellKnownAdminPermission.EDIT_ITEM)
public class EditItem implements AdminPlugin
{
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int itemid = context.getRequest().getParameterInt("itemid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"text\" name=\"itemid\" value=\"").append(itemid).append("\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");

		if(update && itemid != 0)
		{
			Item item = (Item)db.get(Item.class, itemid);

			if(item != null) {
				item.setName(context.getRequest().getParameterString("name"));
				item.setPicture(context.getRequest().getParameterString("picture"));
				item.setLargePicture(context.getRequest().getParameterString("largepicture"));
				item.setCargo(context.getRequest().getParameterInt("itemcargo"));
				item.setEffect(ItemEffectFactory.fromContext(context));
				item.setQuality(Quality.fromString(context.getRequest().getParameterString("quality")));
				item.setDescription(context.getRequest().getParameterString("description"));
				item.setHandel(context.getRequest().getParameterString("handel").equals("true"));
				item.setAccessLevel(context.getRequest().getParameterInt("accesslevel"));
				item.setUnknownItem(context.getRequest().getParameterString("unknownitem").equals("true"));
				item.setSpawnableRess(context.getRequest().getParameterString("spawnableress").equals("true"));

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Kein Item gefunden.</p>");
			}

		}

		if(itemid != 0)
		{
			Item item = (Item)db.get(Item.class, itemid);

			if(item == null)
			{
				return;
			}

			echo.append("<div class='gfxbox' style='width:500px'>");
			echo.append("<form action=\"./ds\" method=\"post\" >");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"itemid\" value=\"").append(itemid).append("\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"").append(item.getName()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild: </td><td><input type=\"text\" name=\"picture\" size='50' value=\"").append(item.getPicture()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild (gro&szlig;): </td><td><input type=\"text\" size='50' name=\"largepicture\" value=\"").append(item.getLargePicture() == null ? "" : item.getLargePicture()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Cargo: </td><td><input type=\"text\" name=\"itemcargo\" value=\"").append(item.getCargo()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Qualit&auml;t: </td><td><input type=\"text\" name=\"quality\" value=\"").append(item.getQuality().toString()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><input type=\"text\" name=\"description\" value=\"").append(item.getDescription()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Handel: </td><td><input type=\"text\" name=\"handel\" value=\"").append(item.getHandel()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Accesslevel: </td><td><input type=\"text\" name=\"accesslevel\" value=\"").append(item.getAccessLevel()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Unbekanntes Item: </td><td><input type=\"text\" name=\"unknownitem\" value=\"").append(item.isUnknownItem()).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Spawn-Ressource: </td><td><input type=\"text\" name=\"spawnableress\" value=\"").append(item.isSpawnableRess()).append("\"></td></tr>\n");
			item.getEffect().getAdminTool(echo);
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("</div>");
		}
	}
}
