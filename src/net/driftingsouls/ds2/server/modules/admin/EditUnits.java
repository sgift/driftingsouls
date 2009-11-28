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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * Aktualisierungstool fuer die Werte einer Einheit.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Einheiten", name = "Einheit editieren")
public class EditUnits implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());
		
		int unitid = context.getRequest().getParameterInt("unitid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"text\" name=\"unitid\" value=\""+ unitid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");
		
		if(update && unitid != 0)
		{
			UnitType unit = (UnitType)db.get(UnitType.class, unitid);
			
			unit.setName(context.getRequest().getParameterString("name"));
			unit.setPicture(context.getRequest().getParameterString("picture"));
			unit.setDauer(context.getRequest().getParameterInt("dauer"));
			unit.setNahrungCost(context.getRequest().getParameterInt("nahrungcost"));
			unit.setReCost(context.getRequest().getParameterInt("recost"));
			unit.setKaperValue(context.getRequest().getParameterInt("kapervalue"));
			unit.setSize(context.getRequest().getParameterInt("size"));
			unit.setDescription(context.getRequest().getParameterString("description"));
			unit.setRes(context.getRequest().getParameterInt("forschung"));
			
			Cargo cargo = new Cargo();
			
			for(Item item: itemlist)
			{
				long amount = context.getRequest().getParameterInt("i"+item.getID());
				int uses = context.getRequest().getParameterInt("i" + item.getID() + "uses");
				cargo.addResource(new ItemID(item.getID(), uses, 0), amount);
			}
			
			unit.setBuildCosts(cargo);
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(unitid != 0)
		{
			UnitType unit = (UnitType)db.get(UnitType.class, unitid);
			
			if(unit == null)
			{
				return;
			}
			
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"unitid\" value=\"" + unitid + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + unit.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + unit.getPicture() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Nahrungskosten: </td><td><input type=\"text\" name=\"nahrungcost\" value=\"" + unit.getNahrungCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">RE Kosten: </td><td><input type=\"text\" name=\"recost\" value=\"" + unit.getReCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Kaper-Wert: </td><td><input type=\"text\" name=\"kapervalue\" value=\"" + unit.getKaperValue() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Gr&ouml;&szlig;e: </td><td><input type=\"text\" name=\"size\" value=\"" + unit.getSize() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><input type=\"text\" name=\"description\" value=\"" + unit.getDescription() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ben&ouml;tigte Forschung: </td><td><input type=\"text\" name=\"forschung\" value=\"" + unit.getRes() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				long amount = unit.getBuildCosts().getResourceCount(new ItemID(item.getID()));
				int uses = 0;
				if(!unit.getBuildCosts().getItem(item.getID()).isEmpty())
				{
					uses = unit.getBuildCosts().getItem(item.getID()).get(0).getMaxUses();
				}
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"i"+item.getID()+"\" value=\"" + amount + "\"></td><td><input type=\"text\" name=\"i"+item.getID()+"u\" value=\"" + uses + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
