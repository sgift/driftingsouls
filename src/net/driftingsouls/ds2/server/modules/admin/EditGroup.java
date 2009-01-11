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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.WarenID;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.hibernate.Query;

/**
 * Ein Menue um mehrere Schiffe mit dem selben Typ zu editieren.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Schiffsgruppe editieren")
public class EditGroup implements AdminPlugin
{
	private static int MAX_SENSORS = 100;
	private static int MAX_COMM = 100;
	private static int MAX_ENGINE = 100;
	private static int MAX_WEAPONS = 100;
	
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		
		int shiptypeId = context.getRequest().getParameterInt("shiptype");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		List<ShipType> shiptypes = Common.cast(db.createQuery("from ShipType").list());

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"shiptype\">");
		for (Iterator<ShipType> iter = shiptypes.iterator(); iter.hasNext();)
		{
			ShipType shiptype = iter.next();

			echo.append("<option value=\"" + shiptype.getId() + "\" " + (shiptype.getId() == shiptypeId ? "selected=\"selected\"" : "") + ">" + shiptype.getNickname() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
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
				ship.setAlarm(context.getRequest().getParameterInt("alarm"));
				
				Cargo cargo = new Cargo();
				
				for(ResourceConfig.Entry resource: ResourceConfig.getResources())
				{
					long amount = context.getRequest().getParameterInt(""+resource.getId());
					cargo.addResource(new WarenID(resource.getId()), amount);
				}
				
				for(Item item: Items.get())
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
			
			Map<Integer, String> alarms = new HashMap<Integer, String>();
			alarms.put(0, "Gelb");
			alarms.put(1, "Rot");
			
			//Name -> Klasse
			Map<Integer, String> groupOptions = new HashMap<Integer, String>();
			groupOptions.put(0, "Im Sektor");
			groupOptions.put(1, "Im System");
			groupOptions.put(2, "Ueberall");
			
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shiptype\" value=\"" + shiptypeId + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Wo: </td><td><select size=\"1\" name=\"groupoption\" \">");
			for(Map.Entry<Integer, String> groupOption: groupOptions.entrySet())
			{
				echo.append("<option value=\""+ groupOption.getKey() +"\"/>"+groupOption.getValue()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">System: </td><td><input type=\"text\" name=\"system\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">x: </td><td><input type=\"text\" name=\"x\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">y: </td><td><input type=\"text\" name=\"y\"></td></tr>\n");
			echo.append("<tr></tr>");
			echo.append("<tr><td class=\"noBorderS\">Huelle: </td><td><input type=\"text\" name=\"hull\" value=\"" + type.getHull() + "\"></td><td class=\"noBorderS\">/ "+type.getHull()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ablative Panzerung: </td><td><input type=\"text\" name=\"ablativearmor\" value=\"" + type.getAblativeArmor() + "\"></td><td class=\"noBorderS\">/ "+type.getAblativeArmor()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schilde: </td><td><input type=\"text\" name=\"shields\" value=\"" + type.getShields() + "\"></td><td class=\"noBorderS\">/ "+type.getShields()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + type.getCrew() + "\"></td><td class=\"noBorderS\">/ "+type.getCrew()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Marines: </td><td><input type=\"text\" name=\"marines\" value=\"" + type.getMarines() + "\"></td><td class=\"noBorderS\">/ "+type.getMarines()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Energie: </td><td><input type=\"text\" name=\"energy\" value=\"" + type.getEps() + "\"></td><td class=\"noBorderS\">/ "+type.getEps()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Sensoren: </td><td><input type=\"text\" name=\"sensors\" value=\"" + MAX_SENSORS + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Antrieb: </td><td><input type=\"text\" name=\"engine\" value=\"" + MAX_ENGINE + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Kommunikation: </td><td><input type=\"text\" name=\"comm\" value=\"" + MAX_COMM + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Waffen: </td><td><input type=\"text\" name=\"weapons\" value=\"" + MAX_WEAPONS + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hitze: </td><td><input type=\"text\" name=\"heat\" value=\"0\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Alarm: </td><td><select size=\"1\" name=\"alarm\" \">");
			for(Map.Entry<Integer, String> alarm: alarms.entrySet())
			{
				echo.append("<option value=\""+ alarm.getKey() +"\"/>"+alarm.getValue()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\""+resource.getId()+"\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: Items.get())
			{
				int uses = 0;
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"i"+item.getID()+"\"></td><td><input type=\"text\" name=\"i"+item.getID()+"u\" value=\"" + uses + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
