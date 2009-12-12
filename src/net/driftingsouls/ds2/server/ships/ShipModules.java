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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Die Moduldaten eines Schiffes.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships_modules")
public class ShipModules implements ShipTypeData {
	@Id
	private int id;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Ship ship;
	private String modules;
	private String nickname;
	private String picture;
	private int ru;
	private int rd;
	private int ra;
	private int rm;
	private int eps;
	private int cost;
	private int hull;
	private int panzerung;
	private long cargo;
	private long nahrungcargo;
	private int heat;
	private int crew;
	private String weapons;
	@Column(name="maxheat")
	private String maxHeat;
	@Column(name="torpedodef")
	private int torpedoDef;
	private int shields;
	private int size;
	@Column(name="jdocks")
	private int jDocks;
	@Column(name="adocks")
	private int aDocks;
	@Column(name="sensorrange")
	private int sensorRange;
	private int hydro;
	@Column(name="deutfactor")
	private int deutFactor;
	@Column(name="recost")
	private int reCost;
	private String flags;
	private int werft;
	@Column(name="ow_werft")
	private int oneWayWerft;
	private int ablativeArmor;
	private boolean srs;
	private int scanCost;
	private int pickingCost;
	private int minCrew;
	private double lostInEmpChance;
	private int maxunitsize;
	private int unitspace;
	private boolean versorger;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ShipModules() {
		// EMPTY
	}
	
	/**
	 * <p>Konstruktor.</p>
	 * Erstellt einen neuen Schiffsmoduleintrag fuer das angegebene Schiff
	 * @param ship Das Schiff
	 */
	public ShipModules(Ship ship) {
		this.id = ship.getId();
		this.ship = ship;
		this.flags = "";
		this.weapons = "";
		this.maxHeat = "";
		this.nickname = "";
		this.picture = "";
		this.modules = "";
	}
	
	/**
	 * Gibt das Schiff zurueck, zu dem der Eintrag gehoert.
	 * @return Das Schiff
	 */
	public Ship getShip() {
		return ship;
	}

	@Override
	public int getADocks() {
		return aDocks;
	}

	/**
	 * Setzt die externen Docks.
	 * @param docks Die externen Docks
	 */
	public void setADocks(int docks) {
		aDocks = docks;
	}

	@Override
	public long getCargo() {
		return cargo;
	}

	/**
	 * Setzt den Cargo.
	 * @param cargo Den Cargo
	 */
	public void setCargo(long cargo) {
		this.cargo = cargo;
	}
	
	@Override
	public long getNahrungCargo() {
		return nahrungcargo;
	}
	
	/**
	 * Setzt den NahrungsCargo.
	 * @param nahrungcargo der Nahrungcargo
	 */
	public void setNahrungCargo(long nahrungcargo) {
		this.nahrungcargo = nahrungcargo;
	}

	@Override
	public int getCost() {
		return cost;
	}

	/**
	 * Setzt die Flugkosten.
	 * @param cost Die Flugkosten
	 */
	public void setCost(int cost) {
		this.cost = cost;
	}

	@Override
	public int getCrew() {
		return crew;
	}

	/**
	 * Setzt die Crew.
	 * @param crew Die Crew
	 */
	public void setCrew(int crew) {
		this.crew = crew;
	}
	
	@Override
	public int getMaxUnitSize() {
		return maxunitsize;
	}

	/**
	 * Setzt die maximale Groesze der Einheiten.
	 * @param maxunitsize Die maximale Groesze
	 */
	public void setMaxUnitSize(int maxunitsize) {
		this.maxunitsize = maxunitsize;
	}
	
	@Override
	public int getUnitSpace() {
		return unitspace;
	}
	
	/**
	 * Setzt den Laderaum fuer Einheiten.
	 * @param unitspace Der Laderaum
	 */
	public void setUnitSpace(int unitspace) {
		this.unitspace = unitspace;
	}
	
	@Override
	public int getDeutFactor() {
		return deutFactor;
	}

	/**
	 * Setzt den Deuteriumfaktor.
	 * @param deutFactor Der Deuteriumfaktor
	 */
	public void setDeutFactor(int deutFactor) {
		this.deutFactor = deutFactor;
	}

	@Override
	public int getEps() {
		return eps;
	}

	/**
	 * Setzt die Eps.
	 * @param eps Die Eps
	 */
	public void setEps(int eps) {
		this.eps = eps;
	}

	@Override
	public String getFlags() {
		return flags;
	}

