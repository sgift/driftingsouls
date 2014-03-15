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
package net.driftingsouls.ds2.server.entities.statistik;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;

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
 * Statistik fuer die Aufenthaltsorte eines Items bei einem Spieler.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stats_module_locations")
public class StatItemLocations {
	@Id @GeneratedValue
	private int id;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="user_id", nullable=false)
	@ForeignKey(name="stats_module_locations_fk_user_id")
	private User user;

	@Column(name="item_id", nullable = false)
	private int itemId;

	@Column(nullable = false)
	private String locations;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public StatItemLocations() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Statistikeintrag.
	 * @param user Der Spieler
	 * @param itemid Das Item
	 * @param locations Die Aufenthaltsorte
	 */
	public StatItemLocations(User user, int itemid, String locations) {
		this.user = user;
		this.itemId = itemid;
		this.locations = locations;
	}

	/**
	 * Gibt die ID des Items zurueck.
	 * @return DIe ID
	 */
	public int getItemId() {
		return itemId;
	}

	/**
	 * Setzt die ID des Items.
	 * @param itemId Die ID
	 */
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	/**
	 * Gibt die Aufenthaltsorte des Items beim Spieler zurueck.
	 * @return Die Aufenthaltsorte
	 */
	public String getLocations() {
		return locations;
	}

	/**
	 * Setzt die Aufenthaltsorte des Items beim Spieler.
	 * @param locations Die Aufenthaltsorte
	 */
	public void setLocations(String locations) {
		this.locations = locations;
	}

	/**
	 * Gibt den Spieler zurueck.
	 * @return Der Spieler
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Spieler.
	 * @param user Der Spieler
	 */
	public void setUser(User user) {
		this.user = user;
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
