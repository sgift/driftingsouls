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
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.WarenID;
import net.driftingsouls.ds2.server.config.ResourceConfig;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.units.UnitCargo;

/**
 * Aktualisierungstool fuer die Werte eines Schiffes.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Schiff editieren")
public class EditShip implements AdminPlugin
{
	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();
		List<Item> itemlist = Common.cast(db.createQuery("from Item").list());
		
		int shipid = context.getRequest().getParameterInt("shipid");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"text\" name=\"shipid\" value=\""+ shipid +"\" />\n");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");
		
		if(update && shipid != 0)
		{
			Ship ship = (Ship)db.get(Ship.class, shipid);
			
			ship.setName(context.getRequest().getParameterString("name"));
			User owner = (User)db.get(User.class, context.getRequest().getParameterInt("owner"));
			if(owner != null)
			{
				ship.setOwner(owner);
			}
			ShipType type = (ShipType)db.get(ShipType.class, context.getRequest().getParameterInt("type"));
			if(type != null)
			{
				ship.setBaseType(type);
			}
			ship.setSystem(context.getRequest().getParameterInt("system"));
			ship.setX(context.getRequest().getParameterInt("x"));
			ship.setY(context.getRequest().getParameterInt("y"));
			ship.setHull(context.getRequest().getParameterInt("hull"));
			ship.setAblativeArmor(context.getRequest().getParameterInt("ablativearmor"));
			ship.setShields(context.getRequest().getParameterInt("shields"));
			ship.setCrew(context.getRequest().getParameterInt("crew"));
			ship.setNahrungCargo(context.getRequest().getParameterInt("nahrungcargo"));
			ship.setEnergy(context.getRequest().getParameterInt("energy"));
			ship.setSensors(context.getRequest().getParameterInt("sensors"));
			ship.setEngine(context.getRequest().getParameterInt("engine"));
			ship.setComm(context.getRequest().getParameterInt("comm"));
			ship.setUnits( new UnitCargo(context.getRequest().getParameterString("unitcargo")));
			ship.setWeapons(context.getRequest().getParameterInt("weapons"));
			ship.setHeat(context.getRequest().getParameterInt("heat"));
			ship.setAlarm(context.getRequest().getParameterInt("alarm"));
			ship.setStatus(context.getRequest().getParameter("status"));
			
			Cargo cargo = new Cargo();
			
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = context.getRequest().getParameterInt(""+resource.getId());
				cargo.addResource(new WarenID(resource.getId()), amount);
			}
			
			for(Item item: itemlist)
			{
				long amount = context.getRequest().getParameterInt("i"+item.getID());
				int uses = context.getRequest().getParameterInt("i" + item.getID() + "uses");
				cargo.addResource(new ItemID(item.getID(), uses, 0), amount);
			}
			
			ship.setCargo(cargo);
			
			echo.append("<p>Update abgeschlossen.</p>");
		}
		
		if(shipid != 0)
		{
			Ship ship = (Ship)db.get(Ship.class, shipid);
			
			if(ship == null)
			{
				return;
			}
			
			ShipTypeData type = ship.getTypeData();
			
			Map<Integer, String> alarms = new HashMap<Integer, String>();
			alarms.put(0, "Gelb");
			alarms.put(1, "Rot");
			
			
			Map<Integer, String> shiptypes = new HashMap<Integer, String>();
			List<ShipType> types = Common.cast(db.createQuery("from ShipType").list());
			for(ShipType shiptype: types)
			{
				shiptypes.put(shiptype.getId(), shiptype.getNickname());
			}
			
			
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shipid\" value=\"" + shipid + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"name\" value=\"" + ship.getName() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Besitzer: </td><td><input type=\"text\" name=\"owner\" value=\"" + ship.getOwner().getId() + "\"></td><td class=\"noBorderS\">"+ Common._title(ship.getOwner().getNickname()) +"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">System: </td><td><input type=\"text\" name=\"system\" value=\"" + ship.getSystem() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">x: </td><td><input type=\"text\" name=\"x\" value=\"" + ship.getX() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">y: </td><td><input type=\"text\" name=\"y\" value=\"" + ship.getY() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schiffstyp: </td><td><select size=\"1\" name=\"type\" \">");
			for(Map.Entry<Integer, String> shiptype: shiptypes.entrySet())
			{
				echo.append("<option value=\""+ shiptype.getKey() +"\" " + (shiptype.getKey().equals(ship.getBaseType().getId()) ? "selected=\"selected\"" : "") + " />"+shiptype.getValue()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">H&uuml;lle: </td><td><input type=\"text\" name=\"hull\" value=\"" + ship.getHull() + "\"></td><td class=\"noBorderS\">/ "+type.getHull()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ablative Panzerung: </td><td><input type=\"text\" name=\"ablativearmor\" value=\"" + ship.getAblativeArmor() + "\"></td><td class=\"noBorderS\">/ "+type.getAblativeArmor()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schilde: </td><td><input type=\"text\" name=\"shields\" value=\"" + ship.getShields() + "\"></td><td class=\"noBorderS\">/ "+type.getShields()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + ship.getCrew() + "\"></td><td class=\"noBorderS\">/ "+type.getCrew()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Nahrungsspeicher: </td><td><input type=\"text\" name=\"nahrungcargo\" value=\"" + ship.getNahrungCargo() + "\"></td><td class=\"noBorderS\">/ "+type.getNahrungCargo()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Einheitenladeraum: </td><td><input type=\"text\" name=\"unitcargo\" value=\"" + ship.getUnits().toString() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Energie: </td><td><input type=\"text\" name=\"energy\" value=\"" + ship.getEnergy() + "\"></td><td class=\"noBorderS\">/ "+type.getEps()+"</td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Sensoren: </td><td><input type=\"text\" name=\"sensors\" value=\"" + ship.getSensors() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Antrieb: </td><td><input type=\"text\" name=\"engine\" value=\"" + ship.getEngine() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Kommunikation: </td><td><input type=\"text\" name=\"comm\" value=\"" + ship.getComm() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Waffen: </td><td><input type=\"text\" name=\"weapons\" value=\"" + ship.getWeapons() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hitze: </td><td><input type=\"text\" name=\"heat\" value=\"" + ship.getHeat() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Alarm: </td><td><select size=\"1\" name=\"alarm\" \">");
			for(Map.Entry<Integer, String> alarm: alarms.entrySet())
			{
				echo.append("<option value=\""+ alarm.getKey() +"\" " + (alarm.getKey().equals(ship.getAlarm()) ? "selected=\"selected\"" : "") + " />"+alarm.getValue()+"</option>");
			}
			echo.append("</select></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flags: </td><td><input type=\"text\" name=\"status\" value=\"" + ship.getStatus() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td></tr>");
			for(ResourceConfig.Entry resource: ResourceConfig.getResources())
			{
				long amount = ship.getCargo().getResourceCount(new WarenID(resource.getId()));
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+resource.getImage()+"\" alt=\"\" />"+resource.getName()+": </td><td><input type=\"text\" name=\""+resource.getId()+"\" value=\"" + amount + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td class=\"noBorderS\">Menge</td><td class=\"noBorderS\">Nutzungen</td></tr>");
			for(Item item: itemlist)
			{
				long amount = ship.getCargo().getResourceCount(new ItemID(item.getID()));
				int uses = 0;
				if(!ship.getCargo().getItem(item.getID()).isEmpty())
				{
					uses = ship.getCargo().getItem(item.getID()).get(0).getMaxUses();
				}
				echo.append("<tr><td class=\"noBorderS\"><img src=\""+item.getPicture()+"\" alt=\"\" />"+item.getName()+": </td><td><input type=\"text\" name=\"i"+item.getID()+"\" value=\"" + amount + "\"></td><td><input type=\"text\" name=\"i"+item.getID()+"u\" value=\"" + uses + "\"></td></tr>");
			}
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
