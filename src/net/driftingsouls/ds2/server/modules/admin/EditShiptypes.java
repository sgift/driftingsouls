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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Aktualisierungstool fuer die Werte von Schiffstypen.
 * 
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Typen editieren")
public class EditShiptypes implements AdminPlugin
{
	public void output(AdminController controller, String page, int action)
	{
		Context context = ContextMap.getContext();
		StringBuffer echo = context.getResponse().getContent();
		org.hibernate.Session db = context.getDB();

		int shiptypeId = context.getRequest().getParameterInt("shiptype");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		List shiptypes = db.createQuery("from ShipType").list();

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"sess\" value=\"" + context.getSession() + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"shiptype\">");
		for (Iterator iter = shiptypes.iterator(); iter.hasNext();)
		{
			ShipType shiptype = (ShipType) iter.next();

			echo.append("<option value=\"" + shiptype.getId() + "\" " + (shiptype.getId() == shiptypeId ? "checked=\"checked\"" : "") + ">" + shiptype.getNickname() + "</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\"");
		echo.append("</form>");

		if (update && shiptypeId > 0)
		{
			Common.dblog("changeshiptype", "", "", "typeid", Integer.toString(shiptypeId));
			
			Request request = context.getRequest();

			int ru = request.getParameterInt("reactoruran");
			int rd = request.getParameterInt("reactordeut");
			int ra = request.getParameterInt("reactoram");
			int rm = request.getParameterInt("reactormaximum");
			int eps = request.getParameterInt("eps");
			int movecost = request.getParameterInt("flycost");
			int hull = request.getParameterInt("hull");
			int armor = request.getParameterInt("armor");
			int cargo = request.getParameterInt("cargo");
			int heat = request.getParameterInt("heat");
			int crew = request.getParameterInt("crew");
			int shields = request.getParameterInt("shields");
			int ablativeArmor = request.getParameterInt("ablativearmor");
			String nickname = request.getParameterString("nickname");
			String picture = request.getParameterString("picture");
			String weapons = request.getParameterString("weapons");
			String maxHeat = request.getParameterString("maxheat");
			int marines = request.getParameterInt("marines");
			int torpedoDef = request.getParameterInt("torpedodef");
			int size = request.getParameterInt("size");
			int jDocks = request.getParameterInt("fighterdocks");
			int aDocks = request.getParameterInt("hulldocks");
			int sensorRange = request.getParameterInt("sensorrange");
			int hydro = request.getParameterInt("hydro");
			int reCost = request.getParameterInt("recost");
			String description = request.getParameter("description");
			int deutFactor = request.getParameterInt("deutfactor");
			int shipClass = request.getParameterInt("class");
			String flags = request.getParameterString("flags");
			int groupwrap = request.getParameterInt("groupwrap");
			int werft = request.getParameterInt("dockyard");
			int oneWayWerft = request.getParameterInt("onewaydockyard");
			int chance4Loot = request.getParameterInt("chanceforloot");
			String modules = request.getParameter("modules");
			int shipCount = request.getParameterInt("shipcount");
			boolean hide = request.getParameterString("hide").trim().toLowerCase().equals("true");
			boolean srs = request.getParameter("srs").trim().toLowerCase().equals("true");
			int scanCost = request.getParameterInt("scancosts");
			int pickingCost = request.getParameterInt("pickingcosts");

			ShipType shiptype = (ShipType) db.createQuery("from ShipType where id=?").setInteger(0, shiptypeId).uniqueResult();

			// Weight the difference between the old and the new value
			Map<String, Integer> factor = new HashMap<String, Integer>();
			factor.put("eps", (int) (eps / (double) shiptype.getEps()));
			factor.put("hull", (int) (hull / (double) shiptype.getHull()));
			factor.put("crew", (int) (crew / (double) shiptype.getCrew()));
			factor.put("shields", (int) (shields / (double) shiptype.getShields()));
			factor.put("ablativearmor", (int) (ablativeArmor / (double) shiptype.getAblativeArmor()));
			factor.put("marines", (int) (marines / (double) shiptype.getMarines()));

			shiptype.setRu(ru);
			shiptype.setRd(rd);
			shiptype.setRa(ra);
			shiptype.setRm(rm);
			shiptype.setEps(eps);
			shiptype.setCost(movecost);
			shiptype.setHull(hull);
			shiptype.setCargo(cargo);
			shiptype.setCrew(crew);
			shiptype.setShields(shields);
			shiptype.setAblativeArmor(ablativeArmor);
			shiptype.setPanzerung(armor);
			shiptype.setHeat(heat);
			shiptype.setNickname(nickname);
			shiptype.setPicture(picture);
			shiptype.setWeapons(weapons);
			shiptype.setMaxHeat(maxHeat);
			shiptype.setMarines(marines);
			shiptype.setTorpedoDef(torpedoDef);
			shiptype.setSize(size);
			shiptype.setJDocks(jDocks);
			shiptype.setADocks(aDocks);
			shiptype.setSensorRange(sensorRange);
			shiptype.setHydro(hydro);
			shiptype.setReCost(reCost);
			shiptype.setDescrip(description);
			shiptype.setDeutFactor(deutFactor);
			shiptype.setShipClass(shipClass);
			shiptype.setFlags(flags);
			shiptype.setGroupwrap(groupwrap);
			shiptype.setWerft(werft);
			shiptype.setOneWayWerft(oneWayWerft);
			shiptype.setChance4Loot(chance4Loot);
			shiptype.setModules(modules);
			shiptype.setShipCount(shipCount);
			shiptype.setHide(hide);
			shiptype.setSrs(srs);
			shiptype.setScanCost(scanCost);
			shiptype.setPickingCost(pickingCost);

			// Update ships
			int count = 0;

			ScrollableResults ships = db.createQuery("from Ship s left join fetch s.modules where s.shiptype= :type").setEntity("type", shiptype).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
			while (ships.next())
			{
				Ship ship = (Ship) ships.get(0);

				ship.setEnergy(ship.getEnergy() * factor.get("eps"));
				ship.setHull(ship.getHull() * factor.get("hull"));
				ship.setCrew(ship.getCrew() * factor.get("crew"));
				ship.setShields(ship.getShields() * factor.get("shields"));
				ship.setAblativeArmor(ship.getAblativeArmor() * factor.get("ablativearmor"));
				ship.setMarines(ship.getMarines() * factor.get("marines"));

				ship.recalculateModules();

				String id = "l " + ship.getId();
				int fighterDocks = ship.getTypeData().getJDocks();
				if (ship.getLandedCount() > fighterDocks)
				{
					List fighters = db.createQuery("from Ship where docked = ?").setString(0, id).list();
					long toStart = ship.getLandedCount() - fighterDocks;
					int fighterCount = 0;

					for (Iterator iter2 = fighters.iterator(); iter2.hasNext() && fighterCount < toStart;)
					{
						Ship fighter = (Ship) iter2.next();

						fighter.setDocked("");
						fighterCount++;
					}
				}

				//Docked
				id = Integer.toString(ship.getId());
				int outerDocks = ship.getTypeData().getADocks();
				if (ship.getDockedCount() > outerDocks)
				{
					List outerDocked = db.createQuery("from Ship where docked = ?").setString(0, id).list();
					long toStart = ship.getDockedCount() - outerDocks;
					int dockedCount = 0;

					for (Iterator iter2 = outerDocked.iterator(); iter2.hasNext() && dockedCount < toStart;)
					{
						Ship outer = (Ship) iter2.next();
						outer.setDocked("");

						dockedCount++;
					}
				}

				ship.recalculateShipStatus();

				count++;
				if (count % 20 == 0)
				{
					db.flush();
					HibernateFacade.evictAll(db, Ship.class, ShipModules.class, Offizier.class);
				}
			}
			db.flush();
			HibernateFacade.evictAll(db, Ship.class, ShipModules.class, Offizier.class);

			ScrollableResults battleShips = db.createQuery("from BattleShip where ship.shiptype=?").setEntity(0, shiptype).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);

			count = 0;
			while (battleShips.next())
			{
				BattleShip battleShip = (BattleShip) battleShips.get(0);
				battleShip.setShields(battleShip.getShields() * factor.get("shields"));
				battleShip.setHull(battleShip.getHull() * factor.get("hull"));
				battleShip.setAblativeArmor(battleShip.getAblativeArmor() * factor.get("ablativearmor"));
				count++;
				//All unflushed changes are part of the sessioncache, so we need to clean it regularly
				if (count % 20 == 0)
				{
					db.flush();
					HibernateFacade.evictAll(db, BattleShip.class, Ship.class);
				}
			}
			db.flush();
			HibernateFacade.evictAll(db, BattleShip.class, Ship.class);

			echo.append("<p>Update abgeschlossen.</p>");
		}

