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

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.Map;

/**
 * Basisklasse fuer ShipTypeData-Wrapper.
 * @author Christopher Jung
 *
 */
public abstract class AbstractShipTypeDataWrapper implements ShipTypeData {
	private ShipTypeData inner;
	
	/**
	 * Konstruktor.
	 * @param inner Das Objekt, das der Wrapper kapseln soll
	 */
	public AbstractShipTypeDataWrapper(ShipTypeData inner) {
		this.inner = inner;
	}
	
	/**
	 * Gibt die innere Klasse zurueck.
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

	@Override
	public int getADocks() {
		return inner.getADocks();
	}

    @Override
	public BigInteger getBounty()
    {
        return inner.getBounty();
    }

	@Override
	public long getCargo() {
		return inner.getCargo();
	}
	
	@Override
	public long getNahrungCargo() {
		return inner.getNahrungCargo();
	}

	@Override
	public int getChance4Loot() {
		return inner.getChance4Loot();
	}

	@Override
	public int getCost() {
		return inner.getCost();
	}

	@Override
	public int getCrew() {
		return inner.getCrew();
	}
	
	@Override
	public int getMaxUnitSize() {
		return inner.getMaxUnitSize();
	}
	
	@Override
	public int getUnitSpace() {
		return inner.getUnitSpace();
	}

	@Override
	public String getDescrip() {
		return inner.getDescrip();
	}

	@Override
	public int getDeutFactor() {
		return inner.getDeutFactor();
	}

	@Override
	public int getEps() {
		return inner.getEps();
	}

	@Override
	public java.util.EnumSet<ShipTypeFlag> getFlags() {
		return inner.getFlags();
	}

	@Override
	public int getGroupwrap() {
		return inner.getGroupwrap();
	}

	@Override
	public int getHeat() {
		return inner.getHeat();
	}

	@Override
	public int getHull() {
		return inner.getHull();
	}

	@Override
	public int getHydro() {
		return inner.getHydro();
	}

	@Override
	public int getJDocks() {
		return inner.getJDocks();
	}

	@Override
	public Map<String, Integer> getMaxHeat() {
		return inner.getMaxHeat();
	}

	@Override
	public String getNickname() {
		return inner.getNickname();
	}

	@Override
	public ShipType getOneWayWerft() {
		return inner.getOneWayWerft();
	}

	@Override
	public int getPanzerung() {
		return inner.getPanzerung();
	}

	@Override
	public String getPicture() {
		return inner.getPicture();
	}

	@Override
	public int getRa() {
		return inner.getRa();
	}

	@Override
	public int getRd() {
		return inner.getRd();
	}

	@Override
	public int getReCost() {
		return inner.getReCost();
	}

	@Override
	public int getRm() {
		return inner.getRm();
	}

	@Override
	public int getRu() {
		return inner.getRu();
	}

	@Override
	public int getSensorRange() {
		return inner.getSensorRange();
	}

	@Override
	public int getShields() {
		return inner.getShields();
	}

	@Override
	public ShipClasses getShipClass() {
		return inner.getShipClass();
	}

	@Override
	public int getSize() {
		return inner.getSize();
	}

	@Override
	public int getTorpedoDef() {
		return inner.getTorpedoDef();
	}

	@Override
	public ShipTypeData getType() {
		return inner.getType();
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
		return inner.getWeapons();
	}

	@Override
	public int getWerft() {
		return inner.getWerft();
	}

	@Override
	public boolean hasFlag(@Nonnull ShipTypeFlag flag)
	{
		return inner.hasFlag(flag);
	}

	@Override
	public boolean isHide() {
		return inner.isHide();
	}

	@Override
	public boolean isMilitary() {
		return inner.isMilitary();
	}
	
	@Override
	public boolean isVersorger() {
		return inner.isVersorger();
	}
	
	@Override
	public int getAblativeArmor() {
		return inner.getAblativeArmor();
	}
	
	@Override
	public boolean hasSrs() {
		return inner.hasSrs();
	}

	@Override
	public int getMinCrew()
	{
		return inner.getMinCrew();
	}
	
	@Override
	public double getLostInEmpChance()
	{
		return inner.getLostInEmpChance();
	}
}
