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
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	@OneToMany(orphanRemoval = true)
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
	private int scanCost;
	private int pickingCost;
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
	 * Konstruktor.
	 * @param changesetString Der String mit den Changeset-Informationen
	 */
	public SchiffstypModifikation(String changesetString)
	{
		if( changesetString.equals(""))
		{
			throw new RuntimeException("Keine Shiptype-Changeset-Informationen vorhanden");
		}

		Map<Weapon,Schiffswaffenkonfiguration> waffen = new HashMap<>();

		String[] changesets = StringUtils.split(changesetString, "|");
		for (String changeset1 : changesets)
		{
			String[] changeset = StringUtils.split(changeset1, ",");
			switch (changeset[0])
			{
				case "weapons":
					String[] weapon = StringUtils.split(changeset[1], "/");

					Weapon waffe = Weapons.get().weapon(weapon[0]);
					waffen.putIfAbsent(waffe, new Schiffswaffenkonfiguration(waffe, 0, 0, 0));
					waffen.get(waffe).setAnzahl(Integer.parseInt(weapon[1]));
					waffen.get(waffe).setHitze(Integer.parseInt(weapon[2]));
					break;
				case "flags":
					List<String> flagList = new ArrayList<>();
					String[] flags = StringUtils.split(changeset[1], "/");
					Collections.addAll(flagList, flags);
					this.flags = ShipTypeFlag.parseFlags(Common.implode(" ", flagList));
					break;
				case "nickname"://
					this.nickname = changeset[1];
					break;
				case "picture"://
					this.picture = changeset[1];
					break;
				case "ru": //
					this.ru = Integer.parseInt(changeset[1]);
					break;
				case "rd"://
					this.rd = Integer.parseInt(changeset[1]);
					break;
				case "ra"://
					this.ra = Integer.parseInt(changeset[1]);
					break;
				case "rm"://
					this.rm = Integer.parseInt(changeset[1]);
					break;
				case "eps"://
					this.eps = Integer.parseInt(changeset[1]);
					break;
				case "cost"://
					this.cost = Integer.parseInt(changeset[1]);
					break;
				case "hull"://
					this.hull = Integer.parseInt(changeset[1]);
					break;
				case "panzerung"://
					this.panzerung = Integer.parseInt(changeset[1]);
					break;
				case "ablativearmor"://
					this.ablativeArmor = Integer.parseInt(changeset[1]);
					break;
				case "cargo"://
					this.cargo = Long.parseLong(changeset[1]);
					break;
				case "nahrungcargo": //
					this.nahrungcargo = Long.parseLong(changeset[1]);
					break;
				case "heat"://
					this.heat = Integer.parseInt(changeset[1]);
					break;
				case "crew"://
					this.crew = Integer.parseInt(changeset[1]);
					break;
				case "maxunitsize"://
					this.maxunitsize = Integer.parseInt(changeset[1]);
					break;
				case "unitspace"://
					this.unitspace = Integer.parseInt(changeset[1]);
					break;
				case "torpdeff"://
					this.torpedoDef = Integer.parseInt(changeset[1]);
					break;
				case "shields"://
					this.shields = Integer.parseInt(changeset[1]);
					break;
				case "size"://
					this.size = Integer.parseInt(changeset[1]);
					break;
				case "jdocks"://
					this.jDocks = Integer.parseInt(changeset[1]);
					break;
				case "adocks"://
					this.aDocks = Integer.parseInt(changeset[1]);
					break;
				case "sensorrange"://
					this.sensorRange = Integer.parseInt(changeset[1]);
					break;
				case "hydro"://
					this.hydro = Integer.parseInt(changeset[1]);
					break;
				case "deutfactor"://
					this.deutFactor = Integer.parseInt(changeset[1]);
					break;
				case "recost"://
					this.reCost = Integer.parseInt(changeset[1]);
					break;
				case "werftslots"://
					this.werft = Integer.parseInt(changeset[1]);
					break;
				case "onewaywerft"://
					int oneWayWerft = Integer.parseInt(changeset[1]);
					if( oneWayWerft != 0 )
					{
						this.oneWayWerft = (ShipType)ContextMap.getContext().getDB().get(ShipType.class,oneWayWerft);
					}
					break;
				case "srs"://
					this.srs = Boolean.parseBoolean(changeset[1]);
					break;
				case "scancost"://
					this.scanCost = Integer.parseInt(changeset[1]);
					break;
				case "pickingcost"://
					this.pickingCost = Integer.parseInt(changeset[1]);
					break;
				case "mincrew"://
					this.minCrew = Integer.parseInt(changeset[1]);
					break;
				case "lostinempchance"://
					this.lostInEmpChance = Double.parseDouble(changeset[1]);
					break;
				case "bounty"://
					this.bounty = new BigInteger(changeset[1]);
					break;
				default:
					throw new RuntimeException("Unbekannte Changeset-Eigenschaft '" + changeset[0] + "'");
			}
		}

		this.waffen = new HashSet<>(waffen.values());
	}

	/**
	 * Gibt zurueck, um wieviel die externen Docks modifiziert werden.
	 * @return Die externen Docks
	 */
	public int getADocks() {
		return aDocks;
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
	 * Gibt zurueck, um wieviel der Cargo modifiziert wird.
	 * @return Der Cargo
	 */
	public long getCargo() {
		return cargo;
	}

	/**
	 * Gibt zurueck, um wieviel der NahrungCargo modifiziert wird.
	 * @return Der NahrungCargo
	 */
	public long getNahrungCargo() {
		return nahrungcargo;
	}

	/**
	 * Gibt zurueck, um wieviel die Flugkosten modifiziert werden.
	 * @return Die Flugkosten
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Gibt zurueck, um wieviel die Crew modifiziert wird.
	 * @return Die Crew
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Gibt zurueck, um wieviel die maximale Groesze der Einheiten modifiziert werden soll.
	 * @return Die maximale Groesze
	 */
	public int getMaxUnitSize() {
		return maxunitsize;
	}

	/**
	 * Gibt zurueck, um wie viel der Einheiten-Laderaum modifiziert werden soll.
	 * @return Der Einheiten Laderaum
	 */
	public int getUnitSpace() {
		return unitspace;
	}

	/**
	 * Gibt zurueck, um wieviel der Deutfaktor modifiziert wird.
	 * @return Der Deutfaktor
	 */
	public int getDeutFactor() {
		return deutFactor;
	}

	/**
	 * Gibt zurueck, um wieviel die EPS modifiziert werden.
	 * @return Die EPS
	 */
	public int getEps() {
		return eps;
	}

	/**
	 * Gibt zurueck, welche Flags zusaetzlich gesetzt werden.
	 * @return Die neuen Flags
	 */
	public Set<ShipTypeFlag> getFlags() {
		return flags;
	}

	/**
	 * Gibt zurueck, um wieviel die Antriebsueberhitzung modifiziert wird.
	 * @return Die Antriebsueberhitzung
	 */
	public int getHeat() {
		return heat;
	}

	/**
	 * Gibt zurueck, um wieviel die Huelle modifiziert wird.
	 * @return Die Huelle
	 */
	public int getHull() {
		return hull;
	}

	/**
	 * Gibt zurueck, um wieviel die Nahrungsproduktion modifiziert wird.
	 * @return Die Nahrungsproduktion
	 */
	public int getHydro() {
		return hydro;
	}

	/**
	 * Gibt zurueck, um wieviel die Jaegerdocks modifiziert werden.
	 * @return Die Jaegerdocks
	 */
	public int getJDocks() {
		return jDocks;
	}

	/**
	 * Gibt den neuen Nickname zurueck.
	 * @return Der Name
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * Gibt die Einweg-Werftdaten zurueck.
	 * @return Die Einweg-Werftdaten
	 */
	public ShipType getOneWayWerft() {
		return oneWayWerft;
	}

	/**
	 * Gibt zurueck, um wieviel die Panzerung modifiziert wird.
	 * @return Die Panzerung
	 */
	public int getPanzerung() {
		return panzerung;
	}

	/**
	 * Gibt zurueck, um wieviel die ablative Panzerung modifiziert wird.
	 * @return Die ablative Panzerung
	 */
	public int getAblativeArmor() {
		return this.ablativeArmor;
	}

	/**
	 * Gibt das neue Bild zurueck.
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Antimaterie modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRa() {
		return ra;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Deuterium modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRd() {
		return rd;
	}

	/**
	 * Gibt zurueck, um wieviel die Wartungskosten modifiziert werden.
	 * @return Die Wartungskosten
	 */
	public int getReCost() {
		return reCost;
	}

	/**
	 * Gibt zurueck, um wieviel die Gesamtenergieproduktion des Reaktors modifiziert wird.
	 * @return Die Gesamtenergieproduktion
	 */
	public int getRm() {
		return rm;
	}

	/**
	 * Gibt zurueck, um wieviel der Reaktorwert fuer Uran modifiziert wird.
	 * @return Der Reaktorwert
	 */
	public int getRu() {
		return ru;
	}

	/**
	 * Gibt zurueck, um wieviel die Sensorenreichweite modifiziert wird.
	 * @return Die Sensorenreichweite
	 */
	public int getSensorRange() {
		return sensorRange;
	}

	/**
	 * Gibt zurueck, um wieviel die Schilde modifiziert werden.
	 * @return Die Schilde
	 */
	public int getShields() {
		return shields;
	}

	/**
	 * Gibt zurueck, um wieviel die Schiffsgroesse modifiziert wird.
	 * @return Die Schiffsgroesse
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gibt zurueck, um wieviel die Torpedoabwehr modifiziert wird.
	 * @return Die Torpedoabwehr
	 */
	public int getTorpedoDef() {
		return torpedoDef;
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
	 * Gibt die neuen Werftdaten zurueck.
	 * @return Die Werftdaten
	 */
	public int getWerft() {
		return werft;
	}

	/**
	 * Gibt zurueck, ob SRS vorhanden sein sollen.
	 * @return <code>true</code>, falls SRS vorhanden sein sollen
	 */
	public Boolean hasSrs() {
		return srs;
	}

	/**
	 * Gibt zurueck, wieviel ein LRS-Scan an Energie kosten soll.
	 * @return Die Energiekosten
	 */
	public int getScanCost() {
		return scanCost;
	}

	/**
	 * Gibt zurueck, wieviel ein LRS-Sektorscan (Scannen des Inhalts eines Sektors) kosten soll.
	 * @return Die Energiekosten
	 */
	public int getPickingCost() {
		return pickingCost;
	}

	/**
	 * @return Crewwert bei dem das Schiff noch normal funktioniert.
	 */
	public int getMinCrew()
	{
		return this.minCrew;
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
	 * @return Das Changeset als Text.
	 */
	@Override
	public String toString()
	{
		String itemstring = "";
		if ( getNickname() != null) {
			itemstring = itemstring + "nickname," + getNickname() + "|";
		}
		if ( getRu() != 0) {
			itemstring = itemstring + "ru," + getRu() + "|";
		}
		if ( getRd() != 0) {
			itemstring = itemstring + "rd," + getRd() + "|";
		}
		if ( getRa() != 0) {
			itemstring = itemstring + "ra," + getRa() + "|";
		}
		if ( getRm() != 0) {
			itemstring = itemstring + "rm," + getRm() + "|";
		}
		if ( getEps() != 0) {
			itemstring = itemstring + "eps," + getEps() + "|";
		}
		if ( getCost() != 0) {
			itemstring = itemstring + "cost," + getCost() + "|";
		}
		if ( getHull() != 0) {
			itemstring = itemstring + "hull," + getHull() + "|";
		}
		if ( getPanzerung() != 0) {
			itemstring = itemstring + "panzerung," + getPanzerung() + "|";
		}
		if ( getAblativeArmor() != 0) {
			itemstring = itemstring + "ablativearmor," + getAblativeArmor() + "|";
		}
		if ( getCargo() != 0) {
			itemstring = itemstring + "cargo," + getCargo() + "|";
		}
		if ( getNahrungCargo() != 0) {
			itemstring = itemstring + "nahrungcargo," + getNahrungCargo() + "|";
		}
		if ( getHeat() != 0) {
			itemstring = itemstring + "heat," + getHeat() + "|";
		}
		if ( getCrew() != 0) {
			itemstring = itemstring + "crew," + getCrew() + "|";
		}
		if ( getMaxUnitSize() != 0) {
			itemstring = itemstring + "maxunitsize," + getMaxUnitSize() + "|";
		}
		if ( getUnitSpace() != 0) {
			itemstring = itemstring + "unitspace," + getUnitSpace() + "|";
		}
		if ( getTorpedoDef() != 0) {
			itemstring = itemstring + "torpdeff," + getTorpedoDef() + "|";
		}
		if ( getShields() != 0) {
			itemstring = itemstring + "shields," + getShields() + "|";
		}
		if ( getSize() != 0) {
			itemstring = itemstring + "size," + getSize() + "|";
		}
		if ( getJDocks() != 0) {
			itemstring = itemstring + "jdocks," + getJDocks() + "|";
		}
		if ( getADocks() != 0) {
			itemstring = itemstring + "adocks," + getADocks() + "|";
		}
		if ( getSensorRange() != 0) {
			itemstring = itemstring + "sensorrange," + getSensorRange() + "|";
		}
		if ( getHydro() != 0) {
			itemstring = itemstring + "hydro," + getHydro() + "|";
		}
		if ( getDeutFactor() != 0) {
			itemstring = itemstring + "deutfactor," + getDeutFactor() + "|";
		}
		if ( getReCost() != 0) {
			itemstring = itemstring + "recost," + getReCost() + "|";
		}
		if ( getWerft() != 0) {
			itemstring = itemstring + "werftslots," + getWerft() + "|";
		}
		if ( this.oneWayWerft != null) {
			itemstring = itemstring + "onewaywerft," + this.oneWayWerft.getId() + "|";
		}
		if ( getScanCost() != 0) {
			itemstring = itemstring + "scancost," + getScanCost() + "|";
		}
		if ( getPickingCost() != 0) {
			itemstring = itemstring + "pickingcost," + getPickingCost() + "|";
		}
		if ( getMinCrew() != 0) {
			itemstring = itemstring + "mincrew," + getMinCrew() + "|";
		}
		if ( getLostInEmpChance() != 0) {
			itemstring = itemstring + "getlostinempchance," + getLostInEmpChance() + "|";
		}
		if ( !getFlags().isEmpty()) {
			itemstring = itemstring + "flags," + getFlags().stream().map(ShipTypeFlag::getFlag).collect(Collectors.joining("/")) + "|";
		}
		if ( hasSrs() != null && hasSrs()) {
			itemstring = itemstring + "srs,true|";
		}
		for( Schiffswaffenkonfiguration conf: getWaffen()) {
			itemstring = itemstring + "weapons," + conf.getWaffe().getId() + "/" + conf.getAnzahl() + "/" + conf.getHitze() + "|";
		}
		itemstring = itemstring.substring(0, itemstring.length() - 1);
		return itemstring;
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
		private volatile String weapons;
		private volatile String maxheat;
		private volatile String baseWeapons;
		private volatile String baseHeat;

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

			String baseWeapons = inner.getWeapons();
			String baseHeat = inner.getMaxHeat();

			Map<String,String> weaponlist = Weapons.parseWeaponList(baseWeapons);
			Map<String,String> heatlist = Weapons.parseWeaponList(baseHeat);

			// Weapons
			Set<Schiffswaffenkonfiguration> mod = SchiffstypModifikation.this.getWaffen();
			for( Schiffswaffenkonfiguration wpn: mod ) {
				String aweapon = wpn.getWaffe().getId();
				int acount = wpn.getAnzahl();
				int aheat = wpn.getHitze();

				if( wpnrpl != null ) {
					if( NumberUtils.toInt(weaponlist.get(wpnrpl)) > 0 ) {
						if( NumberUtils.toInt(weaponlist.get(wpnrpl)) > acount ) {
							int rplCount = NumberUtils.toInt(weaponlist.get(wpnrpl));
							int rplHeat = NumberUtils.toInt(heatlist.get(wpnrpl));
							heatlist.put(wpnrpl, Integer.toString(rplHeat - acount*(rplHeat/rplCount)));
							weaponlist.put(wpnrpl, Integer.toString(rplCount - acount));

							weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
							heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));
						}
						else {
							heatlist.remove(wpnrpl);
							weaponlist.remove(wpnrpl);

							weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
							heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));
						}
					}
				}
				else {
					weaponlist.put(aweapon, Integer.toString(NumberUtils.toInt(weaponlist.get(aweapon)) + acount));
					heatlist.put(aweapon,  Integer.toString(NumberUtils.toInt(heatlist.get(aweapon)) + aheat));

					if( NumberUtils.toInt(weaponlist.get(aweapon)) <= 0 ) {
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
					heatlist.put(weapon, Integer.toString(modheat));
				}
				else
				{
					String heatweapon = heatlist.get(weapon);
					heatlist.put(weapon, Integer.toString(Integer.parseInt(heatweapon) + modheat));
				}
			}

			this.baseWeapons = baseWeapons;
			this.baseHeat = baseHeat;
			this.weapons = Weapons.packWeaponList(weaponlist);
			this.maxheat = Weapons.packWeaponList(heatlist);
		}

		@Override
		public String getMaxHeat() {
			if( (this.maxheat == null) || !inner.getMaxHeat().equals(baseHeat) ) {
				calcWeaponData();
			}
			return this.maxheat;
		}

		@Override
		public String getNickname() {
			if( SchiffstypModifikation.this.getNickname() == null ) {
				return inner.getNickname();
			}
			return SchiffstypModifikation.this.getNickname();
		}

		@Override
		public ShipType getOneWayWerft() {
			if( SchiffstypModifikation.this.getOneWayWerft() == null ) {
				return inner.getOneWayWerft();
			}
			return SchiffstypModifikation.this.getOneWayWerft();
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
			if( SchiffstypModifikation.this.getPicture() == null ) {
				return inner.getPicture();
			}
			return SchiffstypModifikation.this.getPicture();
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
		public String getWeapons() {
			if( (this.weapons == null) || !inner.getWeapons().equals(baseWeapons) ) {
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
			return getWeapons().indexOf('=') > -1;
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
		public int getPickingCost() {
			return SchiffstypModifikation.this.getPickingCost() + inner.getPickingCost();
		}


		@Override
		public int getScanCost() {
			return SchiffstypModifikation.this.getScanCost() + inner.getScanCost();
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
