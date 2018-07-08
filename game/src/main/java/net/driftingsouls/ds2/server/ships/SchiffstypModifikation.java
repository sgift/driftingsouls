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

import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ein Changeset fuer Schiffstypendaten-Aenderungen, wie sie z.B. von
 * Modulen vorgenommen werden koennen.
 * @author Christopher Jung
 *
 */
@Entity
public class SchiffstypModifikation
{
	@Id
	@GeneratedValue
	private Long id;
	@Version
	private int version;

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
	private int ablativeArmor;
	private long cargo;
	private long nahrungcargo;
	private int heat;
	private int crew;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "schiffstyp_modifikation_id")
	@ForeignKey(name="schiffstyp_modifikation_waffen_fk_schiffstyp_modifikation")
	private Set<Schiffswaffenkonfiguration> waffen = new HashSet<>();
	private int torpedoDef;
	private int shields;
	private int size;
	private int jDocks;
	private int aDocks;
	private int sensorRange;
	private int hydro;
	private int deutFactor;
	private int reCost;
	@ElementCollection
	@CollectionTable
	@ForeignKey(name="schiffstypmodifikation_flags_fk_schiffstypmodifikation")
	private Set<ShipTypeFlag> flags = new HashSet<>();
	private int werft;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="schiffstypmodifikation_fk_schiffstyp")
	private ShipType oneWayWerft;
	private Boolean srs;
	private int minCrew;
	private double lostInEmpChance;
	private int maxunitsize;
	private int unitspace;
    private BigInteger bounty;

	/**
	 * Leerer Konstruktor.
	 */
	public SchiffstypModifikation()
	{
		// Leerer Konstruktor
	}

	/**
	 * Gibt die ID der Modifkationsdaten zurueck.
	 * @return Die ID
	 */
	public Long getId()
	{
		return id;
	}

	/**
	 * Gibt zurueck, um wieviel die externen Docks modifiziert werden.
	 * @return Die externen Docks
	 */
	public int getADocks() {
		return aDocks;
	}

	/**
	 * Setzt, um wieviel die externen Docks modifiziert werden.
	 * @param aDocks Die externen Docks
	 */
	public void setADocks(int aDocks)
	{
		this.aDocks = aDocks;
	}

	/**
	 * Gibt das auf das Schiff ausgesetzte Kopfgeld in RE zurueck.
	 * @return Das Kopfgeld
	 */
    public BigInteger getBounty()
    {
        return bounty;
    }

	/**
	 * Setzt das auf das Schiff ausgesetzte Kopfgeld in RE.
	 * @param bounty Das Kopfgeld
	 */
	public void setBounty(BigInteger bounty)
	{
		this.bounty = bounty;
	}

	/**
	 * Gibt zurueck, um wieviel der Cargo modifiziert wird.
	 * @return Der Cargo
	 */
	public long getCargo() {
		return cargo;
	}

	/**
	 * Setzt, um wieviel der Cargo modifiziert wird.
	 * @param cargo Der Cargo
	 */
	public void setCargo(long cargo)
	{
		this.cargo = cargo;
	}

	/**
	 * Gibt zurueck, um wieviel der NahrungCargo modifiziert wird.
	 * @return Der NahrungCargo
	 */
	public long getNahrungCargo() {
		return nahrungcargo;
	}

	/**
	 * Setzt, um wieviel der NahrungCargo modifiziert wird.
	 * @param nahrungcargo Der NahrungCargo
	 */
	public void setNahrungCargo(long nahrungcargo)
	{
		this.nahrungcargo = nahrungcargo;
	}

	/**
	 * Gibt zurueck, um wieviel die Flugkosten modifiziert werden.
	 * @return Die Flugkosten
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Setzt, um wieviel die Flugkosten modifiziert werden.
	 * @param cost Die Flugkosten
	 */
	public void setCost(int cost)
	{
		this.cost = cost;
	}

	/**
	 * Gibt zurueck, um wieviel die Crew modifiziert wird.
	 * @return Die Crew
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Setzt, um wieviel die Crew modifiziert wird.
	 * @param crew Die Crew
	 */
	public void setCrew(int crew)
	{
		this.crew = crew;
	}

	/**
	 * Gibt zurueck, um wieviel die maximale Groesze der Einheiten modifiziert werden soll.
	 * @return Die maximale Groesze
	 */
	public int getMaxUnitSize() {
		return maxunitsize;
	}

	/**
	 * Setzt, um wieviel die maximale Groesze der Einheiten modifiziert werden soll.
	 * @param maxunitsize Die maximale Groesze
	 */
	public void setMaxUnitSize(int maxunitsize)
	{
		this.maxunitsize = maxunitsize;
	}

	/**
	 * Gibt zurueck, um wie viel der Einheiten-Laderaum modifiziert werden soll.
	 * @return Der Einheiten Laderaum
	 */
	public int getUnitSpace() {
		return unitspace;
	}

	/**
	 * Setzt, um wie viel der Einheiten-Laderaum modifiziert werden soll.
	 * @param unitspace Der Einheiten Laderaum
	 */
	public void setUnitSpace(int unitspace)
	{
		this.unitspace = unitspace;
	}

	/**
	 * Gibt zurueck, um wieviel der Deutfaktor modifiziert wird.
	 * @return Der Deutfaktor
	 */
	public int getDeutFactor() {
		return deutFactor;
	}

	/**
	 * Setzt, um wieviel der Deutfaktor modifiziert wird.
	 * @param deutFactor Der Deutfaktor
	 */
	public void setDeutFactor(int deutFactor)
	{
		this.deutFactor = deutFactor;
	}

	/**
	 * Gibt zurueck, um wieviel die EPS modifiziert werden.
	 * @return Die EPS
	 */
	public int getEps() {
		return eps;
	}

	/**
	 * Setzt, um wieviel die EPS modifiziert werden.
	 * @param eps Die EPS
	 */
	public void setEps(int eps)
	{
		this.eps = eps;
	}

	/**
	 * Gibt zurueck, welche Flags zusaetzlich gesetzt werden.
	 * @return Die neuen Flags
	 */
	public Set<ShipTypeFlag> getFlags() {
		return flags;
	}

	/**
	 * Setzt, welche Flags zusaetzlich gesetzt werden.
	 * @param flags Die neuen Flags
	 */
	public void setFlags(Set<ShipTypeFlag> flags)
	{
		this.flags = flags;
	}

	/**
	 * Gibt zurueck, um wieviel die Antriebsueberhitzung modifiziert wird.
	 * @return Die Antriebsueberhitzung
	 */
	public int getHeat() {
		return heat;
	}

	/**
	 * Setzt, um wieviel die Antriebsueberhitzung modifiziert wird.
	 * @param heat Die Antriebsueberhitzung
	 */
	public void setHeat(int heat)
	{
		this.heat = heat;
	}

	/**
	 * Gibt zurueck, um wieviel die Huelle modifiziert wird.
	 * @return Die Huelle
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Setzt, um wieviel die Huelle modifiziert wird.
	 * @param hull Die Huelle
	 */
	public void setHull(int hull)
	{
		this.hull = hull;
	}

	/**
	 * Gibt zurueck, um wieviel die Nahrungsproduktion modifiziert wird.
	 * @return Die Nahrungsproduktion
	 */
	public int getHydro() {
		return hydro;
	}

	/**
	 * Setzt, um wieviel die Nahrungsproduktion modifiziert wird.
	 * @param hydro Die Nahrungsproduktion
	 */
	public void setHydro(int hydro)
	{
		this.hydro = hydro;
	}

	/**
	 * Gibt zurueck, um wieviel die Jaegerdocks modifiziert werden.
	 * @return Die Jaegerdocks
	 */
	public int getJDocks() {
		return jDocks;
	}

	/**
	 * Setzt, um wieviel die Jaegerdocks modifiziert werden.
	 * @param jDocks Die Jaegerdocks
	 */
	public void setJDocks(int jDocks)
	{
		this.jDocks = jDocks;
	}

	/**
	 * Gibt den neuen Nickname zurueck.
	 * @return Der Name
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * Setzt den neuen Nickname.
	 * @param nickname Der Name
	 */
	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	/**
	 * Gibt die Einweg-Werftdaten zurueck.
	 * @return Die Einweg-Werftdaten
	 */
	public ShipType getOneWayWerft() {
		return oneWayWerft;
	}

	/**
	 * Setzt die Einweg-Werftdaten.
	 * @param oneWayWerft Die Einweg-Werftdaten
	 */
	public void setOneWayWerft(ShipType oneWayWerft)
	{
		this.oneWayWerft = oneWayWerft;
	}

	/**
	 * Gibt zurueck, um wieviel die Panzerung modifiziert wird.
	 * @return Die Panzerung
	 */
	public int getPanzerung() {
		return panzerung;
	}

	/**
	 * Setzt, um wieviel die Panzerung modifiziert wird.
	 * @param panzerung Die Panzerung
	 */
	public void setPanzerung(int panzerung)
	{
		this.panzerung = panzerung;
	}

	/**
	 * Gibt zurueck, um wieviel die ablative Panzerung modifiziert wird.
	 * @return Die ablative Panzerung
	 */
	public int getAblativeArmor() {
		return this.ablativeArmor;
	}

	/**
	 * Setzt, um wieviel die ablative Panzerung modifiziert wird.
	 * @param ablativeArmor Die ablative Panzerung
	 */
	public void setAblativeArmor(int ablativeArmor)
	{
		this.ablativeArmor = ablativeArmor;
	}

	/**
	 * Gibt das neue Bild zurueck.
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}

	/**
	 * Setzt das neue Bild.
	 * @param picture Das Bild
	 */
	public void setPicture(String picture)
	{
		this.picture = picture;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Antimaterie modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRa() {
		return ra;
	}

	/**
	 * Setzt, um wieviel der Reaktorwert fuer Antimaterie modifiziert wird.
	 * @param ra Der Reaktorwert
	 */
	public void setRa(int ra)
	{
		this.ra = ra;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Deuterium modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRd() {
		return rd;
	}

	/**
	 * Setzt, um wieviel der Reaktorwert fuer Deuterium modifiziert wird.
	 * @param rd Der Reaktorwert
	 */
	public void setRd(int rd)
	{
		this.rd = rd;
	}

	/**
	 * Gibt zurueck, um wieviel die Wartungskosten modifiziert werden.
	 * @return Die Wartungskosten
	 */
	public int getReCost() {
		return reCost;
	}

	/**
	 * Setzt, um wieviel die Wartungskosten modifiziert werden.
	 * @param reCost Die Wartungskosten
	 */
	public void setReCost(int reCost)
	{
		this.reCost = reCost;
	}

	/**
	 * Gibt zurueck, um wieviel die Gesamtenergieproduktion des Reaktors modifiziert wird.
	 * @return Die Gesamtenergieproduktion
	 */
	public int getRm() {
		return rm;
	}

	/**
	 * Setzt, um wieviel die Gesamtenergieproduktion des Reaktors modifiziert wird.
	 * @param rm Die Gesamtenergieproduktion
	 */
	public void setRm(int rm)
	{
		this.rm = rm;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Uran modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRu() {
		return ru;
	}

	/**
	 * Setzt, um wieviel der Reaktorwert fuer Uran modifiziert wird.
	 * @param ru Der Reaktorwert
	 */
	public void setRu(int ru)
	{
		this.ru = ru;
	}

	/**
	 * Gibt zurueck, um wieviel die Sensorenreichweite modifiziert wird.
	 * @return Die Sensorenreichweite
	 */
	public int getSensorRange() {
		return sensorRange;
	}

	/**
	 * Setzt, um wieviel die Sensorenreichweite modifiziert wird.
	 * @param sensorRange Die Sensorenreichweite
	 */
	public void setSensorRange(int sensorRange)
	{
		this.sensorRange = sensorRange;
	}

	/**
	 * Gibt zurueck, um wieviel die Schilde modifiziert werden.
	 * @return Die Schilde
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Setzt, um wieviel die Schilde modifiziert werden.
	 * @param shields Die Schilde
	 */
	public void setShields(int shields)
	{
		this.shields = shields;
	}

	/**
	 * Gibt zurueck, um wieviel die Schiffsgroesse modifiziert wird.
	 * @return Die Schiffsgroesse
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Setzt, um wieviel die Schiffsgroesse modifiziert wird.
	 * @param size Die Schiffsgroesse
	 */
	public void setSize(int size)
	{
		this.size = size;
	}

	/**
	 * Gibt zurueck, um wieviel die Torpedoabwehr modifiziert wird.
	 * @return Die Torpedoabwehr
	 */
	public int getTorpedoDef() {
		return torpedoDef;
	}

	/**
	 * Setzt, um wieviel die Torpedoabwehr modifiziert wird.
	 * @param torpedoDef Die Torpedoabwehr
	 */
	public void setTorpedoDef(int torpedoDef)
	{
		this.torpedoDef = torpedoDef;
	}

	/**
	 * Gibt die Modifikationsdaten der Waffen zurueck.
	 * @return Die Modifikationsdaten der Waffen
	 */
	public Set<Schiffswaffenkonfiguration> getWaffen()
	{
		return this.waffen;
	}

	/**
	 * Setzt die Modifikationsdaten der Waffen.
	 * @param waffen Die Modifikationsdaten der Waffen
	 */
	public void setWaffen(Set<Schiffswaffenkonfiguration> waffen)
	{
		this.waffen = waffen;
	}

	/**
	 * Gibt die neuen Werftdaten zurueck.
	 * @return Die Werftdaten
	 */
	public int getWerft() {
		return werft;
	}

	/**
	 * Setzt die neuen Werftdaten.
	 * @param werft Die Werftdaten
	 */
	public void setWerft(int werft)
	{
		this.werft = werft;
	}

	/**
	 * Gibt zurueck, ob SRS vorhanden sein sollen.
	 * @return <code>true</code>, falls SRS vorhanden sein sollen
	 */
	public Boolean hasSrs() {
		return srs;
	}

	/**
	 * Setzt, ob SRS vorhanden sein sollen.
	 * @param srs <code>true</code>, falls SRS vorhanden sein sollen
	 */
	public void setSrs(Boolean srs)
	{
		this.srs = srs;
	}

	/**
	 * @return Crewwert bei dem das Schiff noch normal funktioniert.
	 */
	public int getMinCrew()
	{
		return this.minCrew;
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
	public double getLostInEmpChance()
	{
		return this.lostInEmpChance;
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
	 * @return Das Changeset als Text.
	 */
	@Override
	public String toString()
	{
		List<String> effekte = new ArrayList<>();
		if ( nickname != null && !nickname.isEmpty()) {
			effekte.add("nickname=" + nickname);
		}
		if ( ru != 0) {
			effekte.add("ru=" + ru);
		}
		if ( rd != 0) {
			effekte.add("rd=" + rd);
		}
		if ( ra != 0) {
			effekte.add("ra=" + ra);
		}
		if ( rm != 0) {
			effekte.add("rm=" + rm);
		}
		if ( eps != 0) {
			effekte.add("eps=" + eps);
		}
		if ( cost != 0) {
			effekte.add("cost=" + cost);
		}
		if ( hull != 0) {
			effekte.add("hull=" + hull);
		}
		if ( panzerung != 0) {
			effekte.add("panzerung=" + panzerung);
		}
		if ( this.ablativeArmor != 0) {
			effekte.add("ablativearmor=" + this.ablativeArmor);
		}
		if ( cargo != 0) {
			effekte.add("cargo=" + cargo);
		}
		if ( nahrungcargo != 0) {
			effekte.add("nahrungcargo=" + nahrungcargo);
		}
		if ( heat != 0) {
			effekte.add("heat=" + heat);
		}
		if ( crew != 0) {
			effekte.add("crew=" + crew);
		}
		if ( maxunitsize != 0) {
			effekte.add("maxunitsize=" + maxunitsize);
		}
		if ( unitspace != 0) {
			effekte.add("unitspace=" + unitspace);
		}
		if ( torpedoDef != 0) {
			effekte.add("torpdeff=" + torpedoDef);
		}
		if ( shields != 0) {
			effekte.add("shields=" + shields);
		}
		if ( size != 0) {
			effekte.add("size=" + size);
		}
		if ( jDocks != 0) {
			effekte.add("jdocks=" + jDocks);
		}
		if ( aDocks != 0) {
			effekte.add("adocks=" + aDocks);
		}
		if ( sensorRange != 0) {
			effekte.add("sensorrange=" + sensorRange);
		}
		if ( hydro != 0) {
			effekte.add("hydro=" + hydro);
		}
		if ( deutFactor != 0) {
			effekte.add("deutfactor=" + deutFactor);
		}
		if ( reCost != 0) {
			effekte.add("recost=" + reCost);
		}
		if ( werft != 0) {
			effekte.add("werftslots=" + werft);
		}
		if ( this.oneWayWerft != null) {
			effekte.add("onewaywerft=" + this.oneWayWerft.getId());
		}
		if ( this.minCrew != 0) {
			effekte.add("mincrew=" + this.minCrew);
		}
		if ( this.lostInEmpChance != 0) {
			effekte.add("getlostinempchance=" + this.lostInEmpChance);
		}
		if ( !flags.isEmpty()) {
			effekte.add("flags=" + flags.stream().map(ShipTypeFlag::getFlag).collect(Collectors.joining("/")));
		}
		if ( srs != null) {
			effekte.add("srs="+ srs);
		}
		for( Schiffswaffenkonfiguration conf: this.waffen) {
			effekte.add("weapons=" + (conf.getWaffe() != null ? conf.getWaffe().getId() : "null") + "/" + conf.getAnzahl() + "/" + conf.getHitze());
		}
		return String.join(";", effekte);
	}

	/**
	 * Wendet das Changeset auf die angegebenen Schiffstypendaten an.
	 * @param type Die Schiffstypendaten
	 * @return Die modifizierten Daten
	 */
	public ShipTypeData applyTo(ShipTypeData type) {
		return new ShipTypeDataAdapter(type, new String[0]);
	}

	/**
	 * Wendet das Changeset auf die angegebenen Schiffstypendaten an.
	 * @param type Die Schiffstypendaten
	 * @param replaceWeapons Die Waffen, welche ggf ersetzt werden sollen
	 * @return Die modifizierten Daten
	 */
	public ShipTypeData applyTo(ShipTypeData type, String[] replaceWeapons) {
		return new ShipTypeDataAdapter(type, replaceWeapons);
	}

	private class ShipTypeDataAdapter implements ShipTypeData {
		private ShipTypeData inner;
		private String[] weaponrepl;
		private volatile EnumSet<ShipTypeFlag> flags;
		private volatile Map<String, Integer> weapons;
		private volatile Map<String, Integer> maxheat;

		ShipTypeDataAdapter(ShipTypeData type, String[] weaponrepl) {
			this.inner = type;
			this.weaponrepl = weaponrepl;
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			// Wenn die innere Klasse immutable ist, dann ist diese Klasse ebenfalls
			// immutable -> CloneNotSupportedException
			ShipTypeData inner = (ShipTypeData)this.inner.clone();

			ShipTypeDataAdapter clone = (ShipTypeDataAdapter)super.clone();
			clone.inner = inner;
			clone.weaponrepl = this.weaponrepl.clone();

			return clone;
		}

		@Override
		public int getADocks() {
			int value = inner.getADocks() + SchiffstypModifikation.this.getADocks();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public long getCargo() {
			long value = inner.getCargo() + SchiffstypModifikation.this.getCargo();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public long getNahrungCargo() {
			long value = inner.getNahrungCargo() + SchiffstypModifikation.this.getNahrungCargo();
			if( value < 0) {
				return 0;
			}
			return value;
		}

		@Override
		public int getChance4Loot() {
			return inner.getChance4Loot();
		}

		@Override
		public int getCost() {
			if( getType().getCost() > 0 ) {
				int value = inner.getCost() + SchiffstypModifikation.this.getCost();
				if( value < 1 ) {
					return 1;
				}
				return value;
			}
			return inner.getCost();
		}

		@Override
		public int getCrew() {
			if( getType().getCrew() > 0 ) {
				int value = inner.getCrew() + SchiffstypModifikation.this.getCrew();
				if( value < 1 ) {
					return 1;
				}
				return value;
			}
			return inner.getCrew();
		}

		@Override
		public int getMaxUnitSize() {
			if( getType().getMaxUnitSize() > 0 ) {
				int value = inner.getMaxUnitSize() + SchiffstypModifikation.this.getMaxUnitSize();
					if( value < 1 ) {
						return 1;
					}
				return value;
			}
			return inner.getMaxUnitSize();
		}

		@Override
		public int getUnitSpace() {
			if( getType().getUnitSpace() > 0 ) {
				int value = inner.getUnitSpace() + SchiffstypModifikation.this.getUnitSpace();
					if( value < 0 ) {
						return 0;
					}
				return value;
			}
			return inner.getUnitSpace();
		}

		@Override
		public String getDescrip() {
			return inner.getDescrip();
		}

		@Override
		public int getDeutFactor() {
			int value = inner.getDeutFactor() + SchiffstypModifikation.this.getDeutFactor();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getEps() {
			int value = inner.getEps() + SchiffstypModifikation.this.getEps();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public EnumSet<ShipTypeFlag> getFlags() {
			if( this.flags == null ) {
				EnumSet<ShipTypeFlag> flags = inner.getFlags().clone();
				flags.addAll(SchiffstypModifikation.this.getFlags());
				this.flags = flags;
			}
			return flags;
		}

		@Override
		public int getGroupwrap() {
			return inner.getGroupwrap();
		}

		@Override
		public int getHeat() {
			if( getType().getHeat() > 0 ) {
				int value = inner.getHeat() + SchiffstypModifikation.this.getHeat();
				if( value < 2 ) {
					return 2;
				}
				return value;
			}
			return inner.getHeat();
		}

		@Override
		public int getHull() {
			int value = inner.getHull() + SchiffstypModifikation.this.getHull();
			if( value < 1 ) {
				return 1;
			}
			return value;
		}

		@Override
		public int getHydro() {
			int value = inner.getHydro() + SchiffstypModifikation.this.getHydro();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getJDocks() {
			int value = inner.getJDocks() + SchiffstypModifikation.this.getJDocks();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		private void calcWeaponData() {
			String[] wpnrpllist = this.weaponrepl;
			int index = 0;

			String wpnrpl = wpnrpllist.length > index ? wpnrpllist[index++] : null;

			Map<String,Integer> weaponlist = inner.getWeapons();
			Map<String,Integer> heatlist = inner.getMaxHeat();

			// Weapons
			Set<Schiffswaffenkonfiguration> mod = SchiffstypModifikation.this.getWaffen();
			for( Schiffswaffenkonfiguration wpn: mod ) {
				String aweapon = wpn.getWaffe().getId();
				int acount = wpn.getAnzahl();
				int aheat = wpn.getHitze();

				if( wpnrpl != null ) {
					weaponlist.putIfAbsent(wpnrpl, 0);
					heatlist.putIfAbsent(wpnrpl, 0);
					if( weaponlist.get(wpnrpl) > 0 ) {
						if( weaponlist.get(wpnrpl) > acount ) {
							int rplCount = weaponlist.getOrDefault(wpnrpl,0);
							int rplHeat = heatlist.getOrDefault(wpnrpl,0);
							heatlist.put(wpnrpl, rplHeat - acount*(rplHeat/rplCount));
							weaponlist.put(wpnrpl, rplCount - acount);

							weaponlist.put(aweapon, weaponlist.getOrDefault(aweapon,0) + acount);
							heatlist.put(aweapon,  heatlist.getOrDefault(aweapon,0) + aheat);
						}
						else {
							heatlist.remove(wpnrpl);
							weaponlist.remove(wpnrpl);

							weaponlist.put(aweapon, weaponlist.getOrDefault(aweapon,0) + acount);
							heatlist.put(aweapon,  heatlist.getOrDefault(aweapon,0) + aheat);
						}
					}
				}
				else {
					weaponlist.put(aweapon, weaponlist.getOrDefault(aweapon,0) + acount);
					heatlist.put(aweapon,  heatlist.getOrDefault(aweapon,0) + aheat);

					if( weaponlist.get(aweapon) <= 0 ) {
						heatlist.remove(aweapon);
						weaponlist.remove(aweapon);
					}
				}

				wpnrpl = wpnrpllist.length > index ? wpnrpllist[index++] : null;
			}

			// MaxHeat
			for( Schiffswaffenkonfiguration entry: SchiffstypModifikation.this.getWaffen() )
			{
				String weapon = entry.getWaffe().getId();
				int modheat = entry.getMaxUeberhitzung();
				if (modheat == 0)
				{
					continue;
				}
				if (!heatlist.containsKey(weapon))
				{
					heatlist.put(weapon, modheat);
				}
				else
				{
					int heatweapon = heatlist.get(weapon);
					heatlist.put(weapon, heatweapon + modheat);
				}
			}

			this.weapons = weaponlist;
			this.maxheat = heatlist;
		}

		@Override
		public Map<String, Integer> getMaxHeat() {
			if( this.maxheat == null ) {
				calcWeaponData();
			}
			return this.maxheat;
		}

		@Override
		public String getNickname() {
			if( SchiffstypModifikation.this.nickname == null || SchiffstypModifikation.this.nickname.isEmpty() ) {
				return inner.getNickname();
			}
			return SchiffstypModifikation.this.getNickname();
		}

		@Override
		public ShipType getOneWayWerft() {
			if( SchiffstypModifikation.this.oneWayWerft == null ) {
				return inner.getOneWayWerft();
			}
			return SchiffstypModifikation.this.oneWayWerft;
		}

		@Override
		public int getPanzerung() {
			int value = inner.getPanzerung() + SchiffstypModifikation.this.getPanzerung();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public String getPicture() {
			if( SchiffstypModifikation.this.picture == null || SchiffstypModifikation.this.picture.isEmpty() ) {
				return inner.getPicture();
			}
			return SchiffstypModifikation.this.picture;
		}

		@Override
		public int getRa() {
			int value = inner.getRa() + SchiffstypModifikation.this.getRa();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getRd() {
			int value = inner.getRd() + SchiffstypModifikation.this.getRd();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getReCost() {
			int value = inner.getReCost() + SchiffstypModifikation.this.getReCost();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getRm() {
			int value = inner.getRm() + SchiffstypModifikation.this.getRm();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getRu() {
			int value = inner.getRu() + SchiffstypModifikation.this.getRu();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getSensorRange() {
			int value = inner.getSensorRange() + SchiffstypModifikation.this.getSensorRange();
			if( value < 0 ) {
				return 0;
			}
			if( value > 127 )
			{
				value = 127;
			}
			return value;
		}

		@Override
		public int getShields() {
			int value = inner.getShields() + SchiffstypModifikation.this.getShields();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public ShipClasses getShipClass() {
			return inner.getShipClass();
		}

		@Override
		public int getSize() {
			if( getType().getSize() > ShipType.SMALL_SHIP_MAXSIZE ) {
				int value = inner.getSize() + SchiffstypModifikation.this.getSize();
				if( value <= ShipType.SMALL_SHIP_MAXSIZE ) {
					return ShipType.SMALL_SHIP_MAXSIZE+1;
				}
				return value;
			}

			int value = inner.getSize() + SchiffstypModifikation.this.getSize();
			if( value > ShipType.SMALL_SHIP_MAXSIZE ) {
				return 3;
			}
			if( value < 1 ) {
				return 1;
			}
			return value;
		}

		@Override
		public int getTorpedoDef() {
			int value = inner.getTorpedoDef() + SchiffstypModifikation.this.getTorpedoDef();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public int getTypeId() {
			return inner.getTypeId();
		}

		@Override
		public String getTypeModules() {
			return inner.getTypeModules();
		}

		@Override
		public Map<String, Integer> getWeapons() {
			if( this.weapons == null ) {
				calcWeaponData();
			}

			return this.weapons;
		}

		@Override
		public int getWerft() {
			int value = inner.getWerft() + SchiffstypModifikation.this.getWerft();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public boolean hasFlag(@Nonnull ShipTypeFlag flag)
		{
			return getFlags().contains(flag);
		}

		@Override
		public boolean isHide() {
			return inner.isHide();
		}

		@Override
		public boolean isMilitary() {
			return !getWeapons().isEmpty();
		}

		@Override
		public boolean isVersorger() {
			return inner.isVersorger();
		}

		@Override
		public ShipTypeData getType() {
			return inner.getType();
		}

		@Override
		public int getAblativeArmor() {
			int value = inner.getAblativeArmor() + SchiffstypModifikation.this.getAblativeArmor();
			if( value < 0 ) {
				return 0;
			}
			return value;
		}

		@Override
		public boolean hasSrs() {
			if( SchiffstypModifikation.this.hasSrs() == null ) {
				return inner.hasSrs();
			}
			return inner.hasSrs() && SchiffstypModifikation.this.hasSrs();
		}

		@Override
		public int getMinCrew()
		{
			return SchiffstypModifikation.this.getMinCrew() + inner.getMinCrew();
		}

        @Override
		public BigInteger getBounty()
        {
            return SchiffstypModifikation.this.getBounty().add(inner.getBounty());
        }

		/**
		 * Wahrscheinlichkeit, dass das Schiff sich in einem EMP-Nebel verfliegt.
		 *
		 * @return Zahl zwischen 0 und 1.
		 */
		@Override
		public double getLostInEmpChance()
		{
			return SchiffstypModifikation.this.getLostInEmpChance() + inner.getLostInEmpChance();
		}
	}
}
