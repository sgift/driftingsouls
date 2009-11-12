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
package net.driftingsouls.ds2.server.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Die Munition.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ammo")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Ammo {
	/**
	 * Ammoflags.
	 */
	public enum Flags {
		/**
		 * Area-Damage ueber die Distanz nicht reduzieren.
		 */
		AD_FULL(1),
		
		/**
		 * Schaden der Munition wird durch Panzerung dividiert.
		 */
		ARMOR_REDUX(2);
		
		private int bit;
		private Flags(int bit) {
			this.bit = bit;
		}
		
		/**
		 * Gibt das zum Flag gehoerende Bitmuster zurueck.
		 * @return Das Bitmuster
		 */
		public int getBits() {
			return this.bit;
		}
		
	}
	
	@Id @GeneratedValue
	private int id;
	private String name;
	private String type;
	private int damage;
	@Column(name="shielddamage")
	private int shieldDamage;
	@Column(name="subdamage")
	private int subDamage;
	@Column(name="trefferws")
	private int trefferWS;
	@Column(name="smalltrefferws")
	private int smallTrefferWS;
	@Column(name="torptrefferws")
	private int torpTrefferWS;
	@Column(name="subws")
	private int subWS;
	@Column(name="shotspershot")
	private int shotsPerShot;
	@Column(name="areadamage")
	private int areaDamage;
	private double destroyable;
	private int flags;
	private int itemid;
	private String picture;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Ammo() {
		// EMPTY
	}

	/**
	 * Gibt die Areadamage zurueck.
	 * @return Die Areadamage
	 */
	public int getAreaDamage() {
		return areaDamage;
	}

	/**
	 * Gibt den Huellenschaden zurueck.
	 * @return Der Huellenschaden
	 */
	public int getDamage() {
		return damage;
	}

	/**
	 * Gibt den Namen zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Gibt die ItemID zurueck.
	 * @return Die ItemID
	 */
	public int getItemId()
	{
		return this.itemid;
	}
	
	/**
	 * Gibt den Zerstoerbarkeitsfaktor zurueck.
	 * @return Der Zerstoerbarkeitsfaktor
	 */
	public double getDestroyable() {
		return destroyable;
	}

	/**
	 * Gibt die Flags der Ammo zurueck.
	 * @return Die Flags
	 */
	public int getFlags() {
		return flags;
	}
	
	/**
	 * Gibt zurueck, ob die Ammo das angegebene Flag hat.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Ammo das Flag hat
	 */
	public boolean hasFlag(Flags flag) {
		return (this.flags & flag.getBits()) != 0;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt das Bild der Ammo zurueck.
	 * @return Das Bild
	 */
	public String getPicture() {
		return picture;
	}

	/**
	 * Gibt den Schildschaden der Ammo zurueck.
	 * @return Der Schildschaden
	 */
	public int getShieldDamage() {
		return shieldDamage;
	}

	/**
	 * Gibt die Einzelschuesse pro Schuss zurueck.
	 * @return Die Anzahl der Einzelschuesse
	 */
	public int getShotsPerShot() {
		return shotsPerShot;
	}

	/**
	 * Gibt die TrefferWS gegen kleine Objekte zurueck.
	 * @return Die TrefferWS gegen keine Objekte
	 */
	public int getSmallTrefferWS() {
		return smallTrefferWS;
	}

	/**
	 * Gibt den Subsystemschaden zurueck.
	 * @return Der Subsystemschaden
	 */
	public int getSubDamage() {
		return subDamage;
	}

	/**
	 * Gibt die Trefferwahrscheinlichkeit gegen Subsysteme zurueck.
	 * @return Die Trefferwahrscheinlichkeit gegen Subsysteme
	 */
	public int getSubWS() {
		return subWS;
	}

	/**
	 * Gibt die Trefferwahrscheinlichkeit gegen Torpedos zurueck.
	 * @return Die TrefferWS gegen Torpedos
	 */
	public int getTorpTrefferWS() {
		return torpTrefferWS;
	}

	/**
	 * Gibt die Trefferwahrscheinlichkeit zurueck.
	 * @return Die Trefferwahrscheinlichkeit
	 */
	public int getTrefferWS() {
		return trefferWS;
	}

	/**
	 * Gibt den Ammotyp zurueck.
	 * @return Der Ammotyp
	 */
	public String getType() {
		return type;
	}

	/**
	 * Setzt die Ausdehnung des Flaechenschadens.
	 * @param areaDamage Die Areadamage
	 */
	public void setAreaDamage(int areaDamage) {
		this.areaDamage = areaDamage;
	}

	/**
	 * Setzt den Namen der Munition.
	 * @param name Der Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Setzt den Schaden.
	 * @param damage Der Schaden
	 */
	public void setDamage(int damage) {
		this.damage = damage;
	}

	/**
	 * Setzt den Faktor fuer die Zerstoerbarkeit vor dem Aufschlagen.
	 * @param destroyable Die Zerstoerbarkeit
	 */
	public void setDestroyable(double destroyable)
	{
		this.destroyable = destroyable;
	}

	/**
	 * Setzt die Flags der Munition.
	 * @param flags Die Flags
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Setzt den Pfad zum Bild der Munition.
	 * @param picture Der Pfad
	 */
	public void setPicture(String picture) {
		this.picture = picture;
	}

	/**
	 * Setzt den Schaden bei Schilden.
	 * @param shieldDamage Der Schaden
	 */
	public void setShieldDamage(int shieldDamage) {
		this.shieldDamage = shieldDamage;
	}

	/**
	 * Setzt die Anzahl an Geschossen pro abgefeuertem Schuss.
	 * @param shotsPerShot Die Anzahl
	 */
	public void setShotsPerShot(int shotsPerShot) {
		this.shotsPerShot = shotsPerShot;
	}

	/**
	 * Setzt die Treffer-WS gegen kleine Schiffe.
	 * @param smallTrefferWS Die Treffer-WS
	 */
	public void setSmallTrefferWS(int smallTrefferWS) {
		this.smallTrefferWS = smallTrefferWS;
	}

	/**
	 * Setzt den Schaden an Subsystemen.
	 * @param subDamage Der Schaden
	 */
	public void setSubDamage(int subDamage) {
		this.subDamage = subDamage;
	}

	/**
	 * Setzt die Treffer-WS auf Subsystemee.
	 * @param subWS Die Treffer-WS
	 */
	public void setSubWS(int subWS) {
		this.subWS = subWS;
	}

	/**
	 * Setzt die Treffer-WS gegen anfliegende Torpedos.
	 * @param torpTrefferWS Die Treffer-WS
	 */
	public void setTorpTrefferWS(int torpTrefferWS) {
		this.torpTrefferWS = torpTrefferWS;
	}

	/**
	 * Setzt die Treffer-WS gegen normale Schiffe.
	 * @param trefferWS Die Treffer-WS
	 */
	public void setTrefferWS(int trefferWS) {
		this.trefferWS = trefferWS;
	}

	/**
	 * Setzt den Slot mit dem diese Munition verschossen werden kann.
	 * @param type Der Slot
	 */
	public void setType(String type) {
		this.type = type;
	}
}
