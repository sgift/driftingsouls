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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.entities.GlobalSectorTemplate;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Die Sektor-Template-Verwaltung.</h1>
 * Sektor-Templates sind Vorlagen, die 1..* Schiffe mit bestimmten Werten enthalten.
 * Die Vorlagen liegen in System 0.<br>
 * Der SectorTemplateManager fuegt diese Templates an einer Position fuer einen bestimmten
 * Spieler ein.
 * @author Christopher Jung
 *
 */
public class SectorTemplateManager {
	private static SectorTemplateManager instance = null;

	private SectorTemplateManager() {
		// EMPTY
	}

	/**
	 * Gibt eine Instanz des SektorTemplateManagers zurueck.
	 * @return eine Instanz des SektorTemplateManagers
	 */
	public static SectorTemplateManager getInstance() {
		if( instance == null ) {
			instance = new SectorTemplateManager();
		}
		return instance;
	}

	private static class DockEntry {
		final String docked;
		final int shipid;

		DockEntry(int shipid, String docked) {
			this.shipid = shipid;
			this.docked = docked;
		}
	}

	private static class FleetEntry {
		final int fleetid;
		final int shipid;

		FleetEntry(int shipid, int fleetid) {
			this.shipid = shipid;
			this.fleetid = fleetid;
		}
	}

