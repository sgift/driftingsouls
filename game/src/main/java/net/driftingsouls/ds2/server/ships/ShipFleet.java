/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.werften.WerftObject;
import org.apache.log4j.Logger;
import org.hibernate.Query;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.EntityManager;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.util.Iterator;
import java.util.List;

/**
 * Eine Flotte aus Schiffen.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_fleets")
public class ShipFleet {
	/**
	 * Objekt mit Funktionsmeldungen.
	 */
	public static final ContextLocalMessage MESSAGE = new ContextLocalMessage();

	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;

	@Version
	private int version;

	@Transient
	private boolean consignMode = false;

	@Transient
	private final Logger log = Logger.getLogger(ShipFleet.class);

	/**
	 * Konstruktor.
	 *
	 */
	public ShipFleet() {
		this.name = "";
	}

	/**
	 * <p>Konstruktor.</p>
	 * Erstellt eine neue Flotte
	 * @param name Der Name der Flotte
	 */
	public ShipFleet(String name) {
		this.name = name;
	}

	/**
	 * Gibt den Namen der Flotte zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen der Flotte.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt die ID der Flotte zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den Besitzer der Flotte zurueck.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		EntityManager em = ContextMap.getContext().getEM();

		return em.createQuery("select s.owner from Ship as s where s.id>0 and s.fleet=:fleet", User.class)
			.setParameter("fleet", this)
			.setMaxResults(1)
			.getSingleResult();
	}

	/**
	 * Sammelt alle Jaeger eines Typs auf und landet sie auf den Schiffen
	 * der Flotte. Sollen alle Jaeger aufgesammelt werden, so muss als Typ
	 * <code>0</code> angegeben werden.
	 * @param user Der Besitzer der Flotte/Jaeger
	 * @param jaegertypeID Der Typ der Jaeger oder <code>null</code>
	 */
	public void collectFightersByType(User user, int jaegertypeID) {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();

		for (Ship ship : ships)
		{
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getJDocks() == 0)
			{
				continue;
			}
			int free = shiptype.getJDocks() - (int) ship.getLandedCount();
			if (free == 0)
			{
				continue;
			}

			List<Ship> jaegerlist;

			javax.persistence.Query jaegerListeQuery = em.createQuery("select s from Ship as s left join s.modules m " +
					"where " + (jaegertypeID > 0 ? "s.shiptype=:shiptype and " : "") + "s.owner=:user and s.system=:system and " +
					"s.x=:x and s.y=:y and s.docked='' and " +
					"(locate(:jaegerFlag,s.shiptype.flags)!=0 or locate(:jaegerFlag,m.flags)!=0) and " +
					"s.battle is null " +
					"order by s.fleet.id,s.shiptype.id ", Ship.class)
					.setParameter("user", user)
					.setParameter("system", ship.getSystem())
					.setParameter("x", ship.getX())
					.setParameter("y", ship.getY())
					.setParameter("jaegerFlag", ShipTypeFlag.JAEGER.getFlag());

			if (jaegertypeID > 0)
			{
				jaegerListeQuery.setParameter("shiptype", jaegertypeID);
			}
			List<Ship> jaegerliste = jaegerListeQuery.getResultList();

			if (jaegerliste.isEmpty())
			{
				break;
			}

			jaegerlist = jaegerliste.subList(0, Math.min(free, jaegerliste.size()));
			ship.land(jaegerlist.toArray(new Ship[0]));
		}
	}

	/**
	 * Setzt die Alarmstufe der Schiffe in der Flotte.
	 * @param alarm Die Alarmstufe
	 */
	public void setAlarm(Alarmstufe alarm) {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();

		for (Ship ship : ships)
		{

			if ((ship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ) || !ship.getTypeData().isMilitary())
			{
				continue;
			}

			ship.setAlarm(alarm);
		}
	}

	/**
	 * Fuegt alle Schiffe der Zielflotte dieser Flotte hinzu.
	 * @param targetFleet Die Zielflotte
	 */
	public void joinFleet(ShipFleet targetFleet) {
		EntityManager em = ContextMap.getContext().getEM();

		em.createQuery("update Ship set fleet=:fleet where id>0 and fleet=:targetFleet")
			.setParameter("fleet", this)
			.setParameter("targetFleet", targetFleet)
			.executeUpdate();

		// Problem i<0 beruecksichtigen - daher nur loeschen, wenn die Flotte auch wirklich leer ist
		long count = em.createQuery("select count(*) from Ship where fleet=:fleet", Long.class)
			.setParameter("fleet", targetFleet)
			.getSingleResult();
		if( count == 0 ) {
			em.remove(targetFleet);
		}
	}

	/**
	 * Startet alle Jaeger der Flotte.
	 *
	 */
	public void startFighters() {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();
		for (Ship ship : ships)
		{
			ship.start();
		}
	}

	/**
	 * Sammelt alle Container auf und dockt sie an Schiffe der Flotte.
	 * @param user Der Besitzer der Flotte/Container
	 */
	public void collectContainers(User user) {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();

		for (Ship ship : ships)
		{
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getADocks() == 0)
			{
				continue;
			}

			int free = shiptype.getADocks() - (int) ship.getDockedCount();
			if (free == 0)
			{
				continue;
			}
			List<Ship> containerlist;

			List<Ship> containers = em.createQuery("from Ship as s " +
					"where s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.docked='' and " +
					"s.shiptype.shipClass=:cls and s.battle is null " +
					"order by s.fleet.id,s.shiptype.id ", Ship.class)
					.setParameter("owner", user)
					.setParameter("sys", ship.getSystem())
					.setParameter("x", ship.getX())
					.setParameter("y", ship.getY())
					.setParameter("cls", ShipClasses.CONTAINER)
					.getResultList();

			if (containers.isEmpty())
			{
				break;
			}

			containerlist = containers.subList(0, Math.min(free, containers.size()));
			ship.dock(containerlist.toArray(new Ship[0]));
		}
	}

	/**
	 * Sammelt alle Geschütze auf und dockt sie an Schiffe der Flotte.
	 * @param user Der Besitzer der Flotte/Geschütze
	 */
	public void collectGeschuetze(User user) {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();

		for (Ship ship : ships)
		{
			ShipTypeData shiptype = ship.getTypeData();

			if (shiptype.getADocks() == 0)
			{
				continue;
			}

			int free = shiptype.getADocks() - (int) ship.getDockedCount();
			if (free == 0)
			{
				continue;
			}
			List<Ship> geschuetzlist;

			List<Ship> geschuetze = em.createQuery("from Ship as s " +
					"where s.owner=:owner and s.system=:sys and s.x=:x and s.y=:y and s.docked='' and " +
					"s.shiptype.shipClass=:cls and s.battle is null " +
					"order by s.fleet.id,s.shiptype.id ", Ship.class)
					.setParameter("owner", user)
					.setParameter("sys", ship.getSystem())
					.setParameter("x", ship.getX())
					.setParameter("y", ship.getY())
					.setParameter("cls", ShipClasses.GESCHUETZ)
					.getResultList();

			if (geschuetze.isEmpty())
			{
				break;
			}

			geschuetzlist = geschuetze.subList(0, Math.min(free, geschuetze.size()));
			ship.dock(geschuetzlist.toArray(new Ship[0]));
		}
	}

	/**
	 * Dockt alle Container auf Schiffen der Flotte ab.
	 *
	 */
	public void undockContainers() {
		EntityManager em = ContextMap.getContext().getEM();

		List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
			.setParameter("fleet", this)
			.getResultList();
		for (Ship ship : ships)
		{
			ship.undock();
		}
	}

	/**
	 * Uebergibt alle Schiffe der Flotte an den angegebenen Spieler. Meldungen
	 * werden dabei nach {@link #MESSAGE} geschrieben.
	 * @param newowner Der neue Besitzer.
	 * @return <code>true</code>, falls mindestens ein Schiff der Flotte uebergeben werden konnte
	 */
	public boolean consign(User newowner) {
		EntityManager em = ContextMap.getContext().getEM();

		int count = 0;

		this.consignMode = true;
		try {
			List<Ship> shiplist = em.createQuery("from Ship where fleet=:fleet and battle is null", Ship.class)
				.setParameter("fleet", this.id)
				.getResultList();
			for (Ship aship : shiplist)
			{
				boolean tmp = aship.consign(newowner, false);

				String msg = Ship.MESSAGE.getMessage();
				if (msg.length() > 0)
				{
					MESSAGE.get().append(msg).append("<br />");
				}
				if (!tmp)
				{
					count++;
					aship.setFleet(this);
				}
			}
		}
		finally {
			this.consignMode = false;
		}

		return count > 0;
	}

	/**
	 * Entfernt ein Schiff aus der Flotte. Sollte die Flotte anschliessend zu wenige Schiffe haben
	 * wird sie aufgeloesst.
	 * @param ship Das Schiff
	 */
	public void removeShip(Ship ship) {
		if( !this.equals(ship.getFleet()) ) {
			throw new IllegalArgumentException("Das Schiff gehört nicht zu dieser Flotte");
		}

		EntityManager em = ContextMap.getContext().getEM();

		Long fleetcount = em.createQuery("select count(*) from Ship where fleet=:fleet and id>0", Long.class)
				.setParameter("fleet", this.id)
				.getSingleResult();

		if( fleetcount > 2 || this.consignMode ) {
			ship.setFleet(null);
			MESSAGE.get().append("Das Schiff hat die Flotte verlassen");
		}
		else {
			List<Ship> ships = em.createQuery("from Ship where fleet=:fleet", Ship.class)
				.setParameter("fleet", this)
				.getResultList();
			for (Ship aship : ships) {
				aship.setFleet(null);
			}

			em.remove(this);
			MESSAGE.get().append("Flotte aufgelöst");
		}
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if( this == obj ) {
			return true;
		}
		if(!(obj instanceof ShipFleet)) {
			return false;
		}

		final ShipFleet other = (ShipFleet)obj;
		return id == other.getId();
	}

	/**
	 * Schickt die Flotte in die Werft zur Demontage.
	 *
	 * @param shipyard Werft, in der die Schiffe demontiert werden sollen.
	 * @return <code>true</code>, wenn alle Schiffe demontiert wurden.
	 */
	public boolean dismantleFleet(WerftObject shipyard)
	{
		EntityManager em = ContextMap.getContext().getEM();
		Location shipyardLocation = shipyard.getLocation();
		List<Ship> ships = em.createQuery("from Ship where fleet=:fleet and system=:system and x=:x and y=:y", Ship.class)
							 			 .setParameter("fleet", this)
							 			 .setParameter("system", shipyardLocation.getSystem())
							 			 .setParameter("x", shipyardLocation.getX())
							 			 .setParameter("y", shipyardLocation.getY())
							 			 .getResultList();
		log.debug("Ships to dismantle in fleet " + getId() + ": " + ships.size());
		int dismantled = shipyard.dismantleShips(ships);
		log.debug("Ships dismantled in fleet " + getId() + ": " + dismantled);
		return dismantled == ships.size();

	}

	/**
	 * Fuegt das angegebene Schiff zur Flotte hinzu.
	 * @param ship Das Schiff das hinzugefuegt werden soll
	 */
	public void addShip(Ship ship)
	{
		ship.setFleet(this);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * Gibt alle Schiffe der Flotte unabhaengig von Position/Besitzer zurueck.
	 * @return Die Schiffe
	 */
	public List<Ship> getShips()
	{
		EntityManager em = ContextMap.getContext().getEM();
		return em.createQuery("from Ship where fleet=:fleet", Ship.class)
	 			.setParameter("fleet", this)
	 			.getResultList();
	}
}
