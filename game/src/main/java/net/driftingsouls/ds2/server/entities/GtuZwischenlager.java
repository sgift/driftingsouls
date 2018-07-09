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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.ships.Ship;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

/**
 * Ein Eintrag im GTU-Zwischenlager. Dabei handelt es sich
 * um eine Handelsvereinbarung zwischen zwei Spielern, bei der
 * beide Spieler eine geforderte Menge an Waren liefern muessen um die 
 * Vereinbarung zu erfuellen.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="gtu_zwischenlager")
@org.hibernate.annotations.Table(
		appliesTo = "gtu_zwischenlager",
		indexes = {@Index(name="posten", columnNames = {"posten","user1","user2"})})
public class GtuZwischenlager {
	@Id @GeneratedValue
	private int id;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="posten", nullable = false)
	@ForeignKey(name="gtu_zwischenlager_fk_ships")
	private Ship posten;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="user1", nullable = false)
	@ForeignKey(name="gtu_zwischenlager_fk_users1")
	private User user1;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="user2", nullable = false)
	@ForeignKey(name="gtu_zwischenlager_fk_users2")
	private User user2;

	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo cargo1;
	@Type(type="largeCargo")
	@Column(name="cargo1need", nullable = false)
	private Cargo cargo1Need;
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo cargo2;
	@Type(type="largeCargo")
	@Column(name="cargo2need", nullable = false)
	private Cargo cargo2Need;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public GtuZwischenlager() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param posten Der Handelsposten, auf dem gehandelt wird
	 * @param user1 Der erste Handelspartner
	 * @param user2 Der zweite Handelspartner
	 */
	public GtuZwischenlager(Ship posten, User user1, User user2) {
		this.posten = posten;
		this.user1 = user1;
		this.user2 = user2;
		cargo1 = new Cargo();
		cargo1Need = new Cargo();
		cargo2 = new Cargo();
		cargo2Need = new Cargo();
	}

	/**
	 * Gibt den aktuell User1 zur Verfuegung stehenden Waren zurueck.
	 * @return Die Waren
	 */
	public Cargo getCargo1() {
		return new Cargo(cargo1);
	}

	/**
	 * Setzt die aktuell User1 zur Verfuegung stehenden Waren.
	 * @param cargo1 Die Waren
	 */
	public void setCargo1(Cargo cargo1) {
		this.cargo1 = new Cargo(cargo1);
	}

	/**
	 * Gibt die Warenmenge zurueck, die User1 nach Erfuellung der Handelsvereinbarung zusteht.
	 * @return Die Warenmenge
	 */
	public Cargo getCargo1Need() {
		return new Cargo(cargo1Need);
	}

	/**
	 * Setzt die Warenmenge, die User1 nach Erfuellung der Handelsvereinbarung zusteht.
	 * @param cargo1Need Die Warenmenge
	 */
	public void setCargo1Need(Cargo cargo1Need) {
		this.cargo1Need = new Cargo(cargo1Need);
	}

	/**
	 * Gibt den aktuell User2 zur Verfuegung stehenden Waren zurueck.
	 * @return Die Waren
	 */
	public Cargo getCargo2() {
		return new Cargo(cargo2);
	}

	/**
	 * Setzt die aktuell User2 zur Verfuegung stehenden Waren.
	 * @param cargo2 Die Waren
	 */
	public void setCargo2(Cargo cargo2) {
		this.cargo2 = new Cargo(cargo2);
	}

	/**
	 * Gibt die Warenmenge zurueck, die User2 nach Erfuellung der Handelsvereinbarung zusteht.
	 * @return Die Warenmenge
	 */
	public Cargo getCargo2Need() {
		return new Cargo(cargo2Need);
	}

	/**
	 * Setzt die Warenmenge, die User2 nach Erfuellung der Handelsvereinbarung zusteht.
	 * @param cargo2Need Die Warenmenge
	 */
	public void setCargo2Need(Cargo cargo2Need) {
		this.cargo2Need = new Cargo(cargo2Need);
	}

	/**
	 * Gibt den Handelsposten zurueck, der als Zwischenlager fungiert.
	 * @return Der Handelsposten
	 */
	public Ship getPosten() {
		return posten;
	}

	/**
	 * Setzt den Handelsposten, der als Zwischenlanger fungiert.
	 * @param posten Der Handelsposten
	 */
	public void setPosten(Ship posten) {
		this.posten = posten;
	}

	/**
	 * Gibt den ersten Handelspartner zurueck.
	 * @return Der erste Handelspartner
	 */
	public User getUser1() {
		return user1;
	}

	/**
	 * Gibt den ersten Handelspartner zurueck.
	 * @param user1 Der erste Handelspartner
	 */
	public void setUser1(User user1) {
		this.user1 = user1;
	}

	/**
	 * Setzt den zweiten Handelspartner.
	 * @return Der zweite Handelspartner
	 */
	public User getUser2() {
		return user2;
	}

	/**
	 * Setzt den zweiten Handelspartner.
	 * @param user2 Der zweite Partner
	 */
	public void setUser2(User user2) {
		this.user2 = user2;
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
