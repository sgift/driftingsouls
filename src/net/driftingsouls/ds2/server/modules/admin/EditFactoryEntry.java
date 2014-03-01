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
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Fabriken", name = "Fabrikeintraege editieren")
public class EditFactoryEntry implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int entryid = context.getRequest().getParameterInt("entryid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"text\" name=\"entryid\" value=\""+ entryid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");

		if(update && entryid != 0)
		{
			FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, entryid);

			if(entry != null) {
				entry.setBuildCosts(new Cargo(Cargo.Type.AUTO, context.getRequest().getParameterString("buildcosts")));
				entry.setRes1(context.getRequest().getParameterInt("res1"));
				entry.setRes2(context.getRequest().getParameterInt("res2"));
				entry.setRes3(context.getRequest().getParameterInt("res3"));
				entry.setProduce(new Cargo(Cargo.Type.AUTO, context.getRequest().getParameterString("produce")));
				entry.setDauer(BigDecimal.valueOf(Double.parseDouble(context.getRequest().getParameterString("dauer"))));
				entry.setBuildingIdString(context.getRequest().getParameterString("buildingids"));

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Kein Eintrag gefunden.</p>");
			}

		}

		if(entryid != 0)
		{
			FactoryEntry entry = (FactoryEntry)db.get(FactoryEntry.class, entryid);

			if(entry == null)
			{
				return;
			}

			List<Forschung> researchs = Common.cast(db.createQuery("from Forschung").list());

			echo.append("<div class='gfxbox' style='width:600px'>");
			echo.append("<form action=\"./ds\" method=\"post\" >");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"entryid\" value=\"" + entryid + "\" />\n");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<tr><td>Baukosten: </td><td><input type=\"hidden\" id=\"buildcosts\" name=\"buildcosts\" value=\"" + entry.getBuildCosts().toString() + "\"></td></tr>\n");
			echo.append("<tr><td>Forschung1: </td><td><select size=\"1\" name=\"res1\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == entry.getRes1() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td>Forschung2: </td><td><select size=\"1\" name=\"res2\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == entry.getRes2() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td>Forschung3: </td><td><select size=\"1\" name=\"res3\">");
			for (Forschung research: researchs)
			{
				echo.append("<option value=\"" + research.getID() + "\" " + (research.getID() == entry.getRes3() ? "selected=\"selected\"" : "") + ">" + research.getName() + "</option>");
			}
			echo.append("</select>");
			echo.append("<tr><td>Produktion: </td><td><input type=\"hidden\" id=\"produces\" name=\"produce\" value=\"" + entry.getProduce().toString() + "\"></td></tr>\n");
			echo.append("<tr><td>Dauer: </td><td><input type=\"text\" name=\"dauer\" value=\"" + entry.getDauer() + "\"></td></tr>\n");
			echo.append("<tr><td>BuildingIDs: </td><td><input type=\"text\" name=\"buildingids\" value=\"" + entry.getBuildingIdString() + "\"></td></tr>\n");


			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
			echo.append("<script type='text/javascript'>$(document).ready(function() {new CargoEditor('#buildcosts');new CargoEditor('#produces');});</script>");
			echo.append("</div>");
		}
	}
}
