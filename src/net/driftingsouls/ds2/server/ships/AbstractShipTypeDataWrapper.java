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

/**
 * Basisklasse fuer ShipTypeData-Wrapper
 * @author Christopher Jung
 *
 */
public abstract class AbstractShipTypeDataWrapper implements ShipTypeData {
	private ShipTypeData inner;
	
	/**
	 * Konstruktor
	 * @param inner Das Objekt, das der Wrapper kapseln soll
	 */
	public AbstractShipTypeDataWrapper(ShipTypeData inner) {
		this.inner = inner;
	}
	
	/**
	 * Gibt die innere Klasse zurueck
	 * @return Die innere Klasse
	 */
	protected final ShipTypeData getInner() {
		return this.inner;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		AbstractShipTypeDataWrapper wrapper = (AbstractShipTypeDataWrapper)super.clone();
		try {
			wrapper.inner = (ShipTypeData)this.inner.clone();
		}
		catch( CloneNotSupportedException e ) {
			wrapper.inner = this.inner;
		}
		
		return wrapper;
	}

	public int getADocks() {
		return inner.getADocks();
	}

	public long getCargo() {
		return inner.getCargo();
	}

	public int getChance4Loot() {
		return inner.getChance4Loot();
	}

	public int getCost() {
		return inner.getCost();
	}

	public int getCrew() {
		return inner.getCrew();
	}
	
	public int getMarines() {
		return inner.getMarines();
	}

	public String getDescrip() {
		return inner.getDescrip();
	}

	public int getDeutFactor() {
		return inner.getDeutFactor();
	}

	public int getEps() {
		return inner.getEps();
	}

	public String getFlags() {
		return inner.getFlags();
	}

	public int getGroupwrap() {
		return inner.getGroupwrap();
	}

	public int getHeat() {
		return inner.getHeat();
	}

	public int getHull() {
		return inner.getHull();
	}

	public int getHydro() {
		return inner.getHydro();
	}

	public int getJDocks() {
		return inner.getJDocks();
	}

	public String getMaxHeat() {
		return inner.getMaxHeat();
	}

	public String getNickname() {
		return inner.getNickname();
	}

	public int getOneWayWerft() {
		return inner.getOneWayWerft();
	}

	public int getPanzerung() {
		return inner.getPanzerung();
	}

	public String getPicture() {
		return inner.getPicture();
	}

	public int getRa() {
		return inner.getRa();
	}

	public int getRd() {
		return inner.getRd();
	}

	public int getReCost() {
		return inner.getReCost();
	}

	public int getRm() {
		return inner.getRm();
	}

	public int getRu() {
		return inner.getRu();
	}

	public int getSensorRange() {
		return inner.getSensorRange();
	}

	public int getShields() {
		return inner.getShields();
	}

	public int getShipClass() {
		return inner.getShipClass();
	}

	public int getShipCount() {
		return inner.getShipCount();
	}

	public int getSize() {
		return inner.getSize();
	}

	public int getTorpedoDef() {
		return inner.getTorpedoDef();
	}

	public ShipTypeData getType() {
		return inner.getType();
	}

	public int getTypeId() {
		return inner.getTypeId();
	}

	public String getTypeModules() {
		return inner.getTypeModules();
	}

	public String getWeapons() {
		return inner.getWeapons();
	}

	public String getWerft() {
		return inner.getWerft();
	}

	public boolean hasFlag(String flag) {
		return inner.hasFlag(flag);
	}

	public boolean isHide() {
		return inner.isHide();
	}

	public boolean isMilitary() {
		return inner.isMilitary();
	}
	
	public int getAblativeArmor() {
		return inner.getAblativeArmor();
	}
	
	public boolean hasSrs() {
		return inner.hasSrs();
	}
	
	public int getScanCost() {
		return inner.getScanCost();
	}
	
	public int getPickingCost() {
		return inner.getPickingCost();
	}
}
