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
import java.util.List;

import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Adminpanel zum Bearbeiten der Munitionswerte.
 * @author Sebastian Gift
 *
 */
@AdminMenuEntry(category = "Items", name = "Munition bearbeiten")
public class EditAmmo implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		int ammoId = context.getRequest().getParameterInt("ammo");

		// Werte aktualisieren?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		if (update && ammoId > 0)
		{
			Request request = context.getRequest();
			Ammo ammo = (Ammo) db.get(Ammo.class, ammoId);
			ammo.setAreaDamage(request.getParameterInt("area"));
			ammo.setShotsPerShot(request.getParameterInt("shotspershot"));
			ammo.setFlags(request.getParameterInt("flags"));
			ammo.setDestroyable(Double.valueOf(request.getParameterString("destroyable")));
			ammo.setSubDamage(request.getParameterInt("subdamage"));
			ammo.setShieldDamage(request.getParameterInt("sdamage"));
			ammo.setDamage(request.getParameterInt("damage"));
			ammo.setTorpTrefferWS(request.getParameterInt("ttws"));
			ammo.setSubWS(request.getParameterInt("subtws"));
			ammo.setSmallTrefferWS(request.getParameterInt("stws"));
			ammo.setTrefferWS(request.getParameterInt("tws"));
			ammo.setPicture(request.getParameterString("picture"));
			ammo.setName(request.getParameterString("name"));
			db.flush();
		}
		
		// Waffenauswahl
		// Wuerg, keine Typsicherheit, aber keine Warnung. Evtl Warnung
		// unterdruecken?
		List<?> ammos = db.createQuery("FROM Ammo").list();
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"ammo\">");
		for (Object object : ammos)
		{
			Ammo ammo = (Ammo) object;
			echo.append("<option value=\"" + ammo.getId() + "\" " + (ammo.getId() == ammoId ? "selected=\"selected\"" : "") + ">" + ammo.getName() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");

		// Anzeige editieren
		
		if (ammoId > 0)
		{
			Ammo ammo = (Ammo) db.get(Ammo.class, ammoId);

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"ammo\" value=\"" + ammoId + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + ammo.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + ammo.getPicture() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Typ: </td><td><input type=\"text\" name=\"type\" value=\"" + ammo.getType() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Treffer-WS: </td><td><input type=\"text\" name=\"tws\" value=\"" + ammo.getTrefferWS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Small Treffer-WS: </td><td><input type=\"text\" name=\"stws\" value=\"" + ammo.getSmallTrefferWS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Torp Treffer-WS: </td><td><input type=\"text\" name=\"ttws\" value=\"" + ammo.getTorpTrefferWS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Subsystem Treffer-WS: </td><td><input type=\"text\" name=\"subtws\" value=\"" + ammo.getSubWS() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schaden: </td><td><input type=\"text\" name=\"damage\" value=\"" + ammo.getDamage() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schildschaden: </td><td><input type=\"text\" name=\"sdamage\" value=\"" + ammo.getShieldDamage() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Subsystemschaden: </td><td><input type=\"text\" name=\"subdamage\" value=\"" + ammo.getSubDamage() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Zerstoerbar: </td><td><input type=\"text\" name=\"destroyable\" value=\"" + ammo.getDestroyable() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flags: </td><td><input type=\"text\" name=\"flags\" value=\"" + ammo.getFlags() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schüsse pro Schuss: </td><td><input type=\"text\" name=\"shotspershot\" value=\"" + ammo.getShotsPerShot() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flächenschaden: </td><td><input type=\"text\" name=\"area\" value=\"" + ammo.getAreaDamage() + "\"></td></tr>\n");
			
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
