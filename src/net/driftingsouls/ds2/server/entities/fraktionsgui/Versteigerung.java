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
package net.driftingsouls.ds2.server.entities.fraktionsgui;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Basisklasse fuer Versteigerungen.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="versteigerungen")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="mtype", discriminatorType = DiscriminatorType.INTEGER)
public abstract class Versteigerung {
	@Id @GeneratedValue
	private int id;
	private int tick;
	private long preis;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="bieter", nullable = false)
	@ForeignKey(name="versteigerungen_fk_users")
	private User bieter;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="owner", nullable = false)
	@ForeignKey(name="versteigerungen_fk_users2")
	private User owner;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected Versteigerung() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Versteigerung.
	 * @param owner Der Besitzer und zugleich default-Bieter
	 * @param price Der Startpreis
	 */
	public Versteigerung(User owner, long price) {
		this.owner = owner;
		this.bieter = owner;
		this.preis = price;
	}

	/**
	 * Gibt den Bieter zurueck.
	 * @return Der Bieter
	 */
	public User getBieter() {
		return bieter;
	}

	/**
	 * Setzt den Bieter.
	 * @param bieter Der Bieter
	 */
	public void setBieter(User bieter) {
		this.bieter = bieter;
	}

	/**
	 * Gibt den Besitzer der Versteigerung zurueck.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * Setzt den Besitzer der Versteigerung.
	 * @param owner Der Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	/**
	 * Gibt den aktuellen Preis zurueck.
	 * @return Der Preis
	 */
	public long getPreis() {
		return preis;
	}

	/**
	 * Setzt den aktuellen Preis.
	 * @param preis Der Preis
	 */
	public void setPreis(long preis) {
		this.preis = preis;
	}

	/**
	 * Gibt den Tick zurueck, an dem die Versteigerung endet.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, an dem die Versteigerung endet.
	 * @param tick Der Tick
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die ID der Versteigerung zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gibt den Namen des zu versteigernden Objekts zurueck.
	 * @return Der Name
	 */
	public abstract String getObjectName();
	
	/**
	 * Gibt das Bild des zu versteigernden Objekts zurueck.
	 * @return Das Bild
	 */
	public abstract String getObjectPicture();
	
	/**
	 * Gibt eine URL zur Anzeige weiterer Details zurueck.
	 * @return Die URL
	 */
	public abstract String getObjectUrl();
	
	/**
	 * Gibt die Anzahl an zu versteigernden Objekten zurueck.
	 * @return Die Anzahl
	 */
	public abstract long getObjectCount();
	
	/**
	 * Gibt zurueck, ob das Bild des Objekts fest auf 50x50 Pixel gesetzt werden soll.
	 * Andernfalls wird das Bild in der urspruenglichen Groesse verwendet.
	 * @return <code>true</code>, falls das Bild als 50x50 Pixel gossses Bild verwendet werden soll
	 */
	public abstract boolean isObjectFixedImageSize();

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
