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

import net.driftingsouls.ds2.server.config.Weapons;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigInteger;
import java.util.Map;

/**
 * Ein Schiffstyp.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_types")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ShipType implements ShipTypeData {
	/**
	 * Kennzeichnet die maximale Groesse, die ein kleines Schiff (z.B. ein Jaeger) haben kann .
	 */
	public static final int SMALL_SHIP_MAXSIZE = 3;
	
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String nickname;
	@Column(nullable = false)
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
	@Lob
	@Column(nullable = false)
	private String weapons;
	@Lob
	@Column(name="maxheat", nullable = false)
	private String maxHeat;
	@Column(name="torpedodef", nullable = false)
	private int torpedoDef;
	private int shields;
	private int size;
	@Column(name="jdocks", nullable = false)
	private int jDocks;
	@Column(name="adocks", nullable = false)
	private int aDocks;
	@Column(name="sensorrange", nullable = false)
	private int sensorRange;
	private int hydro;
	@Column(name="recost", nullable = false)
	private int reCost;
	@Lob
	@Column(nullable = false)
	private String descrip;
	@Column(name="deutfactor", nullable = false)
	private int deutFactor;
	@Column(name="class", nullable = false)
	private ShipClasses shipClass;
	@Lob
	@Column(nullable = false)
	private String flags;
	private int groupwrap;
	private int werft;
	@ManyToOne
	@JoinColumn(name="ow_werft")
	@ForeignKey(name="ship_types_fk_ship_types")
	private ShipType oneWayWerft;
	private int chance4Loot;
	@Lob
	@Column(nullable = false)
	private String modules;
	private boolean hide;
	private int ablativeArmor;
	private boolean srs;
	private int minCrew;
	private double lostInEmpChance;
	private int maxunitsize;
	private int unitspace;
	@Index(name="shiptype_versorger")
	private boolean versorger;
	@Column(nullable = false, scale = 0)
	private BigInteger bounty = BigInteger.ZERO;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ShipType() {
		this.picture = "";
		this.weapons = "";
		this.maxHeat = "";
		this.flags = "";
		this.descrip = "";
		this.modules = "";
		this.nickname = "";
		this.shipClass = ShipClasses.UNBEKANNT;
	}

	/**
	 * Konstruktor.
	 * @param cls Die Klasse zu der der neue Schiffstyp gehoert
	 */
	public ShipType(ShipClasses cls)
	{
		this();

		this.shipClass = cls;
	}

    @Override
    public BigInteger getBounty()
    {
        return bounty;
    }

	@Override
	public int getADocks() {
		return aDocks;
	}

	@Override
	public long getCargo() {
		return cargo;
	}
	
	@Override
	public long getNahrungCargo() {
		return nahrungcargo;
	}

	@Override
	public int getChance4Loot() {
		return chance4Loot;
	}

	@Override
	public int getCost() {
		return cost;
	}

	@Override
	public int getCrew() {
		return crew;
	}
	
	@Override
	public String getDescrip() {
		return descrip;
	}

	@Override
	public int getDeutFactor() {
		return deutFactor;
	}

	@Override
	public int getEps() {
		return eps;
	}
	
	@Override
	public java.util.EnumSet<ShipTypeFlag> getFlags() {
		return ShipTypeFlag.parseFlags(flags);
	}

	@Override
	public int getGroupwrap() {
		return groupwrap;
	}

	@Override
	public int getHeat() {
		return heat;
	}

	@Override
	public boolean isHide() {
		return hide;
	}

	@Override
	public int getHull() {
		return hull;
	}

	@Override
	public int getHydro() {
		return hydro;
	}

	/**
	 * Gibt die ID des Schifftyps zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	@Override
	public int getJDocks() {
		return jDocks;
	}

	@Override
	public Map<String, Integer> getMaxHeat() {
		return Weapons.parseWeaponList(maxHeat);
	}
	
	/**
	 * Gibt die Modulsteckplaetze des Schiffstyps zurueck.
	 * @return Die Modulsteckplaetze
	 */
	public String getModules() {
		return modules;
	}

	@Override
	public String getTypeModules() {
		return getModules();
	}

	@Override
	public String getNickname() {
		return nickname;
	}

	@Override
	public ShipType getOneWayWerft() {
		return oneWayWerft;
	}

	@Override
	public int getPanzerung() {
		return panzerung;
	}

	@Override
	public String getPicture() {
		return picture;
	}

	@Override
	public int getRa() {
		return ra;
	}

	@Override
	public int getRd() {
		return rd;
	}

	@Override
	public int getReCost() {
		return reCost;
	}

	@Override
	public int getRm() {
		return rm;
	}

	@Override
	public int getRu() {
		return ru;
	}

	@Override
	public int getSensorRange() {
		return sensorRange;
	}

	@Override
	public int getShields() {
		return shields;
	}

	@Override
	public ShipClasses getShipClass() {
		return shipClass;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getTorpedoDef() {
		return torpedoDef;
	}

	@Override
	public int getTypeId() {
		return getId();
	}
	
	@Override
	public Map<String, Integer> getWeapons() {
		return Weapons.parseWeaponList(weapons);
	}

	@Override
	public int getWerft() {
		return werft;
	}
	
	@Override
	public int getMaxUnitSize() {
		return maxunitsize;
	}
	
	@Override
	public int getUnitSpace() {
		return unitspace;
	}
	
	@Override
	public boolean isMilitary() {
		return !getWeapons().isEmpty();
	}
	
	/**
	 * @return <code>true</code>, wenn dieses Schiff ein Versorger ist.
	 */
	@Override
	public boolean isVersorger() {
		return versorger;
	}
	
	/**
	 * Setzt, ob es sich um einen Versorger handelt.
	 * @param versorger <code>true</code>, falls es ein Versorger ist
	 */
	public void setVersorger(boolean versorger) {
		this.versorger = versorger;
	}

	@Override
	public boolean hasFlag(@Nonnull ShipTypeFlag flag)
	{
		return getFlags().contains(flag);
	}

	@Override
	public ShipTypeData getType() {
		return this;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Diese Klasse ist unveraenderbar");
	}

	@Override
	public int getAblativeArmor() {
		return ablativeArmor;
	}

	@Override
	public boolean hasSrs() {
		return srs;
	}

	/**
	 * Setzt die ablative Panzerung.
	 * @param ablativeArmor Die Panzerung
	 */
	public void setAblativeArmor(int ablativeArmor) {
		this.ablativeArmor = ablativeArmor;
	}

	/**
	 * Setzt die Anzahl der externen Docks.
	 * @param docks Die Docks
	 */
	public void setADocks(int docks) {
		aDocks = docks;
	}

    /**
     * @param bounty Setzt das Kopfgeld fuer den Abschuss dieses Schiffes.
     */
    public void setBounty(BigInteger bounty)
    {
        this.bounty = bounty;
    }

	/**
	 * Setzt den vorhandenen Cargo.
	 * @param cargo Der Cargo
	 */
	public void setCargo(long cargo) {
		this.cargo = cargo;
	}
	
	/**
	 * Setzt den NahrungsCargo.
	 * @param nahrungcargo Der Nahrungscargo
	 */
	public void setNahrungCargo(long nahrungcargo) {
		this.nahrungcargo = nahrungcargo;
	}

	/**
	 * Setzt die Wahrscheinlichkeit fuer einen Drop.
	 * @param chance4Loot Die Wahrscheinlichkeit
	 */
	public void setChance4Loot(int chance4Loot) {
		this.chance4Loot = chance4Loot;
	}

	/**
	 * Setzt die Flugkosten in Energie.
	 * @param cost Die Kosten
	 */
	public void setCost(int cost) {
		this.cost = cost;
	}

	/**
	 * Setzt die maximale Crewmenge.
	 * @param crew Die Crewmenge
	 */
	public void setCrew(int crew) {
		this.crew = crew;
	}

	/**
	 * Setzt die Beschreibung des Schiffstyps.
	 * @param descrip Die Beschreibung
	 */
	public void setDescrip(String descrip) {
		this.descrip = descrip;
	}

	/**
	 * Setzt den Faktor mit dem das Schiff Deuterium extrahieren kann.
	 * @param deutFactor Der Faktor
	 */
	public void setDeutFactor(int deutFactor) {
		this.deutFactor = deutFactor;
	}

	/**
	 * Setzt die maximale Energiemenge des Schiffs.
	 * @param eps Die Energiemenge
	 */
	public void setEps(int eps) {
		this.eps = eps;
	}

	/**
	 * Setzt die Flags des Schiffs.
	 * @param flags Die Flags
	 */
	public void setFlags(String flags) {
		this.flags = flags;
	}

	/**
	 * Setzt den Schwellenwert fuer die Gruppierung von Schiffen dieses Typs.
	 * @param groupwrap Der Schwellenwert
	 */
	public void setGroupwrap(int groupwrap) {
		this.groupwrap = groupwrap;
	}

	/**
	 * Setzt die Ueberhitzung pro geflogenen Feld.
	 * @param heat Die Ueberhitzung
	 */
	public void setHeat(int heat) {
		this.heat = heat;
	}

	/**
	 * Setzt, ob der Schiffstyp versteckt, d.h. nicht fuer alle sichtbar ist.
	 * @param hide <code>true</code>, falls er versteckt ist
	 */
	public void setHide(boolean hide) {
		this.hide = hide;
	}

	/**
	 * Setzt den maximalen Huellenwert des Typs.
	 * @param hull Der Huellenwert
	 */
	public void setHull(int hull) {
		this.hull = hull;
	}

	/**
	 * Setzt die Menge der produzierten Nahrung pro Tick.
	 * @param hydro Die Menge
	 */
	public void setHydro(int hydro) {
		this.hydro = hydro;
	}

	/**
	 * Setzt die Anzahl der Jaegerdocks.
	 * @param docks Die Anzahl
	 */
	public void setJDocks(int docks) {
		jDocks = docks;
	}

	/**
	 * Setzt die maximale Ueberhitzung der Waffen.
	 * @param maxHeat Die max. Hitze
	 */
	public void setMaxHeat(Map<String,Integer> maxHeat) {
		this.maxHeat = Weapons.packWeaponList(maxHeat);
	}

	/**
	 * Setzt die Modulslots des Schiffes.
	 * @param modules Die Slots
	 */
	public void setModules(String modules) {
		this.modules = modules;
	}

	/**
	 * Setzt den Namen des Typs.
	 * @param nickname Der Name
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * Setzt das Gegenstueck beim Bau, falls es sich um eine Einwegwerft handelt.
	 * Falls der Wert <code>null</code> ist handelt es sich nicht um eine Einwegwerft
	 * @param oneWayWerft Der Schiffstyp
	 */
	public void setOneWayWerft(ShipType oneWayWerft) {
		this.oneWayWerft = oneWayWerft;
	}

	/**
	 * Setzt die Panzerung des Schiffes.
	 * @param panzerung Die Panzerung
	 */
	public void setPanzerung(int panzerung) {
		this.panzerung = panzerung;
	}

	/**
	 * Setzt das Bild des Schiffstyps.
	 * @param picture Das Bild
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	/**
	 * Setzt die Reaktoreffizienz bei Antimaterie.
	 * @param ra Die Effizienz
	 */
	public void setRa(int ra) {
		this.ra = ra;
	}

	/**
	 * Setzt die Reaktoreffizienz bei Deuterium.
	 * @param rd Die Effizienz
	 */
	public void setRd(int rd) {
		this.rd = rd;
	}

	/**
	 * Setzt die Wartungskosten in RE.
	 * @param reCost Die Kosten
	 */
	public void setReCost(int reCost) {
		this.reCost = reCost;
	}

	/**
	 * Setzt die Gesamtenergieproduktion pro Tick.
	 * @param rm Die Energie
	 */
	public void setRm(int rm) {
		this.rm = rm;
	}

	/**
	 * Setzt die Reaktoreffizienz bei Uran.
	 * @param ru Die Effizienz
	 */
	public void setRu(int ru) {
		this.ru = ru;
	}

	/**
	 * Setzt die Sensorreichweite.
	 * @param sensorRange Die Reichweite
	 */
	public void setSensorRange(int sensorRange) {
		this.sensorRange = sensorRange;
	}

	/**
	 * Setzt die Schildpunkte.
	 * @param shields Die Schildpunkte
	 */
	public void setShields(int shields) {
		this.shields = shields;
	}

	/**
	 * Setzt die Schiffsklasse, zu der der Typ zugehoert.
	 * @param shipClass Die Schiffsklasse
	 */
	public void setShipClass(ShipClasses shipClass) {
		this.shipClass = shipClass;
	}

	/**
	 * Setzt die Groesse des Schiffstyps.
	 * @param size Die Groesse
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * Setzt, ob das Schiff einen SRS-Scanner besitzt.
	 * @param srs <code>true</code>, falls es einen solchen Scanner besitzt
	 */
	public void setSrs(boolean srs) {
		this.srs = srs;
	}

	/**
	 * Setzt den Verteidigungsfaktor gegenueber Torpedos.
	 * @param torpedoDef Der Faktor
	 */
	public void setTorpedoDef(int torpedoDef) {
		this.torpedoDef = torpedoDef;
	}

	/**
	 * Setzt die Waffen des Schiffes.
	 * @param weapons Die Waffen
	 */
	public void setWeapons(Map<String,Integer> weapons) {
		this.weapons = Weapons.packWeaponList(weapons);
	}

	/**
	 * Setzt die Anzahl der Werftslots.
	 * @param werft Die Anzahl
	 */
	public void setWerft(int werft)	{
		this.werft = werft;
	}
	
	/**
	 * Setzt die maximale Groesze der Einheiten.
	 * @param maxsize Die maximale Groesze
	 */
	public void setMaxUnitSize(int maxsize) {
		this.maxunitsize = maxsize;
	}

	/**
	 * Setzt den maximalen Laderaum der Einheiten.
	 * @param unitspace Der maximale Laderaum
	 */
	public void setUnitSpace(int unitspace) {
		this.unitspace = unitspace;
	}
	
	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * @return Crewwert bei dem das Schiff noch normal funktioniert.
	 */
	@Override
	public int getMinCrew()
	{
		return minCrew;
	}

	/**
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
	@Override
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
}
