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

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Looteintrag.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_loot")
public class ShipLoot {
	@Id @GeneratedValue
	private int id;
	@Index(name="shiploot_shiptype")
	@Column(name="shiptype", nullable = false)
	private int shipType;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="owner", nullable = false)
	@ForeignKey(name="ship_loot_fk_users1")
	private User owner;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="targetuser", nullable = false)
	@ForeignKey(name="ship_loot_fk_users2")
	private User targetUser;

	private int chance;
	@Column(nullable = false)
	private String resource;
	private int count;
	@Column(name="totalmax", nullable = false)
	private int totalMax;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ShipLoot() {
		this.resource = "";
	}

	/**
	 * Gibt die Chance des Eintrags zurueck.
	 * @return Die Chance
	 */
	public int getChance() {
		return chance;
	}

	/**
	 * Setzt die Chance des Eintrags.
	 * @param chance Die Chance
	 */
	public void setChance(int chance) {
		this.chance = chance;
	}

	/**
	 * Gibt die Resourcenmenge zurueck.
	 * @return Die Menge
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Setzt die Resourcenmenge.
	 * @param count Die Menge
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gibt den Spieler zurueck, dessen Schiffe fuer Loot zerstoert werden muessen.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * Setzt den Besitzer, dessen Schiffe fuer Loot zerstoert werden muessen.
	 * @param owner Der Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	/**
	 * Gibt die Resource zurueck.
	 * @return Die Resource
	 */
	public String getResource() {
		return resource;
	}

	/**
	 * Setzt die Resource.
	 * @param resource Die Resource
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

	/**
	 * Gibt den Schiffstyp zurueck, der fuer Loot zerstoert werden muss oder, falls
	 * der Wert negativ ist, die ID des Schiffs.
	 * @return Der Schiffstyp oder das Schiff
	 */
	public int getShipType() {
		return shipType;
	}

	/**
	 * Setzt den Schiffstyp, der fuer Loot zerstoert werden muss oder, falls
	 * der Wert negativ ist, die ID des Schiffs.
	 * @param shipType Der Schiffstyp oder das Schiff
	 */
	public void setShipType(int shipType) {
		this.shipType = shipType;
	}

	/**
	 * Gibt den Spieler zurueck, fuer den dieser Eintrag gilt (ID 0 fuer alle Spieler).
	 * @return Der Spieler fuer den diesen Eintrag gilt
	 */
	public User getTargetUser() {
		return targetUser;
	}

	/**
	 * Setzt den Spieler, fuer den dieser Eintrag gilt (ID 0 fuer alle).
	 * @param targetUser Der Spieler
	 */
	public void setTargetUser(User targetUser) {
		this.targetUser = targetUser;
	}

	/**
	 * Gibt die maximale Anzahl an Loot zurueck, die von diesen Eintrag generiert werden kann.
	 * @return Die maximale Anzahl an Loot
	 */
	public int getTotalMax() {
		return totalMax;
	}

	/**
	 * Setzt die maximale Anzahl an Loot die von diesem Eintrag generiert werden kann.
	 * @param totalMax Die maximale Anzahl an Loot
	 */
	public void setTotalMax(int totalMax) {
		this.totalMax = totalMax;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