	/**
	 * Fuegt Schiffe eines Templates an einer gegebenen Position ein.
	 * @param em EntityManager
	 * @param name Der Name des Templates
	 * @param location Die Position, an der das Template eingefuegt werden soll
	 * @param owner Der Besitzer der einzufuegenden Schiffe
	 * @param smartid Soll die erste freie ID verwendet werden (<code>true</code>)?
	 * @return Die IDs der eingefuegten Schiffe
	 */
	public Integer[] useTemplate(EntityManager em, ShipService shipService, String name, Location location, int owner, boolean smartid ) {
		if( smartid ) {
			System.err.println("FIXME: SectorTemplateManager.useTemplate -> smartid not implemented");
			new Throwable().printStackTrace();
		}
		User user = em.find(User.class, owner);

		GlobalSectorTemplate res = em.find(GlobalSectorTemplate.class, name);
		if( res == null ) {
			System.err.println("ERROR: SectorTemplateManager.useTemplate -> unknown resourceid '"+name+"' used");
			new Throwable().printStackTrace();
			return new Integer[0];
		}

		List<Integer> shipids = new ArrayList<>();

		String query = "FROM Ship WHERE id>0 AND system=0 ";

		if( res.getWidth() == 0 ) {
			query += " AND x="+res.getX()+" ";
		}
		else {
			query += " AND (x BETWEEN "+res.getX()+" AND "+(res.getX()+res.getWidth())+") ";
		}

		if( res.getHeigth() == 0 ) {
			query += " AND y="+res.getY()+" ";
		}
		else {
			query += " AND (y BETWEEN "+res.getY()+" AND "+(res.getY()+res.getHeigth())+") ";
		}

		List<DockEntry> docked = new ArrayList<>();
		Map<Integer,Integer> idtable = new HashMap<>();
		List<FleetEntry> fleet = new ArrayList<>();

		List<Ship> ships = em.createQuery(query, Ship.class).getResultList();
		for(Ship ship : ships ) {
			int newx = location.getX() + ship.getX() - res.getX();
			int newy = location.getY() + ship.getY() - res.getY();

			Ship newship = new Ship(user, ship.getBaseType(), location.getSystem(), newx, newy);

			em.persist(newship);
			em.persist(newship.getHistory());

			ModuleEntry[] modules = ship.getModuleEntries();
			for( ModuleEntry entry : modules)
			{
				shipService.addModule(newship, entry.getSlot(), entry.getModuleType(), entry.getData());
			}
			newship.setName(ship.getName());
			newship.setStatus(ship.getStatus());
			newship.setCrew(ship.getCrew());
			newship.setEnergy(ship.getEnergy());
			newship.setHeat(ship.getHeat());
			newship.setHull(ship.getHull());
			newship.setShields(ship.getShields());
			newship.setWeaponHeat(ship.getWeaponHeat());
			newship.setEngine(ship.getEngine());
			newship.setWeapons(ship.getWeapons());
			newship.setComm(ship.getComm());
			newship.setSensors(ship.getSensors());
			newship.setAlarm(ship.getAlarm());
			newship.setJumpTarget(ship.getJumpTarget());
			newship.getHistory().setHistory(ship.getHistory().getHistory());
			newship.setAblativeArmor(ship.getAblativeArmor());
			newship.setNahrungCargo(ship.getNahrungCargo());

			SchiffEinstellungen ce = ship.getEinstellungen();
			SchiffEinstellungen ne = newship.getEinstellungen();
			ne.setAllyFeeding(ce.isAllyFeeding());
			ne.setAutoDeut(ce.getAutoDeut());
			ne.setBookmark(ce.isBookmark());
			ne.setDestCom(ce.getDestCom());
			ne.setDestSystem(ce.getDestSystem());
			ne.setDestX(ce.getDestX());
			ne.setDestY(ce.getDestY());
			ne.setFeeding(ce.isFeeding());
			ne.setStartFighters(ce.startFighters());
			ne.persistIfNecessary(newship);

			int shipid = newship.getId();

			idtable.put(ship.getId(), shipid);

			if( !ship.getDocked().equals("") ) {
				docked.add( new DockEntry(shipid, ship.getDocked()) );
			}

			if( ship.getFleet() != null ) {
				fleet.add( new FleetEntry(shipid, ship.getFleet().getId()) );
			}

			shipids.add(shipid);
		}

		// Gedockte Schiffe behandeln
		boolean landed;
		for( DockEntry dock : docked ) {
			int masterid;
			if( dock.docked.charAt(0) == 'l' ) {
				String[] split = dock.docked.split(" ");
				masterid = Integer.parseInt(split[1]);
				landed = true;
			}
			else {
				masterid = Integer.parseInt(dock.docked);
				landed = false;
			}

			masterid = idtable.get(masterid);

			String newdock;
			if( landed ) {
				newdock = "l "+masterid;
			}
			else {
				newdock = Integer.toString(masterid);
			}

			em.createQuery("UPDATE Ship SET docked= :docked WHERE id= :id")
				.setParameter("docked", newdock)
				.setParameter("id", dock.shipid)
				.executeUpdate();
		}

		Map<Integer,Integer> fleetlist = new HashMap<>();

		for( FleetEntry flship : fleet ) {
			if( !fleetlist.containsKey(flship.fleetid) ) {
				ShipFleet flotte = em.find(ShipFleet.class, flship.fleetid);

				ShipFleet newfleet = new ShipFleet(flotte.getName());
				em.persist(newfleet);
				fleetlist.put( flship.fleetid, newfleet.getId() );
			}
			em.createQuery("UPDATE Ship SET fleet= :fleet WHERE id=:id")
				.setParameter("fleet", fleetlist.get(flship.fleetid))
				.setParameter("id", flship.shipid)
				.executeUpdate();
		}

		return shipids.toArray(new Integer[0]);
	}

	/**
	 * Fuegt Schiffe eines Templates an einer gegebenen Position ein. Als Schiffs-ID
	 * wird die naechste von der DB vergebene verwendet.
	 * @param em EntityManager
	 * @param name Der Name des Templates
	 * @param location Die Position, an der das Template eingefuegt werden soll
	 * @param owner Der Besitzer der einzufuegenden Schiffe
	 * @return Die IDs der eingefuegten Schiffe
	 */
	public Integer[] useTemplate(EntityManager em, ShipService shipService, String name, Location location, int owner) {
		return useTemplate(em, shipService, name, location, owner, false);
	}
}
