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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.framework.Common;
import org.hibernate.annotations.ForeignKey;

/**
 * Ein Handelseintrag.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="handel")
public class Handel {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="who", nullable = false)
	@ForeignKey(name="handel_fk_users")
	private User who;
	private long time;
	// Hinweis: Hier wird kein Cargo-Objekt verwendet, da
	// sucht und bietet auch "-1" enthalten kann fuer "irgendwas"
	@Column(nullable = false)
	@Lob
	private String sucht;
	@Column(nullable = false)
	@Lob
	private String bietet;
	@Column(name="comm", nullable = false)
	@Lob
	private String kommentar;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Handel() {
		// EMPTY
	}
	
	/**
	 * <p>Konstruktor.</p>
	 * Erstellt einen neuen Handelseintrag fuer den aktuellen Zeitpunkt.
	 * @param who Der Besitzer des Eintrags
	 */
	public Handel(User who) {
		this.who = who;
		this.time = Common.time();
		this.sucht = "-1";
		this.bietet = "-1";
		this.kommentar = "";
	}

	/**
	 * Gibt die gebotenen Waren zurueck.
	 * @return Die gebotenen Waren
	 */
	public String getBietet() {
		return bietet;
	}

	/**
	 * Setzt die gebotenen Waren.
	 * @param bietet Die Waren
	 */
	public void setBietet(String bietet) {
		this.bietet = bietet;
	}

	/**
	 * Gibt den Kommentar zurueck.
	 * @return der Kommentar
	 */
	public String getKommentar() {
		return kommentar;
	}

	/**
	 * Setzt den Kommentar.
	 * @param kommentar Der Kommentar
	 */
	public void setKommentar(String kommentar) {
		this.kommentar = kommentar;
	}

	/**
	 * Gibt die gesuchten Waren zurueck.
	 * @return Die gesuchten Waren
	 */
	public String getSucht() {
		return sucht;
	}

	/**
	 * Setzt die gesuchten Waren.
	 * @param sucht Die gesuchten Waren
	 */
	public void setSucht(String sucht) {
		this.sucht = sucht;
	}

	/**
	 * Gibt den Zeitpunkt zurueck, an dem das Angebot erstellt wurde.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt, an dem das Angebot erstellt wurde.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den Besitzer des Angebots zurueck.
	 * @return Der Besitzer
	 */
	public User getWho() {
		return who;
	}

	/**
	 * Setzt den Besitzer des Angebots.
	 * @param who Der Besitzer
	 */
	public void setWho(User who) {
		this.who = who;
	}

	/**
	 * Gibt die ID des Handelseintrags zurueck.
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
