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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.Rasse;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.ShipBaubar;

/**
 * Aktualisierungstool fuer die Kosten von Schiffstypen.
 */
@AdminMenuEntry(category = "Schiffe", name = "Baukosten editieren")
public class EditShipCosts implements AdminPlugin
{
	
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());

		int shiptypeId = context.getRequest().getParameterInt("shiptype");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		List<?> shiptypes = db.createQuery("from ShipBaubar").list();

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"shiptype\">");
		for (Iterator<?> iter = shiptypes.iterator(); iter.hasNext();)
		{
			ShipBaubar shiptype = (ShipBaubar) iter.next();

			echo.append("<option value=\"" + shiptype.getId() + "\" " + (shiptype.getId() == shiptypeId ? "selected=\"selected\"" : "") + ">" + shiptype.getType().getNickname() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");

		if (update && shiptypeId > 0)
		{
			Request request = context.getRequest();

			int ekosten = request.getParameterInt("ekosten");
			int crew = request.getParameterInt("crew");
			int dauer = request.getParameterInt("dauer");
			int race = request.getParameterInt("race");
			int werftslots = request.getParameterInt("werftslots");
			boolean flagschiff = request.getParameterString("flagschiff").trim().toLowerCase().equals("true");
			int tr1 = request.getParameterInt("tr1");
			int tr2 = request.getParameterInt("tr2");
			int tr3 = request.getParameterInt("tr3");
			
			ShipBaubar shiptype = (ShipBaubar) db.createQuery("from ShipBaubar where id=?").setInteger(0, shiptypeId).uniqueResult();
			
			shiptype.setEKosten(ekosten);
			shiptype.setCrew(crew);
			shiptype.setDauer(dauer);
			shiptype.setRace(race);
			shiptype.setWerftSlots(werftslots);
			shiptype.setFlagschiff(flagschiff);
			shiptype.setRes1(tr1);
			shiptype.setRes2(tr2);
			shiptype.setRes3(tr3);
			
			Cargo cargo = new Cargo();
			
			for(Item item: itemlist)
			{
				long amount = context.getRequest().getParameterInt("i"+item.getID());
				int uses = context.getRequest().getParameterInt("i" + item.getID() + "uses");
				cargo.addResource(new ItemID(item.getID(), uses, 0), amount);
			}
			
			shiptype.setCosts(cargo);
			
			echo.append("<p>Update abgeschlossen.</p>");
		}

		// Ship choosen - get the values
		if (shiptypeId > 0)
		{
			ShipBaubar ship = (ShipBaubar) db.createQuery("from ShipBaubar where id=?").setInteger(0, shiptypeId).uniqueResult();

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shiptype\" value=\"" + shiptypeId + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Energiekosten: </td><td><input type=\"text\" name=\"ekosten\" value=\"" + ship.getEKosten() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + ship.getCrew() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Dauer: </td><td><input type=\"text\" name=\"dauer\" value=\"" + ship.getDauer() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderX\">Rasse: </td><td><select size=\"1\" name=\"race\" \">");
			for(Rasse race: Rassen.get())
			{
				echo.append("<option value=\""+ race.getID() +"\" " + (race.getID() == ship.getRace() ? "selected=\"selected\"" : "") + " />"+race.getName()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ben&ouml;tigte Werftslots: </td><td><input type=\"text\" name=\"werftslots\" value=\"" + ship.getWerftSlots() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flagschiff (true/false): </td><td><input type=\"text\" name=\"flagschiff\" value=\"" + ship.isFlagschiff() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Forschung 1: </td><td><select name=\"tr1\" size=\"1\" style=\"width:200px\">\n");
			List<?> researches = db.createQuery("from Forschung").list();
			for( Iterator<?> iter=researches.iterator(); iter.hasNext(); )
			{
				Forschung requirement = (Forschung)iter.next();
				echo.append("<option value=\""+requirement.getID()+"\" "+(requirement.getID() == ship.getRes(1) ? "selected=\"selected\"" : "")+" \">"+requirement.getName()+"</option>\n");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Forschung 2: </td><td><select name=\"tr2\" size=\"1\" style=\"width:200px\">\n");
			researches = db.createQuery("from Forschung").list();
			for( Iterator<?> iter=researches.iterator(); iter.hasNext(); )
			{
				Forschung requirement = (Forschung)iter.next();
				echo.append("<option value=\""+requirement.getID()+"\" "+(requirement.getID() == ship.getRes(2) ? "selected=\"selected\"" : "")+" \">"+requirement.getName()+"</option>\n");
			}
			echo.append("<tr><td class=\"noBorderS\">Forschung 3: </td><td><select name=\"tr3\" size=\"1\" style=\"width:200px\">\n");
			researches = db.createQuery("from Forschung").list();
			for( Iterator<?> iter=researches.iterator(); iter.hasNext(); )
			{
				Forschung requirement = (Forschung)iter.next();
				echo.append("<option value=\""+requirement.getID()+"\" "+(requirement.getID() == ship.getRes(3) ? "selected=\"selected\"" : "")+" \">"+requirement.getName()+"</option>\n");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				long amount = ship.getCosts().getResourceCount(new ItemID(item.getID()));
				int uses = 0;
				if(!ship.getCosts().getItem(item.getID()).isEmpty())
				{
					uses = ship.getCosts().getItem(item.getID()).get(0).getMaxUses();
				}
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"i"+item.getID()+"\" value=\"" + amount + "\"></td><td><input type=\"text\" name=\"i"+item.getID()+"u\" value=\"" + uses + "\"></td></tr>");
			}
			
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
