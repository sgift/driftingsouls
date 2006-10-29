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
package net.driftingsouls.ds2.server.config;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Eine einfache Waffe in DS. Basisklasse fuer alle Waffen
 * @author Christopher Jung
 *
 */
public class Weapon {
	/**
	 * Waffenflags
	 */
	public enum Flags {
		/**
		 * Spezial-Waffe (eigene Auswahlbox unter sonstiges)
		 */
		SPECIAL(1),
		/**
		 * Nach dem Abfeuern das Schiff zerstoeren
		 */
		DESTROY_AFTER(2),
		/**
		 * Ammo-Auswahl fuer diese Waffe zulassen
		 */
		AMMO_SELECT(4),
		/**
		 * Area-Damage ueber die Distanz nicht reduzieren
		 */
		AD_FULL(8),
		/**
		 * Weitreichende Waffen koennen aus der zweiten Reihe heraus abgefeuert werden
		 */
		LONG_RANGE(16);	
		
		private int bit;
		private Flags(int bit) {
			this.bit = bit;
		}
		
		public int getBits() {
			return this.bit;
		}
		
	}
	protected Weapon() {
		throw new RuntimeException("STUB");
	}
	
	public String getName() {
		throw new RuntimeException("STUB");
	}
	
	public int getAPCost() {
		throw new RuntimeException("STUB");
	}
	
	public int getECost() {
		throw new RuntimeException("STUB");
	}
	
	public int getBaseDamage(SQLResultRow ownShipType) {
		throw new RuntimeException("STUB");
	}
	
	public int getBaseDamageModifier(SQLResultRow enemyShipType) {
		throw new RuntimeException("STUB");
	}
	
	public int getShieldDamage(SQLResultRow ownShipType) {
		throw new RuntimeException("STUB");
	}
	
	public int getSubDamage(SQLResultRow ownShipType) {
		throw new RuntimeException("STUB");
	}
	
	public int getDefTrefferWS() {
		throw new RuntimeException("STUB");
	}
	
	public int getDefSmallTrefferWS() {
		throw new RuntimeException("STUB");
	}
	
	public int getTorpTrefferWS() {
		throw new RuntimeException("STUB");
	}
	
	public int getDefSubWS() {
		throw new RuntimeException("STUB");
	}
	
	public boolean calcShipTypes(SQLResultRow ownShipType, SQLResultRow enemyShipType) {
		throw new RuntimeException("STUB");
	}
	
	public String getAmmoType() {
		throw new RuntimeException("STUB");
	}
	
	public int getSingleShots() {
		throw new RuntimeException("STUB");
	}
	
	public int getAreaDamage() {
		throw new RuntimeException("STUB");
	}
	
	public boolean getDestroyable() {
		throw new RuntimeException("STUB");
	}
	
	public boolean hasFlag(Flags flag) {
		throw new RuntimeException("STUB");
	}
}
