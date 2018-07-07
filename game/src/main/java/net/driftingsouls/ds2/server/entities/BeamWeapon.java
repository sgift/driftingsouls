/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.ships.AbstractShipTypeDataWrapper;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein Strahlengeschuetz in DS.
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("BeamWeapon")
public class BeamWeapon extends Weapon
{
	/**
	 * Konstruktor.
	 */
	protected BeamWeapon()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID der Waffe
	 */
	public BeamWeapon(String id)
	{
		super(id);
	}

	@Override
	public ShipTypeData calcEnemyShipType(ShipTypeData ownShipType, ShipTypeData enemyShipType) {
		ShipTypeData enemy = super.calcEnemyShipType(ownShipType, enemyShipType);
		
		if( (enemyShipType.getSize() > ShipType.SMALL_SHIP_MAXSIZE) && (enemyShipType.getSize() < ownShipType.getSize() - 6 ) ) {
			enemy = new ShipTypeWrapper(enemy, ownShipType.getSize() - 6);
		}
		return enemy;
	}
	
	private static class ShipTypeWrapper extends AbstractShipTypeDataWrapper {
		private int size;

		ShipTypeWrapper(ShipTypeData inner, int size) {
			super(inner);
			this.size = size;
		}

		@Override
		public int getSize() {
			return this.size;
		}
	}
}
