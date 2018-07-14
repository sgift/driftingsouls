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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import net.driftingsouls.ds2.server.units.UnitCargo;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Map;

/**
 * Repraesentiert ein Schiff in einer Schlacht.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="battles_ships")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class BattleShip {
	@Id
	@ForeignKey(name="battles_ships_fk_ships")
	private int shipid;

	@PrimaryKeyJoinColumn
	@OneToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="shipid", nullable = false)
	@ForeignKey(name="battles_ships_fk_ships")
	private Ship ship;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="battleid", nullable=false)
	@ForeignKey(name="battles_ships_fk_battles")
	private Battle battle;
	private int side;
	private int hull;
	private int shields;
	private int engine;
	private int weapons;
	private int comm;
	private int sensors;
	private int action;
	private int destroyer;
	private int ablativeArmor;
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 */
	public BattleShip() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Schlachteintrag fuer ein Schiff.
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
		this.ablativeArmor = ship.getAblativeArmor();
		this.destroyer = 0;
	}

	/**
	 * Gibt die ID des Schiffes zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return shipid;
	}
	
	/**
	 * Gibt das Schiff zurueck.
	 * @return Das Schiff
	 */
	public Ship getShip() {
		return ship;
	}

	/**
	 * Gibt dieSchlacht zurueck.
	 * @return Die Schlacht
	 */
	public Battle getBattle() {
		return battle;
	}

	/**
	 * Setzt die Schlacht.
	 * @param battle Die neue Schlacht
	 */
	public void setBattle(Battle battle) {
		this.battle = battle;
	}

	/**
	 * Gibt die ID der Seite zurueck, auf der das Schiff kaempft.
	 * @return Die ID der Seite
	 */
	public int getSide() {
		return side;
	}

	/**
	 * Setzt die ID der Seite, auf der das Schiff kaempft.
	 * @param side Die ID der Seite
	 */
	public void setSide(int side) {
		this.side = side;
	}

	/**
	 * Gibt die Anzahl an Huellenpunkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Huellenpunkte am Ende der Runde
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Setzt die Anzahl der Huellenpunkte, welche das Schiff am Ende der Runde hat.
	 * @param hull Die Huellenpunkte am Ende der Runde
	 */
	public void setHull(int hull) {
		this.hull = hull;
	}

	/**
	 * Gibt die Anzahl an Schildpunkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Schildpunkte am Ende der Runde
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Setzt die Anzahl der Schildpunkte, welche das Schiff am Ende der Runde hat.
	 * @param shields Die Schildpunkte am Ende der Runde
	 */
	public void setShields(int shields) {
		this.shields = shields;
	}

	/**
	 * Gibt die Anzahl an Antriebspunkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Antriebspunkte am Ende der Runde
	 */
	public int getEngine() {
		return engine;
	}

	/**
	 * Setzt die Anzahl der Antriebspunkte, welche das Schiff am Ende der Runde hat.
	 * @param engine Die Antriebspunkte am Ende der Runde
	 */
	public void setEngine(int engine) {
		this.engine = engine;
	}
	
	/**
	 * Gibt die Anzahl an Waffenpunkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Waffenpunkte am Ende der Runde
	 */
	public int getWeapons() {
		return weapons;
	}

	/**
	 * Setzt die Anzahl der Waffenpunkte, welche das Schiff am Ende der Runde hat.
	 * @param weapons Die Waffenpunkte am Ende der Runde
	 */
	public void setWeapons(int weapons) {
		this.weapons = weapons;
	}

	/**
	 * Gibt die Anzahl an Komm.Sys.punkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Komm.Sys.punkte am Ende der Runde
	 */
	public int getComm() {
		return comm;
	}

	/**
	 * Setzt die Anzahl der Komm.Sys.punkte, welche das Schiff am Ende der Runde hat.
	 * @param comm Die Komm.Sys.punkte am Ende der Runde
	 */
	public void setComm(int comm) {
		this.comm = comm;
	}

	/**
	 * Gibt die Anzahl an Sensorenpunkten, welche das Schiff am Ende der Runde hat, zurueck.
	 * @return Die Sensorenpunkte am Ende der Runde
	 */
	public int getSensors() {
		return sensors;
	}

	/**
	 * Setzt die Anzahl der Sensorenpunkte, welche das Schiff am Ende der Runde hat.
	 * @param sensors Die Sensorenpunkte am Ende der Runde
	 */
	public void setSensors(int sensors) {
		this.sensors = sensors;
	}

	/**
	 * Prueft, ob das angegebene Flag fuer das Schiff gesetzt ist.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean hasFlag(@Nonnull BattleShipFlag flag)
	{
		return (this.action & flag.getBit()) != 0;
	}

	/**
	 * Fuegt das angegebene Flag zum Schiff hinzu.
	 * @param flag das Flag
	 */
	public void addFlag(@Nonnull BattleShipFlag flag)
	{
		this.action |= flag.getBit();
	}

	/**
	 * Entfernt das angegebene Flag vom Schiff.
	 * @param flag das Flag
	 */
	public void removeFlag(@Nonnull BattleShipFlag flag)
	{
		if( hasFlag(flag) )
		{
			this.action ^= flag.getBit();
		}
	}

	/**
	 * Entfernt alle Flags vom Schiff.
	 */
	public void removeAllFlags()
	{
		this.action = 0;
	}

	/**
	 * Gibt die ID des Spielers zurueck, der dieses Schiff zerstoert hat.
	 * @return Die ID des Spielers
	 */
	public int getDestroyer() {
		return destroyer;
	}

	/**
	 * Setzt setzt die ID des Spielers der dieses Schiff zerstoert hat.
	 * @param id Die ID des Spielers
	 */
	public void setDestroyer(int id) {
		this.destroyer = id;
	}

	/**
	 * Gibt den Cargo des Schiffes zurueck.
	 * @return Der Cargo
	 * @see net.driftingsouls.ds2.server.ships.Ship#getCargo()
	 */
	public Cargo getCargo() {
		return ship.getCargo();
	}

	/**
	 * Gibt die Crewanzahl auf dem Schiff zurueck.
	 * @return Die Crewanzahl
	 * @see net.driftingsouls.ds2.server.ships.Ship#getCrew()
	 */
	public int getCrew() {
		return ship.getCrew();
	}
	
	/**
	 * Gibt die Einheiten auf dem Schiff zurueck.
	 * @return Die Einheiten
	 * @see net.driftingsouls.ds2.server.ships.Ship#getUnits()
	 */
	public UnitCargo getUnits() {
		return ship.getUnits();
	}
	
	/**
	 * Setzt die Einheiten auf dem Schiff.
	 * @param unitcargo Die neuen Einheiten
     */
	public void setUnits(UnitCargo unitcargo) {
		ship.setUnits(unitcargo);
	}

	/**
	 * Gibt den Namen des Schiffes zurueck.
	 * @return Der Name
	 * @see net.driftingsouls.ds2.server.ships.Ship#getName()
	 */
	public String getName() {
		return ship.getName();
	}

	/**
	 * Gibt den Besitzer des Schiffes zurueck.
	 * @return Der Besitzer
	 * @see net.driftingsouls.ds2.server.ships.Ship#getOwner()
	 */
	public User getOwner() {
		return ship.getOwner();
	}

	/**
	 * Gibt die Typen-Daten des Schiffs zurueck .
	 * @return die Typen-Daten
	 * @see net.driftingsouls.ds2.server.ships.Ship#getTypeData()
	 */
	public ShipTypeData getTypeData() {
		return ship.getTypeData();
	}

	/**
	 * Gibt die Waffenhitze zurueck.
	 * @return heat Die Waffenhitze
	 * @see net.driftingsouls.ds2.server.ships.Ship#getWeaponHeat()
	 */
	public Map<String,Integer> getWeaponHeat() {
		return ship.getWeaponHeat();
	}

	/**
	 * Gibt die Punkte an ablativer Panzerung zurueck, ueber die das Schiff noch verfuegt.
	 * @return Die ablative Panzerung
	 */
	public int getAblativeArmor() {
		return ablativeArmor;
	}

	/**
	 * Setzt die ablative Panzerung des Schiffes.
	 * @param ablativeArmour Der neue Wert der ablativen Panzerung
	 */
	public void setAblativeArmor(int ablativeArmour) {
		this.ablativeArmor = ablativeArmour;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
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
		
		int sizeModifier = new ConfigService().getValue(WellKnownConfigValue.BATTLE_VALUE_SIZE_MODIFIER);
		int dockModifier = new ConfigService().getValue(WellKnownConfigValue.BATTLE_VALUE_DOCK_MODIFIER);
		
		return getTypeData().getSize() * sizeModifier + getTypeData().getJDocks() * dockModifier;
	}

    /**
     * @return Der Schaden, der einem Schiff diese Runde noch zugefuegt werden kann.
     */
    public int calcPossibleDamage()
    {
        int possibleDamage = Integer.MAX_VALUE;
        if(getTypeData().hasFlag(ShipTypeFlag.ZERSTOERERPANZERUNG))
        {
            possibleDamage = (int)(Math.ceil(getHull() * 0.25) - (getShip().getHull() - getHull()));
        }

        return possibleDamage;
    }
	
	/**
	 * Checks if the ship is joining the battle.
	 * 
	 * @return true, if the ship is joining, false otherwise.
	 */
	public boolean isJoining()
	{
		return hasFlag(BattleShipFlag.JOIN);
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
	
	/**
	 * Checks, if the ship is in the second row.
	 * 
	 * @return true, if the ship is in the second row, false otherwise.
	 */
	public boolean isSecondRow()
	{
		return hasFlag(BattleShipFlag.SECONDROW);
	}
	
	/**
	 * @return Offensivwert des Schiffes.
	 */
	public int getOffensiveValue()
	{
		Offizier officer = ship.getOffizier();
		if(officer != null)
		{
			return officer.getOffensiveSkill();
		}
		
		return 1;
	}
	
	/**
	 * @return Defensivwert des Schiffes.
	 */
	public int getDefensiveValue()
	{
		Offizier officer = ship.getOffizier();
		if(officer != null)
		{
			double value = officer.getDefensiveSkill() / (double)getTypeData().getSize();
			return Math.max(1, (int)Math.round(value));
		}
		
		return 1;
	}
	
	/**
	 * @return Aktueller Panzerungswert des Schiffes.
	 */
	public int getArmor()
	{
		ShipTypeData shipType = getTypeData();
		
		return (int)Math.round(shipType.getPanzerung()*ship.getHull()/(double)shipType.getHull());
	}
	
	/**
	 * @return Navigationswert des Schiffes.
	 */
	public int getNavigationalValue()
	{
		double navskill = getTypeData().getSize();
		
		Offizier officer = ship.getOffizier();
		if(officer != null ) 
		{
			navskill = officer.getAbility(Offizier.Ability.NAV);
		} 
		
		navskill *= (ship.getEngine()/100d);
		
		return Math.max(1, (int)Math.round(navskill));
	}
	
	private Session getDB()
	{
		return ContextMap.getContext().getDB();
	}
}
