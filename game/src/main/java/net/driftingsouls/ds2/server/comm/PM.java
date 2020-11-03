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
package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

/**
 * <p>Repraesentiert eine PM in der Datenbank.</p>
 * <p>Eine PM ist immer mit einem Sender sowie Empfaenger verbunden.
 * Zudem befindet sie sich in einem Ordner, wobei 0 der Hauptordner ist.
 * Eine PM besitzt zudem einen Gelesen-Status. Ist dieser 0 so wurde die Nachricht noch
 * nicht gelesen. 1 kennzeichnet sie als gelesen. Wenn der Wert 2 oder hoeher ist
 * wurde die PM geloescht. Ihr gelesen-Status steigt dann jeden Tick um 1
 * bis ein Schwellenwert ueberschritten und die PM endgueltig geloescht wird.</p>
 * <p>Zudem steht ein Kommentarfeld fuer Anmerkungen sowie eine Reihe von Flags zur
 * Verfuegung.</p>
 * @author Christopher Jung
 * @author Christian Peltz
 *
 */
@Entity
@Table(name="transmissionen")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@org.hibernate.annotations.Table(
	appliesTo = "transmissionen",
	indexes = {@Index(name="empfaenger", columnNames = {"empfaenger", "gelesen"})}
)
public class PM {
	/**
	 * Die PM hat einen Admin-Hintergrund.
	 */
	public static final int FLAGS_ADMIN = 1;
	/**
	 * Es handelt sich um eine automatisch versendete PM.
	 */
	public static final int FLAGS_AUTOMATIC = 2;
	/**
	 * Die PM wurde durch den Tick versendet.
	 */
	public static final int FLAGS_TICK = 4;
	/**
	 * Die PM hat einen rassenspezifischen Hintergrund.
	 */
	public static final int FLAGS_OFFICIAL = 8;	// Spezieller (fraktions/rassenspezifischer) Hintergrund
	/**
	 * Die PM muss gelesen werden bevor sie geloescht werden kann.
	 */
	public static final int FLAGS_IMPORTANT = 16;	// Muss "absichtlich" gelesen werden

	/**
	 * Der PM-Empfaenger des Taskmanagers.
	 */
	public static final int TASK = Integer.MIN_VALUE;

	@Version
	private int version;

	@Transient
	private static final Log log = LogFactory.getLog(PM.class);

	@Id @GeneratedValue
	private int id;
	private int gelesen;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="sender", nullable=false)
	@ForeignKey(name="transmissionen_fk_users1")
	private User sender;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="empfaenger", nullable=false)
	@ForeignKey(name="transmissionen_fk_users2")
	private User empfaenger;
	@Column(nullable = false)
	private String title;
	private long time;
	// Kein Join auf Ordner, da der Hauptordner 0 nicht in der DB existiert
	private int ordner;
	private int flags;
	@Lob
	@Column(nullable = false)
	private String inhalt;
	@Lob
	@Column(nullable = false)
	private String kommentar;

	/**
	 * Konstruktor.
	 *
	 */
	public PM() {
		// EMPTY
	}

	/**
	 * Erstellt eine neue PM.
	 * @param sender Der Sender der PM
	 * @param empfaenger Der Empfaenger
	 * @param title Der Titel
	 * @param inhalt Der Inhalt
	 */
	public PM(User sender, User empfaenger, String title, String inhalt) {
		this.gelesen = 0;
		this.sender = sender;
		this.empfaenger = empfaenger;
		this.title = title;
		this.time = Common.time();
		this.ordner = 0;
		this.flags = 0;
		this.inhalt = inhalt;
		this.kommentar = "";
	}

	/**
	 * Gibt den Empfaenger zurueck.
	 * @return Der Empfaenger
	 */
	public User getEmpfaenger() {
		return empfaenger;
	}

	/**
	 * Setzt den Empfaenger.
	 * @param empfaenger Der Empfaenger
	 */
	public void setEmpfaenger(User empfaenger) {
		this.empfaenger = empfaenger;
	}

	/**
	 * Gibt die Flags zurueck.
	 * @return Die Flags
	 */
	public int getFlags() {
		return flags;
	}

	/**
	 * Prueft, ob die Nachricht das angegebene Flag hat.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Nachricht das Flag hat
	 */
	public boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	 * Setzt die Flags der Nachricht.
	 * @param flags Die Flags
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Gibt den Gelesen-Status der Nachricht zurueck.
	 * @return Der Gelesen-Status
	 */
	public int getGelesen() {
		return gelesen;
	}

	/**
	 * Setzt den Gelesen-Status der Nachricht.
	 * @param gelesen Der Gelesen-Status
	 */
	public void setGelesen(int gelesen) {
		this.gelesen = gelesen;
	}

	/**
	 * Gibt den Inhalt der Nachricht zurueck.
	 * @return Der Inhalt
	 */
	public String getInhalt() {
		return inhalt;
	}

	/**
	 * Setzt den Inahlt der Nachricht.
	 * @param inhalt Der Inhalt
	 */
	public void setInhalt(String inhalt) {
		this.inhalt = inhalt;
	}

	/**
	 * Gibt den Kommentar/die Anmerkung zur Nachricht zurueck.
	 * @return Der Kommentar
	 */
	public String getKommentar() {
		return kommentar;
	}

	/**
	 * Setzt den Kommentar/die Anmerkung zur Nachricht.
	 * @param kommentar Der Kommentar
	 */
	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	/**
	 * Gibt den Ordner zurueck, in dem sich die Nachricht befindet.
	 * @return Der Ordner
	 */
	public int getOrdner() {
		return ordner;
	}

	/**
	 * Setzt den Ordner, in dem sich die Nachricht befindet.
	 * @param ordner Der Ordner
	 */
	public void setOrdner(int ordner) {
		this.ordner = ordner;
	}

	/**
	 * Gibt den Sender der Nachricht zurueck.
	 * @return Der Sender
	 */
	public User getSender() {
		return sender;
	}

	/**
	 * Setzt den Sender der Nachricht.
	 * @param sender Der Sender
	 */
	public void setSender(User sender) {
		this.sender = sender;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem die Nachricht erstellt wurde.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt, an dem die Nachricht erstellt wurde.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den Titel der Nachricht zurueck.
	 * @return Der Titel
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Setzt den Titel der Nachricht.
	 * @param title Der Titel
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gibt die ID der Nachricht zurueck.
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
