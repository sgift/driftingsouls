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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Angebot einer Fraktion.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="factions_angebote")
public class FraktionsAngebot
{
	@Id @GeneratedValue
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(name="faction", nullable = false)
	@ForeignKey(name="factions_angebote_fk_user")
	private User faction;
	@Column(nullable = false)
	private String title;
	@Column(nullable = false)
	private String image;
	@Lob
	@Column(nullable = false)
	private String description;

	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public FraktionsAngebot() {
		this.title = "";
		this.image = "";
		this.description = "";
	}
	
	/**
	 * Konstruktor.
	 * @param faction Die Fraktion, der das Angebot gehoert
	 * @param title Der Titel des Angebots
	 */
	public FraktionsAngebot(User faction, String title) {
		setFaction(faction);
		setTitle(title);
	}

	/**
	 * Gibt die Angebotsbeschreibung zurueck.
	 * @return Die Angebotsbeschreibung
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setzt die Angebotsbeschreibung.
	 * @param description Die Angebotsbeschreibung
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gibt die ID der Fraktion zurueck.
	 * @return Die ID der Fraktion
	 */
	public User getFaction() {
		return faction;
	}

	/**
	 * Setzt die Fraktion der das Angebot gehoert.
	 * @param faction Die ID der Fraktion
	 */
	public final void setFaction(User faction) {
		this.faction = faction;
	}

	/**
	 * Gibt das zum Angebot gehoerende Bild zurueck.
	 * @return Das Bild
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Setzt das zum Angebot gehoerende Bild.
	 * @param image Das Bild
	 */
	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * Gibt den Titel des Angebots zurueck.
	 * @return Der Titel
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Setzt den Titel des Angebots.
	 * @param title Der Titel
	 */
	public final void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gibt die ID des Angebots zurueck.
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
