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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Alarmstufe;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.Query;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ein Menue um mehrere Schiffe mit dem selben Typ zu editieren.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Schiffsgruppe editieren", permission = WellKnownAdminPermission.EDIT_GROUP)
public class EditGroup implements AdminPlugin
{
	private static int MAX_SENSORS = 100;
	private static int MAX_COMM = 100;
	private static int MAX_ENGINE = 100;
	private static int MAX_WEAPONS = 100;
	
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());
		
		int shiptypeId = context.getRequest().getParameterInt("shiptype");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		List<ShipType> shiptypes = Common.cast(db.createQuery("from ShipType").list());

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"shiptype\">");
		for (ShipType shiptype : shiptypes)
		{
			echo.append("<option value=\"").append(shiptype.getId()).append("\" ").append(shiptype.getId() == shiptypeId ? "selected=\"selected\"" : "").append(">").append(shiptype.getNickname()).append("</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");
		
		if(update && shiptypeId != 0)
		{
			//Where to edit the ships?
			int groupOption = context.getRequest().getParameterInt("groupoption");
			int system = context.getRequest().getParameterInt("system");
			int x = context.getRequest().getParameterInt("x");
			int y = context.getRequest().getParameterInt("y");
			
			ShipType type = (ShipType)db.get(ShipType.class, shiptypeId);
			Query query;
			if(groupOption == 0)
			{
				query = db.createQuery("from Ship where shiptype=:shiptype and owner=:owner and system=:system and x=:x and y=:y")
						  .setParameter("shiptype",type)
						  .setParameter("owner", context.getActiveUser())
						  .setParameter("system", system)
						  .setParameter("x", x)
						  .setParameter("y", y);
			}
			else if(groupOption == 1)
			{
				query = db.createQuery("from Ship where shiptype=:shiptype and owner=:owner and system=:system")
				  		  .setParameter("shiptype",type)
						  .setParameter("owner", context.getActiveUser())
				  		  .setParameter("system", system);
			}
			else
			{
				query = db.createQuery("from Ship where shiptype=:shiptype and owner=:owner")
						  .setParameter("shiptype",type)
		  		  	      .setParameter("owner", context.getActiveUser());
			}
			
			//Get ships to edit
			List<Ship> ships = Common.cast(query.list());
			for(Ship ship: ships)
			{				
				ship.setHull(context.getRequest().getParameterInt("hull"));
				ship.setAblativeArmor(context.getRequest().getParameterInt("ablativearmor"));
				ship.setShields(context.getRequest().getParameterInt("shields"));
				ship.setCrew(context.getRequest().getParameterInt("crew"));
				ship.setEnergy(context.getRequest().getParameterInt("energy"));
				ship.setSensors(context.getRequest().getParameterInt("sensors"));
				ship.setEngine(context.getRequest().getParameterInt("engine"));
				ship.setComm(context.getRequest().getParameterInt("comm"));
				ship.setWeapons(context.getRequest().getParameterInt("weapons"));
				ship.setHeat(context.getRequest().getParameterInt("heat"));
				ship.setAlarm(Alarmstufe.values()[context.getRequest().getParameterInt("alarm")]);
				
				Cargo cargo = new Cargo();
				
				for(Item item: itemlist)
				{
					long amount = context.getRequest().getParameterInt("i"+item.getID());
					int uses = context.getRequest().getParameterInt("i" + item.getID() + "uses");
					cargo.addResource(new ItemID(item.getID(), uses, 0), amount);
				}
				
				ship.setCargo(cargo);
			}
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(shiptypeId != 0)
		{
			ShipType type = (ShipType)db.get(ShipType.class, shiptypeId);
			
			if(type == null)
			{
				return;
			}
			
			Map<Integer, String> alarms = new HashMap<>();
			alarms.put(0, "Gr&uuml;n");
			alarms.put(1, "Gelb");
			alarms.put(2, "Rot");
			
			//Name -> Klasse
			Map<Integer, String> groupOptions = new HashMap<>();
			groupOptions.put(0, "Im Sektor");
			groupOptions.put(1, "Im System");
			groupOptions.put(2, "Ueberall");
			
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shiptype\" value=\"").append(shiptypeId).append("\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Wo: </td><td><select size=\"1\" name=\"groupoption\" \">");
			for(Map.Entry<Integer, String> groupOption: groupOptions.entrySet())
			{
				echo.append("<option value=\"").append(groupOption.getKey()).append("\"/>").append(groupOption.getValue()).append("</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">System: </td><td><input type=\"text\" name=\"system\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">x: </td><td><input type=\"text\" name=\"x\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">y: </td><td><input type=\"text\" name=\"y\"></td></tr>\n");
			echo.append("<tr></tr>");
			echo.append("<tr><td class=\"noBorderS\">Huelle: </td><td><input type=\"text\" name=\"hull\" value=\"").append(type.getHull()).append("\"></td><td class=\"noBorderS\">/ ").append(type.getHull()).append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ablative Panzerung: </td><td><input type=\"text\" name=\"ablativearmor\" value=\"").append(type.getAblativeArmor()).append("\"></td><td class=\"noBorderS\">/ ").append(type.getAblativeArmor()).append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schilde: </td><td><input type=\"text\" name=\"shields\" value=\"").append(type.getShields()).append("\"></td><td class=\"noBorderS\">/ ").append(type.getShields()).append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Crew: </td><td><input type=\"text\" name=\"crew\" value=\"").append(type.getCrew()).append("\"></td><td class=\"noBorderS\">/ ").append(type.getCrew()).append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Energie: </td><td><input type=\"text\" name=\"energy\" value=\"").append(type.getEps()).append("\"></td><td class=\"noBorderS\">/ ").append(type.getEps()).append("</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Sensoren: </td><td><input type=\"text\" name=\"sensors\" value=\"").append(MAX_SENSORS).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Antrieb: </td><td><input type=\"text\" name=\"engine\" value=\"").append(MAX_ENGINE).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Kommunikation: </td><td><input type=\"text\" name=\"comm\" value=\"").append(MAX_COMM).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Waffen: </td><td><input type=\"text\" name=\"weapons\" value=\"").append(MAX_WEAPONS).append("\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hitze: </td><td><input type=\"text\" name=\"heat\" value=\"0\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Alarm: </td><td><select size=\"1\" name=\"alarm\" \">");
			for(Map.Entry<Integer, String> alarm: alarms.entrySet())
			{
				echo.append("<option value=\"").append(alarm.getKey()).append("\"/>").append(alarm.getValue()).append("</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				int uses = 0;
				echo.append("<tr><td class=\"noBorderS\"><img src=\"").append(item.getPicture()).append("\" alt=\"\" />").append(item.getName()).append(": </td><td><input type=\"text\" name=\"i").append(item.getID()).append("\"></td><td><input type=\"text\" name=\"i").append(item.getID()).append("u\" value=\"").append(uses).append("\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