	/**
	 * Setzt die Flags.
	 * @param flags Die Flags
	 */
	public void setFlags(String flags) {
		this.flags = flags;
	}

	@Override
	public int getHeat() {
		return heat;
	}

	/**
	 * Setzt die Antriebsueberhitzung.
	 * @param heat Die Antriebsueberhitzung
	 */
	public void setHeat(int heat) {
		this.heat = heat;
	}

	@Override
	public int getHull() {
		return hull;
	}

	/**
	 * Setzt die Huellenpunkte.
	 * @param hull Die Huellenpunkte
	 */
	public void setHull(int hull) {
		this.hull = hull;
	}

	@Override
	public int getHydro() {
		return hydro;
	}

	/**
	 * Setzt die Nahrungsproduktion.
	 * @param hydro Die Nahrungsproduktion
	 */
	public void setHydro(int hydro) {
		this.hydro = hydro;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	@Override
	public int getJDocks() {
		return jDocks;
	}

	/**
	 * Setzt die Jaegerdocks.
	 * @param docks Die Jaegerdocks
	 */
	public void setJDocks(int docks) {
		jDocks = docks;
	}

	@Override
	public String getMaxHeat() {
		return maxHeat;
	}

	/**
	 * Setzt die max. Waffenueberhitzungs.
	 * @param maxHeat Die Waffenueberhitzung
	 */
	public void setMaxHeat(String maxHeat) {
		this.maxHeat = maxHeat;
	}

	/**
	 * Gibt die Moduldaten der eingebauten Module zurueck.
	 * @return Die Moduldaten
	 */
	public String getModules() {
		return modules;
	}

	/**
	 * Setzt die Moduldaten der eingebauten Module.
	 * @param modules Die Moduldaten
	 */
	public void setModules(String modules) {
		this.modules = modules;
	}

	@Override
	public String getNickname() {
		return nickname;
	}

	/**
	 * Setzt den Namen.
	 * @param nickname Der Name
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@Override
	public int getOneWayWerft() {
		return oneWayWerft;
	}

	/**
	 * Setzt die Einweg-Werftdaten.
	 * @param oneWayWerft Die Einweg-Werftdaten
	 */
	public void setOneWayWerft(int oneWayWerft) {
		this.oneWayWerft = oneWayWerft;
	}

	@Override
	public int getPanzerung() {
		return panzerung;
	}

	/**
	 * Setzt die Panzerung.
	 * @param panzerung Die Panzerung
	 */
	public void setPanzerung(int panzerung) {
		this.panzerung = panzerung;
	}

	@Override
	public String getPicture() {
		return picture;
	}

	/**
	 * Setzt das Bild.
	 * @param picture Das Bild
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	@Override
	public int getRa() {
		return ra;
	}

	/**
	 * Setzt die Reaktoreffizienz bei Antimaterie.
	 * @param ra Die Reaktoreffizienz
	 */
	public void setRa(int ra) {
		this.ra = ra;
	}

	@Override
	public int getRd() {
		return rd;
	}

	/**
	 * Setzt die Reaktoreffizienz bei Deuterium.
	 * @param rd Die Reaktoreffizienz
	 */
	public void setRd(int rd) {
		this.rd = rd;
	}

	@Override
	public int getReCost() {
		return reCost;
	}
	
	/**
	 * Setzt die Wartungskosten.
	 * @param recost Die Wartungskosten
	 */
	public void setReCost(int recost) {
		this.reCost = recost;
	}

	@Override
	public int getRm() {
		return rm;
	}

	/**
	 * Setzt die Gesamtenergieproduktion.
	 * @param rm Die Gesamtenergieproduktion
	 */
	public void setRm(int rm) {
		this.rm = rm;
	}

	@Override
	public int getRu() {
		return ru;
	}

	/**
	 * Setzt die Energieeffizienz bei Uran.
	 * @param ru Die Energieeffizienz
	 */
	public void setRu(int ru) {
		this.ru = ru;
	}

	@Override
	public int getSensorRange() {
		return sensorRange;
	}

	/**
	 * Setzt die Sensorenreichweite.
	 * @param sensorRange Die Sensorenreichweite
	 */
	public void setSensorRange(int sensorRange) {
		this.sensorRange = sensorRange;
	}

	@Override
	public int getShields() {
		return shields;
	}

	/**
	 * Setzt die Schildpunkte.
	 * @param shields Die Schildpunkte
	 */
	public void setShields(int shields) {
		this.shields = shields;
	}

	@Override
	public int getSize() {
		return size;
	}

	/**
	 * Setzt die Groesse.
	 * @param size Die Groesse
	 */
	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public int getTorpedoDef() {
		return torpedoDef;
	}

	/**
	 * Setzt die Torpedoabwehrdaten.
	 * @param torpedoDef Die Torpedoabwehrdaten
	 */
	public void setTorpedoDef(int torpedoDef) {
		this.torpedoDef = torpedoDef;
	}

	@Override
	public String getWeapons() {
		return weapons;
	}

	/**
	 * Setzt die Waffen.
	 * @param weapons Die Waffen
	 */
	public void setWeapons(String weapons) {
		this.weapons = weapons;
	}

	@Override
	public int getWerft() {
		return werft;
	}

	/**
	 * Setzt die Werftdaten.
	 * @param werft Die Werftdaten
	 */
	public void setWerft(int werft) {
		this.werft = werft;
	}
	
	@Override
	public int getGroupwrap() {
		return getShip().getBaseType().getGroupwrap();
	}
	
	@Override
	public String getTypeModules() {
		return getShip().getBaseType().getModules();
	}
	
	@Override
	public int getShipCount() {
		return getShip().getBaseType().getShipCount();
	}
	
	@Override
	public String getDescrip() {
		return getShip().getBaseType().getDescrip();
	}
	
	@Override
	public boolean isHide() {
		return getShip().getBaseType().isHide();
	}
	
	@Override
	public int getShipClass() {
		return getShip().getBaseType().getShipClass();
	}
	
	@Override
	public int getChance4Loot() {
		return getShip().getBaseType().getChance4Loot();
	}
	
	@Override
	public boolean isMilitary() {
		return getWeapons().indexOf('=') > -1;
	}
	
	/**
	 * @return <code>true</code>, falls es ein Versorger ist.
	 */
	public boolean isVersorger() {
		return versorger;
	}
	
	/**
	 * Setzt, ob es sich bei diesem Schiff um einen Versorger handelt.
	 * @param versorger <code>true</code>, falls es ein Versorger ist
	 */
	public void setVersorger(boolean versorger) {
		this.versorger = versorger;
	}
	
	@Override
	public int getTypeId() {
		return getShip().getBaseType().getId();
	}

	@Override
	public ShipTypeData getType() {
		return getShip().getBaseType();
	}
	
	@Override
	public boolean hasFlag(String flag) {
		if( getFlags().indexOf(flag) > -1 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public int getAblativeArmor() {
		return ablativeArmor;
	}

	/**
	 * Setzt den Wert der ablativen Panzerung.
	 * @param ablativeArmor Die ablative Panzerung
	 */
	public void setAblativeArmor(int ablativeArmor) {
		this.ablativeArmor = ablativeArmor;
	}

	@Override
	public boolean hasSrs() {
		return srs;
	}

	/**
	 * Setzt, ob das Schiff SRS hat oder nicht.
	 * @param srs <code>true</code>, falls es SRS haben soll
	 */
	public void setSrs(boolean srs) {
		this.srs = srs;
	}
	
	@Override
	public int getScanCost() {
		return scanCost;
	}

	/**
	 * Setzt die Energiekosten pro LRS-Scan (Gesamtuebersicht).
	 * @param scanCost Die Energiekosten
	 */
	public void setScanCost(int scanCost) {
		this.scanCost = scanCost;
	}
	
	@Override
	public int getPickingCost() {
		return pickingCost;
	}

	/**
	 * Setzt die Energiekosten pro LRS-Sektorscan (Scannen des Inhalts eines Sektors).
	 * @param pickingCost Die Energiekosten
	 */
	public void setPickingCost(int pickingCost) {
		this.pickingCost = pickingCost;
	}
	
	@Override
	public int getMinCrew()
	{
		return minCrew;
	}

	/**
	 * Crewwert.
	 * 
	 * @param minCrew Crewwert bei dem das Schiff noch normal funktioniert.
	 */
	public void setMinCrew(int minCrew)
	{
		this.minCrew = minCrew;
	}
	
	/**
	 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
	 * 
	 * @return Zahl zwischen 0 und 1.
	 */
	public double getLostInEmpChance()
	{
		return lostInEmpChance;
	}
	
	/**
	 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
	 * 
	 * @param lostInEmpChance Zahl zwischen 0 und 1.
	 */
	public void setLostInEmpChance(double lostInEmpChance)
	{
		this.lostInEmpChance = lostInEmpChance;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
