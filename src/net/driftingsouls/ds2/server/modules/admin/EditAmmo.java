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
import java.math.BigDecimal;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Adminpanel zum Bearbeiten der Munitionswerte.
 * @author Sebastian Gift
 *
 */
@AdminMenuEntry(category = "Waffen", name = "Munition bearbeiten")
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
			Common.dblog("changeammo", "", "", "typeid", Integer.toString(ammoId));
			Request request = context.getRequest();
			Ammo ammo = (Ammo) db.get(Ammo.class, ammoId);
			ammo.setAreaDamage(request.getParameterInt("area"));
			ammo.setBuildCosts(new Cargo(Cargo.Type.STRING, request.getParameterString("buildcosts")));
			ammo.setDauer(new BigDecimal(request.getParameterString("buildtime")));
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
			ammo.setReplaces((Ammo) db.get(Ammo.class, request.getParameterInt("replace")));
			ammo.setRes1(request.getParameterInt("res1"));
			ammo.setRes2(request.getParameterInt("res2"));
			ammo.setRes3(request.getParameterInt("res3"));
			ammo.setDescription(request.getParameterString("description"));
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
		final int MAX_DEPENDENCIES = 3; // Maximale Anzahl
										// Forschungsvoraussetzungen
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
			echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><textarea cols=\"50\" rows=\"10\" name=\"description\">" + ammo.getDescription() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ersetzt: </td><td class=\"noBorderS\">");
			echo.append("<select size=\"1\" name=\"replace\">");
			int ammoReplaceId = 0;
			if (ammo.getReplaces() != null)
			{
				ammoReplaceId = ammo.getReplaces().getId();
			}
			echo.append("<option value=\"0\"" + (0 == ammoReplaceId ? "selected=\"selected\"" : "") + ">(ersetzt nichts)</option>");
			for (Object object : ammos)
			{
				Ammo replace = (Ammo) object;
				ammoReplaceId = 0;
				if (ammo.getReplaces() != null)
				{
					ammoReplaceId = ammo.getReplaces().getId();
				}
				echo.append("<option value=\"" + replace.getId() + "\"" + (replace.getId() == ammoReplaceId ? "selected=\"selected\"" : "") + ">" + replace.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("</td></tr>");
			List<?> researches = db.createQuery("FROM Forschung").list();
			for (int i = 1; i <= MAX_DEPENDENCIES; i++)
			{
				echo.append("<tr><td class=\"noBorderS\">");
				echo.append("Voraussetzung " + i);
				echo.append("</td><td class=\"noBorderS\">");
				echo.append("<select size=\"1\" name=\"res" + i + "\">");
				echo.append("<option value=\"0\"" + (0 == ammo.getRes(i) ? "selected=\"selected\"" : "") + ">(keine Voraussetzung)</option>");
				echo.append("<option value=\"-1\"" + (-1 == ammo.getRes(i) ? "selected=\"selected\"" : "") + ">(nicht erforschbar)</option>");
				for (Object object : researches)
				{
					Forschung research = (Forschung) object;
					echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == ammo.getRes(i) ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
				}
				echo.append("</select>");
				echo.append("</td></tr>");
			}
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
			echo.append("<tr><td class=\"noBorderS\">Bauzeit: </td><td><input type=\"text\" name=\"buildtime\" value=\"" + ammo.getDauer() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Baukosten: </td><td><input type=\"text\" name=\"buildcosts\" value=\"" + ammo.getBuildCosts() + "\"></td></tr>\n");

			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
