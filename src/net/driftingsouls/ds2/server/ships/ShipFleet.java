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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.werften.WerftObject;

import org.apache.log4j.Logger;
import org.hibernate.Query;

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
		// EMPTY
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
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		return (User)db.createQuery("select s.owner from Ship as s where s.id>0 and s.fleet=?")
			.setEntity(0, this)
			.setMaxResults(1)
			.uniqueResult();
	}
	
	/**
	 * Sammelt alle Jaeger eines Typs auf und landet sie auf den Schiffen 
	 * der Flotte. Sollen alle Jaeger aufgesammelt werden, so muss als Typ
	 * <code>0</code> angegeben werden.
	 * @param user Der Besitzer der Flotte/Jaeger
	 * @param jaegertypeID Der Typ der Jaeger oder <code>null</code>
	 */
	public void collectFightersByType(User user, int jaegertypeID) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> ships = db.createQuery("from Ship where id>0 and fleet=? and battle is null" )
			.setEntity(0, this)
			.list();
		
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			ShipTypeData shiptype = ship.getTypeData();
			
			if( shiptype.getJDocks() == 0 ) {
				continue;
			}
			int free = shiptype.getJDocks() - (int)ship.getLandedCount();
			if( free == 0 ) {
				continue;
			}
			
			List<Ship>jaegerlist = new ArrayList<Ship>();
			
			Query jaegerListeQuery = db.createQuery("from Ship as s left join fetch s.modules " +
					"where "+(jaegertypeID > 0 ? "s.shiptype=:shiptype and " : "")+"s.owner=:user and s.system=:system and " +
							"s.x=:x and s.y=:y and s.docked='' and " +
							"(locate(:jaegerFlag,s.shiptype.flags)!=0 or locate(:jaegerFlag,s.modules.flags)!=0) and " +
							"s.battle is null " +
					"order by s.fleet,s.shiptype ")
				.setEntity("user", user)
				.setInteger("system", ship.getSystem())
				.setInteger("x", ship.getX())
				.setInteger("y", ship.getY())
				.setString("jaegerFlag", ShipTypes.SF_JAEGER);
			
			if( jaegertypeID > 0 ) {
				jaegerListeQuery.setInteger("shiptype", jaegertypeID);
			}
			List<Ship> jaegerliste = Common.cast(jaegerListeQuery.list());
			
			if( jaegerliste.isEmpty() ) {
				break;
			}
			
			jaegerlist = jaegerliste.subList(0, free > jaegerliste.size() ? jaegerliste.size() : free);
			ship.land(jaegerlist.toArray(new Ship[jaegerlist.size()]));
		}
	}

	/**
	 * Setzt die Alarmstufe der Schiffe in der Flotte.
	 * @param alarm Die Alarmstufe
	 */
	public void setAlarm(int alarm) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> ships = db.createQuery("from Ship where id>0 and fleet=? and battle is null" )
			.setEntity(0, this)
			.list();
		
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
		
			if( (ship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ.ordinal()) || !ship.getTypeData().isMilitary() ) {
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
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		db.createQuery("update Ship set fleet=? where id>0 and fleet=?")
			.setEntity(0, this)
			.setEntity(1, targetFleet)
			.executeUpdate();
		
		// Problem i<0 beruecksichtigen - daher nur loeschen, wenn die Flotte auch wirklich leer ist
		long count = (Long)db.createQuery("select count(*) from Ship where fleet=?")
			.setEntity(0, targetFleet)
			.iterate().next();
		if( count == 0 ) {
			db.delete(targetFleet);
		}
	}

	/**
	 * Startet alle Jaeger der Flotte.
	 *
	 */
	public void startFighters() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> ships = db.createQuery("from Ship where id>0 and fleet=? and battle is null")
			.setEntity(0, this)
			.list();
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			aship.start();
		}
	}

	/**
	 * Sammelt alle Container auf und dockt sie an Schiffe der Flotte.
	 * @param user Der Besitzer der Flotte/Container
	 */
	public void collectContainers(User user) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> ships = db.createQuery("from Ship where id>0 and fleet=? and battle is null" )
			.setEntity(0, this)
			.list();
		
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship ship = (Ship)iter.next();
			ShipTypeData shiptype = ship.getTypeData();
			
			if( shiptype.getADocks() == 0 ) {
				continue;
			}
	
			int free = shiptype.getADocks() - (int)ship.getDockedCount();
			if( free == 0 ) {
				continue;
			}
			List<Ship> containerlist = new ArrayList<Ship>();
			
			List<?> containers = db.createQuery("from Ship as s " +
					"where s.owner=? and s.system=? and s.x=? and s.y=? and s.docked='' and " +
							"s.shiptype.shipClass=? and s.battle is null " +
					"order by s.fleet,s.shiptype ")
				.setEntity(0, user)
				.setInteger(1, ship.getSystem())
				.setInteger(2, ship.getX())
				.setInteger(3, ship.getY())
				.setInteger(4, ShipClasses.CONTAINER.ordinal())
				.list();
			
			if( containers.isEmpty() ) {
				break;
			}
			
			containerlist = Common.cast(containers,Ship.class).subList(0, free > containers.size() ? containers.size() : free);
			ship.dock(containerlist.toArray(new Ship[containerlist.size()]));
		}
	}

	/**
	 * Dockt alle Container auf Schiffen der Flotte ab.
	 *
	 */
	public void undockContainers() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		List<?> ships = db.createQuery("from Ship where id>0 and fleet=? and battle is null")
			.setEntity(0, this)
			.list();
		for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
			Ship aship = (Ship)iter.next();
			aship.undock();
		}
	}
	
	/**
	 * Uebergibt alle Schiffe der Flotte an den angegebenen Spieler. Meldungen
	 * werden dabei nach {@link #MESSAGE} geschrieben.
	 * @param newowner Der neue Besitzer.
	 * @return <code>true</code>, falls mindestens ein Schiff der Flotte uebergeben werden konnte
	 */
	public boolean consign(User newowner) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		int count = 0;
		
		this.consignMode = true;
		try {
			List<?> shiplist = db.createQuery("from Ship where fleet=? and battle is null" )
				.setInteger(0, this.id)
				.list();
			for( Iterator<?> iter=shiplist.iterator(); iter.hasNext(); ) {
				Ship aship = (Ship)iter.next();
				boolean tmp = aship.consign(newowner, false );
			
				String msg = Ship.MESSAGE.getMessage();
				if( msg.length() > 0 ) {
					MESSAGE.get().append(msg+"<br />");	
				}
				if( !tmp ) {
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
			throw new IllegalArgumentException("Das Schiff gehoert nicht zu dieser Flotte");
		}
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		int fleetcount = ((Number)db.createQuery("select count(*) from Ship where fleet=? and id>0")
				.setInteger(0, this.id)
				.iterate().next()).intValue();

		if( fleetcount > 2 || this.consignMode ) {
			ship.setFleet(null);
			MESSAGE.get().append("aus der Flotte ausgetreten");
		} 
		else {
			final Iterator<?> shipIter = db.createQuery("from Ship where fleet=?")
				.setEntity(0, this)
				.iterate();
			while( shipIter.hasNext() ) {
				Ship aship = (Ship)shipIter.next();
				aship.setFleet(null);
			}
			
			db.delete(this);
			MESSAGE.get().append("Flotte aufgel&ouml;&szlig;t");
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
		if( obj == null || !(obj instanceof ShipFleet) ) {
			return false;
		}

		final ShipFleet other = (ShipFleet)obj;
		if( id != other.getId() ) {
			return false;
		}
		return true;
	}
	
	/**
	 * Schickt die Flotte in die Werft zur Demontage.
	 * 
	 * @param shipyard Werft, in der die Schiffe demontiert werden sollen.
	 * @return <code>true</code>, wenn alle Schiffe demontiert wurden.
	 */
	public boolean dismantleFleet(WerftObject shipyard)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Location shipyardLocation = shipyard.getLocation();
		List<Ship> ships = Common.cast(db.createQuery("from Ship where fleet=:fleet and system=:system and x=:x and y=:y")
							 			 .setParameter("fleet", this)
							 			 .setParameter("system", shipyardLocation.getSystem())
							 			 .setParameter("x", shipyardLocation.getX())
							 			 .setParameter("y", shipyardLocation.getY())
							 			 .list());
		log.debug("Ships to dismantle in fleet " + getId() + ": " + ships.size());
		int dismantled = shipyard.dismantleShips(ships);
		log.debug("Ships dismantled in fleet " + getId() + ": " + dismantled);
		if(dismantled == ships.size())
		{
			return true;
		}
		
		return false;
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
}
