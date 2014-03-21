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

import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.framework.db.batch.EvictableUnitOfWork;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Aktualisierungstool fuer die Werte von Schiffstypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Typen editieren")
public class EditShiptypes extends AbstractEditPlugin<ShipType>
{
	public EditShiptypes()
	{
		super(ShipType.class);
	}

	@Override
	protected void edit(EditorForm form, ShipType ship)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		long shipCount = (Long) db
				.createQuery("select count(*) from Ship s where s.shiptype=:type")
				.setEntity("type", ship)
				.uniqueResult();

		form.label("Anzahl vorhandener Schiffe", shipCount);
		form.field("Name", "nickname", String.class, ship.getNickname());
		form.field("Bild", "picture", String.class, ship.getPicture());
		form.field("Uranreaktor", "reactoruran", Integer.class, ship.getRu());
		form.field("Deuteriumreaktor", "reactordeut", Integer.class, ship.getRd());
		form.field("Antimateriereaktor", "reactoram", Integer.class, ship.getRa());
		form.field("Reaktor Maximal", "reactormaximum", Integer.class, ship.getRm());
		form.field("EPS", "eps", Integer.class, ship.getEps());
		form.field("Flugkosten", "flycost", Integer.class, ship.getCost());
		form.field("Hülle", "hull", Integer.class, ship.getHull());
		form.field("Panzerung", "armor", Integer.class, ship.getPanzerung());
		form.field("Cargo", "cargo", Long.class, ship.getCargo());
		form.field("Nahrungsspeicher", "nahrungcargo", Long.class, ship.getNahrungCargo());
		form.field("Hitze", "heat", Integer.class, ship.getHeat());
		form.field("Crew", "crew", Integer.class, ship.getCrew());
		form.field("Maximale Größe für Einheiten", "maxunitsize", Integer.class, ship.getMaxUnitSize());
		form.field("Laderaum für Einheiten", "unitspace", Integer.class, ship.getUnitSpace());
		form.field("Waffen", "weapons", String.class, ship.getWeapons());
		form.field("Maximale Hitze", "maxheat", String.class, ship.getMaxHeat());
		form.field("Torpedoabwehr", "torpedodef", Integer.class, ship.getTorpedoDef());
		form.field("Schilde", "shields", Integer.class, ship.getShields());
		form.field("Größe", "size", Integer.class, ship.getSize());
		form.field("Jägerdocks", "fighterdocks", Integer.class, ship.getJDocks());
		form.field("Aussendocks", "hulldocks", Integer.class, ship.getADocks());
		form.field("Sensorreichweite", "sensorrange", Integer.class, ship.getSensorRange());
		form.field("Hydros", "hydro", Integer.class, ship.getHydro());
		form.field("RE Kosten", "recosts", Integer.class, ship.getReCost());
		form.textArea("Beschreibung", "description", ship.getDescrip());
		form.field("Deuteriumsammeln", "deutfactor", Integer.class, ship.getDeutFactor());
		Map<Integer,String> shipClasses = new HashMap<>();
		for (ShipClasses sc : ShipClasses.values())
		{
			shipClasses.put(sc.ordinal(), sc.getSingular());
		}
		form.field("Schiffsklasse", "class", Integer.class, ship.getShipClass().ordinal()).withOptions(shipClasses);
		form.field("Flags", "flags", String.class, ship.getFlags());
		form.field("Groupwrap", "groupwrap", Integer.class, ship.getGroupwrap());
		form.field("Werft (Slots)", "dockyard", Integer.class, ship.getWerft());
		form.field("Einmalwerft", "onewaydockyard", ShipType.class, ship.getOneWayWerft()).withNullOption("Deaktiviert");
		form.field("Loot-Chance", "chanceforloot", Integer.class, ship.getChance4Loot());
		form.field("Module", "modules", String.class, ship.getModules());
		form.field("Verstecken", "hide", Boolean.class, ship.isHide());
		form.field("Ablative Panzerung", "ablativearmor", Integer.class, ship.getAblativeArmor());
		form.field("Besitzt SRS", "srs", Boolean.class, ship.hasSrs());
		form.field("Scankosten", "scancosts", Integer.class, ship.getScanCost());
		form.field("Picking-Kosten", "pickingcosts", Integer.class, ship.getPickingCost());
		form.field("Mindest-Crew", "mincrew", Integer.class, ship.getMinCrew());
		form.field("EMP verfliegen", "lostinempchance", Double.class, ship.getLostInEmpChance());
	}

	@Override
	protected void update(StatusWriter echo, final ShipType shiptype) throws IOException
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();
		org.hibernate.Session db = context.getDB();

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
		int nahrungcargo = request.getParameterInt("nahrungcargo");
		int shields = request.getParameterInt("shields");
		int ablativeArmor = request.getParameterInt("ablativearmor");
		String nickname = request.getParameterString("nickname");
		String picture = request.getParameterString("picture");
		String weapons = request.getParameterString("weapons");
		String maxHeat = request.getParameterString("maxheat");
		int maxunitsize = request.getParameterInt("maxunitsize");
		int unitspace = request.getParameterInt("unitspace");
		int torpedoDef = request.getParameterInt("torpedodef");
		int size = request.getParameterInt("size");
		int jDocks = request.getParameterInt("fighterdocks");
		int aDocks = request.getParameterInt("hulldocks");
		int sensorRange = request.getParameterInt("sensorrange");
		int hydro = request.getParameterInt("hydro");
		int reCost = request.getParameterInt("recosts");
		String description = request.getParameter("description");
		int deutFactor = request.getParameterInt("deutfactor");
		int shipClass = request.getParameterInt("class");
		String flags = request.getParameterString("flags");
		int groupwrap = request.getParameterInt("groupwrap");
		int werft = request.getParameterInt("dockyard");
		int oneWayWerft = request.getParameterInt("onewaydockyard");
		int chance4Loot = request.getParameterInt("chanceforloot");
		String modules = request.getParameter("modules");
		boolean hide = "true".equals(request.getParameterString("hide").trim().toLowerCase());
		boolean srs = "true".equals(request.getParameter("srs").trim().toLowerCase());
		int scanCost = request.getParameterInt("scancosts");
		int pickingCost = request.getParameterInt("pickingcosts");
		int minCrew = request.getParameterInt("mincrew");
		double lostInEmpChance = Double.parseDouble(request.getParameter("lostinempchance"));

		final int oldeps = shiptype.getEps();
		final int oldhull = shiptype.getHull();
		final int oldcrew = shiptype.getCrew();
		final int oldshields = shiptype.getShields();
		final int oldablativearmor = shiptype.getAblativeArmor();
		final long oldnahrungcargo = shiptype.getNahrungCargo();

		shiptype.setRu(ru);
		shiptype.setRd(rd);
		shiptype.setRa(ra);
		shiptype.setRm(rm);
		shiptype.setEps(eps);
		shiptype.setCost(movecost);
		shiptype.setHull(hull);
		shiptype.setCargo(cargo);
		shiptype.setCrew(crew);
		shiptype.setNahrungCargo(nahrungcargo);
		shiptype.setShields(shields);
		shiptype.setAblativeArmor(ablativeArmor);
		shiptype.setPanzerung(armor);
		shiptype.setHeat(heat);
		shiptype.setNickname(nickname);
		shiptype.setPicture(picture);
		shiptype.setWeapons(weapons);
		shiptype.setMaxHeat(maxHeat);
		shiptype.setMaxUnitSize(maxunitsize);
		shiptype.setUnitSpace(unitspace);
		shiptype.setTorpedoDef(torpedoDef);
		shiptype.setSize(size);
		shiptype.setJDocks(jDocks);
		shiptype.setADocks(aDocks);
		shiptype.setSensorRange(sensorRange);
		shiptype.setHydro(hydro);
		shiptype.setReCost(reCost);
		shiptype.setDescrip(description);
		shiptype.setDeutFactor(deutFactor);
		shiptype.setShipClass(ShipClasses.values()[shipClass]);
		shiptype.setFlags(flags);
		shiptype.setGroupwrap(groupwrap);
		shiptype.setWerft(werft);
		shiptype.setOneWayWerft(oneWayWerft != 0 ? (ShipType)db.get(ShipType.class, oneWayWerft) : null);
		shiptype.setChance4Loot(chance4Loot);
		shiptype.setModules(modules);
		shiptype.setHide(hide);
		shiptype.setSrs(srs);
		shiptype.setScanCost(scanCost);
		shiptype.setPickingCost(pickingCost);
		shiptype.setMinCrew(minCrew);
		shiptype.setLostInEmpChance(lostInEmpChance);

		db.getTransaction().commit();

		List<Integer> shipIds = Common.cast(db
				.createQuery("select s.id from Ship s where s.shiptype= :type")
				.setEntity("type", shiptype)
				.list());
		new EvictableUnitOfWork<Integer>("EditShiptypes - Ship Update") {
			@Override
			public void doWork(Integer shipId) throws Exception
			{
				Ship ship = (Ship) getDB().get(Ship.class, shipId);
				boolean modules = ship.getModules().length > 0;
				ShipTypeData type = ship.getTypeData();

				// Weight the difference between the old and the new value
				Map<String, Double> factor = new HashMap<>();
				factor.put("eps", ship.getEnergy() / (double) (modules ? type.getEps() : oldeps));
				factor.put("hull", ship.getHull() / (double) (modules ? type.getHull() : oldhull));
				factor.put("crew", ship.getCrew() / (double) (modules ? type.getCrew() : oldcrew));
				factor.put("shields", ship.getShields() / (double) (modules ? type.getShields() : oldshields));
				factor.put("ablativearmor", ship.getAblativeArmor() / (double) (modules ? type.getAblativeArmor() : oldablativearmor));
				factor.put("nahrungcargo", ship.getNahrungCargo() / (double) (modules ? type.getAblativeArmor() : oldnahrungcargo));

				ship.recalculateModules();
				type = ship.getTypeData();

				ship.setEnergy((int)Math.floor(type.getEps() * factor.get("eps")));
				ship.setHull((int)Math.floor(type.getHull() * factor.get("hull")));
				ship.setCrew((int)Math.floor(type.getCrew() * factor.get("crew")));
				ship.setShields((int)Math.floor(type.getShields() * factor.get("shields")));
				ship.setAblativeArmor((int)Math.floor(type.getAblativeArmor() * factor.get("ablativearmor")));
				ship.setNahrungCargo((long)Math.floor(type.getNahrungCargo() * factor.get("nahrungcargo")));

				int fighterDocks = ship.getTypeData().getJDocks();
				if (ship.getLandedCount() > fighterDocks)
				{
					List<Ship> fighters = ship.getLandedShips();
					long toStart = fighters.size() - fighterDocks;
					int fighterCount = 0;

					for (Iterator<Ship> iter2 = fighters.iterator(); iter2.hasNext() && fighterCount < toStart;)
					{
						Ship fighter = iter2.next();

						fighter.setDocked("");
						fighterCount++;
					}
				}

				//Docked
				int outerDocks = ship.getTypeData().getADocks();
				if (ship.getDockedCount() > outerDocks)
				{
					List<Ship> outerDocked = ship.getDockedShips();
					long toStart = outerDocked.size() - outerDocks;
					int dockedCount = 0;

					for (Iterator<?> iter2 = outerDocked.iterator(); iter2.hasNext() && dockedCount < toStart;)
					{
						Ship outer = (Ship) iter2.next();
						outer.setDocked("");

						dockedCount++;
					}
				}

				if(ship.getId() >= 0)
				{
					ship.recalculateShipStatus();
				}
			}
		}.setFlushSize(10).executeFor(shipIds);

		List<Integer> battleShipIds = Common.cast(db.createQuery("select id from BattleShip where ship.shiptype=:type")
				.setEntity("type", shiptype)
				.list());

		new EvictableUnitOfWork<Integer>("EditShiptypes - BattleShip Update") {
			@Override
			public void doWork(Integer battleShipId) throws Exception
			{
				BattleShip battleShip = (BattleShip) getDB().get(BattleShip.class, battleShipId);
	
				ShipTypeData type = battleShip.getShip().getTypeData();
				// Weight the difference between the old and the new value
				Map<String, Double> factor = new HashMap<>();
				if( type.getHull() == shiptype.getHull() ) {
					factor.put("hull", battleShip.getHull() / (double) oldhull);
				}
				else {
					factor.put("hull", battleShip.getHull() / (double) type.getHull());
				}
				if( type.getShields() == shiptype.getShields() ) {
					factor.put("shields", battleShip.getShields() / (double) oldshields);
				}
				else {
					factor.put("shields", battleShip.getShields() / (double) type.getShields());
				}
				if( type.getAblativeArmor() == shiptype.getAblativeArmor() ) {
					factor.put("ablativearmor", battleShip.getAblativeArmor() / (double) oldablativearmor);
				}
				else {
					factor.put("ablativearmor", battleShip.getAblativeArmor() / (double) type.getAblativeArmor());
				}

				battleShip.setShields((int)Math.floor(type.getShields() * factor.get("shields")));
				battleShip.setHull((int)Math.floor(type.getHull() * factor.get("hull")));
				battleShip.setAblativeArmor((int)Math.floor(type.getAblativeArmor() * factor.get("ablativearmor")));
			}
		}.setFlushSize(10).executeFor(battleShipIds);
	}
}