		// Ship choosen - get the values
		if (shiptypeId > 0)
		{
			ShipType ship = (ShipType) db.createQuery("from ShipType where id=?").setInteger(0, shiptypeId).uniqueResult();

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table class=\"noBorder\" width=\"100%\">");
			echo.append("<input type=\"hidden\" name=\"sess\" value=\"" + context.getSession() + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shiptype\" value=\"" + shiptypeId + "\" />\n");
			echo.append("<tr><td class=\"noBorderS\">Name: </td><td><input type=\"text\" name=\"nickname\" value=\"" + ship.getNickname() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + ship.getPicture() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Uranreaktor: </td><td><input type=\"text\" name=\"reactorruran\" value=\"" + ship.getRu() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Deuteriumreaktor: </td><td><input type=\"text\" name=\"reactordeut\" value=\"" + ship.getRd() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Antimateriereaktor: </td><td><input type=\"text\" name=\"reactoram\" value=\"" + ship.getRa() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Reaktor Maximal: </td><td><input type=\"text\" name=\"reactormaximum\" value=\"" + ship.getRm() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">EPS: </td><td><input type=\"text\" name=\"eps\" value=\"" + ship.getEps() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flugkosten: </td><td><input type=\"text\" name=\"flycost\" value=\"" + ship.getCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hülle: </td><td><input type=\"text\" name=\"hull\" value=\"" + ship.getHull() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Panzerung: </td><td><input type=\"text\" name=\"armor\" value=\"" + ship.getPanzerung() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Cargo: </td><td><input type=\"text\" name=\"cargo\" value=\"" + ship.getCargo() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hitze: </td><td><input type=\"text\" name=\"heat\" value=\"" + ship.getHeat() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + ship.getCrew() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Marines: </td><td><input type=\"text\" name=\"marines\" value=\"" + ship.getMarines() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Waffen: </td><td><textarea cols=\"50\" rows=\"10\" name=\"weapons\">" + ship.getWeapons() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Maximale Hitze: </td><td><textarea cols=\"50\" rows=\"10\" name=\"maxheat\">" + ship.getMaxHeat() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Torpedoabwehr: </td><td><input type=\"text\" name=\"torpedodef\" value=\"" + ship.getTorpedoDef() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schilde: </td><td><input type=\"text\" name=\"shields\" value=\"" + ship.getShields() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Größe: </td><td><input type=\"text\" name=\"size\" value=\"" + ship.getSize() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Jägerdocks: </td><td><input type=\"text\" name=\"fighterdocks\" value=\"" + ship.getJDocks() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Aussendocks: </td><td><input type=\"text\" name=\"hulldocks\" value=\"" + ship.getADocks() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Sensorreichweite: </td><td><input type=\"text\" name=\"sensorrange\" value=\"" + ship.getSensorRange() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Hydros: </td><td><input type=\"text\" name=\"hydro\" value=\"" + ship.getHydro() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">RE Kosten (?): </td><td><input type=\"text\" name=\"recosts\" value=\"" + ship.getReCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Beschreibung: </td><td><textarea cols=\"50\" rows=\"10\" name=\"description\">" + ship.getDescrip() + "</textarea></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Deuteriumsammeln: </td><td><input type=\"text\" name=\"deutfactor\" value=\"" + ship.getDeutFactor() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schiffsklasse: </td><td><input type=\"text\" name=\"class\" value=\"" + ship.getShipClass() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Flags: </td><td><input type=\"text\" name=\"flags\" value=\"" + ship.getFlags() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Groupwrap: </td><td><input type=\"text\" name=\"groupwrap\" value=\"" + ship.getGroupwrap() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Werft: </td><td><input type=\"text\" name=\"dockyard\" value=\"" + ship.getWerft() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Einmalwerft: </td><td><input type=\"text\" name=\"onewaydockyard\" value=\"" + ship.getOneWayWerft() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Loot-Chance: </td><td><input type=\"text\" name=\"chanceforloot\" value=\"" + ship.getChance4Loot() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Module: </td><td><input type=\"text\" name=\"modules\" value=\"" + ship.getModules() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Schiffszähler: </td><td><input type=\"text\" name=\"shipcount\" value=\"" + ship.getShipCount() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Verstecken: </td><td><input type=\"text\" name=\"hide\" value=\"" + ship.isHide() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Ablative Panzerung: </td><td><input type=\"text\" name=\"ablativearmor\" value=\"" + ship.getAblativeArmor() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Besitzt SRS: </td><td><input type=\"text\" name=\"srs\" value=\"" + ship.hasSrs() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Scankosten: </td><td><input type=\"text\" name=\"scancosts\" value=\"" + ship.getScanCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\">Picking-Kosten: </td><td><input type=\"text\" name=\"pickingcosts\" value=\"" + ship.getPickingCost() + "\"></td></tr>\n");
			echo.append("<tr><td class=\"noBorderS\"></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</form>\n");
		}
	}
}
