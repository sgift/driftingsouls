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

import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.framework.Common;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

/**
 * <p>Repraesentiert einen Eintrag im Log der durchgefuehrten Ueberweisungen
 * zwischen zwei Spielern.</p>
 * <p>Unterschieden wird dabei zwischen echten Ueberweisungen und Dummy/Fake-
 * Ueberweisungen, bei denen zwar der Empfaenger Geld bekommen, der ueberweisende
 * Spieler jedoch kein Geld abgebucht bekommen hat.</p>
 * <p>Zudem gibt es eine Einteilung bzgl. des Automatisierungsgrads unter
 * dem die Ueberweisung zu Stande kam.</p>
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="user_moneytransfer")
@org.hibernate.annotations.Table(
	appliesTo = "user_moneytransfer",
	indexes = {@Index(name="from_idx", columnNames = {"from_id", "to_id"})}
)
public class UserMoneyTransfer {
	/**
	 * Der Automatisierungsgrad unter dem die Ueberweisung zu Stande kam.
	 */
	public enum Transfer {
		/**
		 * Geldtransfer - Der Transfer ist manuell vom Spieler durchgefuerht worden.
		 */
		NORMAL,
		/**
		 * Geldtransfer - Der Transfer ist in direkter Folge einer Spieleraktion ausgefuehrt worden.
		 */
		SEMIAUTO,
		/**
		 * Geldtransfer - Der Transfer ist automatisch erfolgt.
		 */
		AUTO
	}
	
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(nullable=false)
	@ForeignKey(name="user_moneytransfer_fk_users1")
	private User from;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(nullable=false)
	@ForeignKey(name="user_moneytransfer_fk_users2")
	private User to;
	@Index(name="time")
	private long time;
	@Column(nullable = false)
	private BigInteger count;
	@Lob
	@Column(nullable = false)
	private String text;
	private int fake;
	private int type;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public UserMoneyTransfer() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Transfereintrag.
	 * @param from Der ueberweisende Benutzer
	 * @param to Der Empfaenger der Ueberweisung
	 * @param count Der ueberweisene Betrag
	 * @param text Der Erlaeutungstext
	 */
	public UserMoneyTransfer(User from, User to, BigInteger count, String text) {
		this.from = from;
		this.to = to;
		this.time = Common.time();
		this.count = count;
		this.text = text;
		this.fake = 0;
		this.type = Transfer.NORMAL.ordinal();
	}

	/**
	 * Gibt die ueberwiesene Menge zurueck.
	 * @return Der Geldbetrag
	 */
	public BigInteger getCount() {
		return count;
	}

	/**
	 * Setzt die ueberwiesene Menge.
	 * @param count Der Geldbetrag
	 */
	public void setCount(BigInteger count) {
		this.count = count;
	}

	/**
	 * Gibt zurueck, ob es sich um eine Dummy-Ueberweisung handelt,
	 * bei der beim ueberweisenden kein Geld abgebucht wurde.
	 * @return <code>true</code>, falls es eine Dummy-Ueberweisung ist
	 */
	public boolean isFake() {
		return fake != 0;
	}

	/**
	 * Setzt, ob es sich um eine Dummy-Ueberweisung handelt, 
	 * bei der beim ueberweisenden kein Geld abgebucht wurde.
	 * @param fake <code>true</code>, falls es eine Dummy-Ueberweisung ist
	 */
	public void setFake(boolean fake) {
		this.fake = fake ? 1 : 0;
	}

	/**
	 * Gibt den ueberweisenden Spieler zurueck.
	 * @return Der ueberweisende Spieler
	 */
	public User getFrom() {
		return from;
	}

	/**
	 * Setzt den ueberweisenden Spieler.
	 * @param from Der ueberweisende Spieler
	 */
	public void setFrom(User from) {
		this.from = from;
	}

	/**
	 * Gibt den Hinweistext zur Ueberweisung zurueck.
	 * @return Der Text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt den Hinweistext zur Ueberweisung.
	 * @param text Der Hinweistext
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gibt den Zeitpunkt der Ueberweisung zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt der Ueberweisung.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den Empfaenger der Ueberweisung zurueck.
	 * @return Der Empfaenger
	 */
	public User getTo() {
		return to;
	}

	/**
	 * Setzt den Empfaenger der Ueberweisung.
	 * @param to Der Empfaenger
	 */
	public void setTo(User to) {
		this.to = to;
	}

	/**
	 * Gibt den Typ der Ueberweisung zurueck.
	 * @return Der Typ
	 */
	public Transfer getType() {
		return Transfer.values()[this.type];
	}

	/**
	 * Setzt den Typ der Ueberweisung.
	 * @param type Der Typ
	 */
	public void setType(Transfer type) {
		this.type = type.ordinal();
	}

	/**
	 * Gibt die ID der Ueberweisung zurueck.
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
