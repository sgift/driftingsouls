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
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

/**
 * Ein Schiffstyp
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_types")
@Immutable
@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
public class ShipType implements ShipTypeData {
	/**
	 * Kennzeichnet die maximale Groesse, die ein kleines Schiff (z.B. ein Jaeger) haben kann 
	 */
	public static final int SMALL_SHIP_MAXSIZE = 3;
	
	@Id
	private int id;
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
	private int heat;
	private int crew;
	private int marines;
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
	@Column(name="recost")
	private int reCost;
	private String descrip;
	@Column(name="deutfactor")
	private int deutFactor;
	@Column(name="class")
	private int shipClass;
	private String flags;
	private int groupwrap;
	private int werft;
	@Column(name="ow_werft")
	private int oneWayWerft;
	private int chance4Loot;
	private String modules;
	private int shipCount;
	private boolean hide;
	private int ablativeArmor;
	private boolean srs;
	private int scanCost;
	private int pickingCost;
	
	/**
	 * Konstruktor
	 *
	 */
	public ShipType() {
		// EMPTY
	}

	public int getADocks() {
		return aDocks;
	}

	public long getCargo() {
		return cargo;
	}

	public int getChance4Loot() {
		return chance4Loot;
	}

	public int getCost() {
		return cost;
	}

	public int getCrew() {
		return crew;
	}
	
	public int getMarines(){
		return marines;
	}

	public String getDescrip() {
		return descrip;
	}

	public int getDeutFactor() {
		return deutFactor;
	}

	public int getEps() {
		return eps;
	}

	public String getFlags() {
		return flags;
	}

	public int getGroupwrap() {
		return groupwrap;
	}

	public int getHeat() {
		return heat;
	}

	public boolean isHide() {
		return hide;
	}

	public int getHull() {
		return hull;
	}

	public int getHydro() {
		return hydro;
	}

	/**
	 * Gibt die ID des Schifftyps zurueck
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	public int getJDocks() {
		return jDocks;
	}

	public String getMaxHeat() {
		return maxHeat;
	}
	
	/**
	 * Gibt die Modulsteckplaetze des Schiffstyps zurueck
	 * @return Die Modulsteckplaetze
	 */
	public String getModules() {
		return modules;
	}

	public String getTypeModules() {
		return getModules();
	}

	public String getNickname() {
		return nickname;
	}

	public int getOneWayWerft() {
		return oneWayWerft;
	}

	public int getPanzerung() {
		return panzerung;
	}

	public String getPicture() {
		return picture;
	}

	public int getRa() {
		return ra;
	}

	public int getRd() {
		return rd;
	}

	public int getReCost() {
		return reCost;
	}

	public int getRm() {
		return rm;
	}

	public int getRu() {
		return ru;
	}

	public int getSensorRange() {
		return sensorRange;
	}

	public int getShields() {
		return shields;
	}

	public int getShipClass() {
		return shipClass;
	}

	public int getShipCount() {
		return shipCount;
	}

	public int getSize() {
		return size;
	}

	public int getTorpedoDef() {
		return torpedoDef;
	}

	public int getTypeId() {
		return getId();
	}
	
	public String getWeapons() {
		return weapons;
	}

	public int getWerft() {
		return werft;
	}
	
	public boolean isMilitary() {
		return getWeapons().indexOf('=') > -1;
	}
	
	public boolean hasFlag(String flag) {
		if( getFlags().indexOf(flag) > -1 ) {
			return true;
		}
		return false;
	}
	
	public ShipTypeData getType() {
		return this;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Diese Klasse ist unveraenderbar");
	}

	public int getAblativeArmor() {
		return ablativeArmor;
	}

	public boolean hasSrs() {
		return srs;
	}

	public int getScanCost() {
		return scanCost;
	}

	public int getPickingCost() {
		return pickingCost;
	}
}
