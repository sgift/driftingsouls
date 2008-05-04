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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Angebot einer Fraktion
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="factions_angebote")
public class FactionOffer {
	@Id @GeneratedValue
	private int id;
	private int faction;
	private String title;
	private String image;
	private String description;

	@Version
	private int version;
	
	/**
	 * Konstruktor
	 *
	 */
	public FactionOffer() {
		// EMPTY
	}
	
	/**
	 * Konstruktor
	 * @param faction Die Fraktion, der das Angebot gehoert
	 * @param title Der Titel des Angebots
	 */
	public FactionOffer(int faction, String title) {
		setFaction(faction);
		setTitle(title);
	}

	/**
	 * Gibt die Angebotsbeschreibung zurueck
	 * @return Die Angebotsbeschreibung
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setzt die Angebotsbeschreibung
	 * @param description Die Angebotsbeschreibung
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gibt die ID der Fraktion zurueck
	 * @return Die ID der Fraktion
	 */
	public int getFaction() {
		return faction;
	}

	/**
	 * Setzt die Fraktion der das Angebot gehoert
	 * @param faction Die ID der Fraktion
	 */
	public final void setFaction(int faction) {
		this.faction = faction;
	}

	/**
	 * Gibt das zum Angebot gehoerende Bild zurueck
	 * @return Das Bild
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Setzt das zum Angebot gehoerende Bild
	 * @param image Das Bild
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * Gibt den Titel des Angebots zurueck
	 * @return Der Titel
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Setzt den Titel des Angebots
	 * @param title Der Titel
	 */
	public final void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gibt die ID des Angebots zurueck
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Versionsnummer zurueck
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
