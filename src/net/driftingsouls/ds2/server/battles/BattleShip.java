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
package net.driftingsouls.ds2.server.battles;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Repraesentiert ein Schiff in einer Schlacht
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="battles_ships")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class BattleShip {
	@Id
	private int shipid;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Ship ship;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="battleid", nullable=false)
	private Battle battle;
	private int side;
	private int hull;
	private int shields;
	private int engine;
	private int weapons;
	private int comm;
	private int sensors;
	private int action;
	private int count;
	private int newcount;
	private int ablativeArmor;
	@Version
	private int version;
	
	/**
	 * Konstruktor
	 */
	public BattleShip() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Schlachteintrag fuer ein Schiff
	 * @param battle Die Schlacht
	 * @param ship Das Schiff
	 */
	public BattleShip(Battle battle, Ship ship) {
		this.battle = battle;
		this.ship = ship;
		this.shipid = ship.getId();
		this.hull = ship.getHull();
		this.shields = ship.getShields();
		this.engine = ship.getEngine();
		this.weapons = ship.getWeapons();
		this.comm = ship.getComm();
		this.sensors = ship.getSensors();
		this.count = ship.getTypeData().getShipCount();
		this.ablativeArmor = ship.getAblativeArmor();
		this.newcount = 0;
	}

	/**
	 * Gibt die ID des Schiffes zurueck
	 * @return Die ID
	 */
	public int getId() {
		return shipid;
	}
	
	/**
	 * Gibt das Schiff zurueck
	 * @return Das Schiff
	 */
	public Ship getShip() {
		return ship;
	}

	/**
	 * Gibt dieSchlacht zurueck
	 * @return Die Schlacht
	 */
	public Battle getBattle() {
		return battle;
	}

	/**
	 * Setzt die Schlacht
	 * @param battle Die neue Schlacht
	 */
	public void setBattle(Battle battle) {
		this.battle = battle;
	}

	/**
	 * Gibt die ID der Seite zurueck, auf der das Schiff kaempft
	 * @return Die ID der Seite
	 */
	public int getSide() {
		return side;
	}

	/**
	 * Setzt die ID der Seite, auf der das Schiff kaempft
	 * @param side Die ID der Seite
	 */
	public void setSide(int side) {
		this.side = side;
	}

	/**
	 * Gibt die Anzahl an Huellenpunkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Huellenpunkte am Ende der Runde
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Setzt die Anzahl der Huellenpunkte, welche das Schiff am Ende der Runde hat
	 * @param hull Die Huellenpunkte am Ende der Runde
	 */
	public void setHull(int hull) {
		this.hull = hull;
	}

	/**
	 * Gibt die Anzahl an Schildpunkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Schildpunkte am Ende der Runde
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Setzt die Anzahl der Schildpunkte, welche das Schiff am Ende der Runde hat
	 * @param shields Die Schildpunkte am Ende der Runde
	 */
	public void setShields(int shields) {
		this.shields = shields;
	}

	/**
	 * Gibt die Anzahl an Antriebspunkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Antriebspunkte am Ende der Runde
	 */
	public int getEngine() {
		return engine;
	}

	/**
	 * Setzt die Anzahl der Antriebspunkte, welche das Schiff am Ende der Runde hat
	 * @param engine Die Antriebspunkte am Ende der Runde
	 */
	public void setEngine(int engine) {
		this.engine = engine;
	}
	
	/**
	 * Gibt die Anzahl an Waffenpunkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Waffenpunkte am Ende der Runde
	 */
	public int getWeapons() {
		return weapons;
	}

	/**
	 * Setzt die Anzahl der Waffenpunkte, welche das Schiff am Ende der Runde hat
	 * @param weapons Die Waffenpunkte am Ende der Runde
	 */
	public void setWeapons(int weapons) {
		this.weapons = weapons;
	}

	/**
	 * Gibt die Anzahl an Komm.Sys.punkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Komm.Sys.punkte am Ende der Runde
	 */
	public int getComm() {
		return comm;
	}

	/**
	 * Setzt die Anzahl der Komm.Sys.punkte, welche das Schiff am Ende der Runde hat
	 * @param comm Die Komm.Sys.punkte am Ende der Runde
	 */
	public void setComm(int comm) {
		this.comm = comm;
	}

	/**
	 * Gibt die Anzahl an Sensorenpunkten, welche das Schiff am Ende der Runde hat, zurueck
	 * @return Die Sensorenpunkte am Ende der Runde
	 */
	public int getSensors() {
		return sensors;
	}

	/**
	 * Setzt die Anzahl der Sensorenpunkte, welche das Schiff am Ende der Runde hat
	 * @param sensors Die Sensorenpunkte am Ende der Runde
	 */
	public void setSensors(int sensors) {
		this.sensors = sensors;
	}

	/**
	 * Gibt die Aktionsflags des Schiffes zurueck
	 * @return Die Aktionsflags
	 */
	public int getAction() {
		return action;
	}

	/**
	 * Setzt die Aktionsflags des Schiffes
	 * @param action Die Aktionsflags
	 */
	public void setAction(int action) {
		this.action = action;
	}

	/**
	 * Gibt die Anzahl an Schiffen zurueck, welche in diesem Schiff enthalten sind ("Staffeln")
	 * @return Die Anzahl an Schiffen
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Setzt die Anzahl an Schiffen, welche in diesem Schiff enthalten sind ("Staffeln")
	 * @param count Die Anzahl an Schiffen
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gibt die Anzahl an Schiffen zurueck, welche am Ende der Runde in diesem Schiff enthalten sein werden ("Staffeln")
	 * @return Die Anzahl an Schiffen
	 */
	public int getNewCount() {
		return newcount;
	}

	/**
	 * Setzt die Anzahl an Schiffen, welche am Ende der Runde in diesem Schiff enthalten sein werden ("Staffeln")
	 * @param newcount Die Anzahl an Schiffen
	 */
	public void setNewCount(int newcount) {
		this.newcount = newcount;
	}

	/**
	 * Gibt den Cargo des Schiffes zurueck
	 * @return Der Cargo
	 * @see net.driftingsouls.ds2.server.ships.Ship#getCargo()
	 */
	public Cargo getCargo() {
		return ship.getCargo();
	}

	/**
	 * Gibt die Crewanzahl auf dem Schiff zurueck
	 * @return Die Crewanzahl
	 * @see net.driftingsouls.ds2.server.ships.Ship#getCrew()
	 */
	public int getCrew() {
		return ship.getCrew();
	}
	
	/**
	 * Gibt die Marineanzahl auf dem Schiff zurueck
	 * @return Die Marineanzahl
	 * @see net.driftingsouls.ds2.server.ships.Ship#getMarines()
	 */
	public int getMarines() {
		return ship.getMarines();
	}

	/**
	 * Gibt den Namen des Schiffes zurueck
	 * @return Der Name
	 * @see net.driftingsouls.ds2.server.ships.Ship#getName()
	 */
	public String getName() {
		return ship.getName();
	}

	/**
	 * Gibt den Besitzer des Schiffes zurueck
	 * @return Der Besitzer
	 * @see net.driftingsouls.ds2.server.ships.Ship#getOwner()
	 */
	public User getOwner() {
		return ship.getOwner();
	}

	/**
	 * Gibt die Typen-Daten des Schiffs zurueck 
	 * @return die Typen-Daten
	 * @see net.driftingsouls.ds2.server.ships.Ship#getTypeData()
	 */
	public ShipTypeData getTypeData() {
		return ship.getTypeData();
	}

	/**
	 * Gibt die Waffenhitze zurueck
	 * @return heat Die Waffenhitze
	 * @see net.driftingsouls.ds2.server.ships.Ship#getWeaponHeat()
	 */
	public String getWeaponHeat() {
		return ship.getWeaponHeat();
	}

	/**
	 * Gibt die Dochdaten des Schiffes zurueck
	 * @return Die Dockdaten
	 * @see net.driftingsouls.ds2.server.ships.Ship#getDocked()
	 */
	public String getDocked() {
		return ship.getDocked();
	}

	/**
	 * Gibt die Punkte an ablativer Panzerung zurueck, ueber die das Schiff noch verfuegt
	 * @return Die ablative Panzerung
	 */
	public int getAblativeArmor() {
		return ablativeArmor;
	}

	/**
	 * Setzt die ablative Panzerung des Schiffes
	 * @param ablativeArmour Der neue Wert der ablativen Panzerung
	 */
	public void setAblativeArmor(int ablativeArmour) {
		this.ablativeArmor = ablativeArmour;
	}

	/**
	 * Gibt die Versionsnummer zurueck
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	@Override
	public String toString() {
		return "{BattleShip: "+this.shipid+" Battle: "+(this.battle != null ? battle.getId() : "null")+"}";
	}
	
	/**
	 * Returns the battle value of a ship.
	 * The battle value is a measurement of the fighting power a single ship has.
	 * 
	 * @return The battle value.
	 */
	public int getBattleValue()
	{
		if(isJoining())
		{
			return 0;
		}
		
		if(isStarved())
		{
			return 0;
		}
		
		if(!isMilitary())
		{
			return 0;
		}
		
		ConfigValue sizeModifier = (ConfigValue)getDB().get(ConfigValue.class, "bvsizemodifier");
		ConfigValue dockModifier = (ConfigValue)getDB().get(ConfigValue.class, "bvdockmodifier");
		
		return getTypeData().getSize() * Integer.valueOf(sizeModifier.getValue()) + getTypeData().getJDocks() * Integer.valueOf(dockModifier.getValue());
	}
	
	/**
	 * Checks if the ship is joining the battle.
	 * 
	 * @return true, if the ship is joining, false otherwise.
	 */
	public boolean isJoining()
	{
		return (getAction() & Battle.BS_JOIN) != 0;
	}
	
	/**
	 * Checks, if the ship is starved.
	 * A ship is starved, if the shiptype demands crew, but the ship is crewless.
	 * 
	 * @return true, if the ship is starved, false otherwise.
	 */
	public boolean isStarved()
	{
		return getCrew() == 0 && getTypeData().getCrew() > 0;
	}
	
	/**
	 * Determines, if the ship is a military ship.
	 * 
	 * @return true, if it is a military ship, false otherwise.
	 */
	public boolean isMilitary()
	{
		return getTypeData().isMilitary();
	}
	
	private Session getDB()
	{
		return ContextMap.getContext().getDB();
	}
}