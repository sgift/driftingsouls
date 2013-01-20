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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Statistikeintrag ueber die Positionen der Module eines Spielers (zum letzten Berechnungszeitpunkt).
 *
 */
@Entity
@Table(name="stats_module_locations")
public class StatModuleLocation {
	@Id
	int id;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id",nullable=false)
	private User user;
	
	private int item_id;
	private String locations;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public StatModuleLocation() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Statistikeintrag.
	 * @param user Der User
	 * @param itemid Die Itemid
	 * @param locations Die Positionen
	 */
	public StatModuleLocation(User user, int itemid, String locations) {
		setUser(user);
		setItemId(itemid);
		setLocations(locations);
	}

	/**
	 * Gibt die ItemId des Items zurueck.
	 * @return Die ID
	 */
	public int getItemId() {
		return item_id;
	}

	/**
	 * Setzt die ItemId des Items.
	 * @param itemid Die ID
	 */
	public final void setItemId(final int itemid) {
		this.item_id = itemid;
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
	public final void setUser(final User user) {
		this.user = user;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	/**
	 * Gibt die Orte des Items zurueck.
	 * @return Die Orte
	 */
	public String getLocations() {
		return this.locations;
	}
	
	/**
	 * Setzt die Orte fuer dieses Item.
	 * @param locations Die Orte
	 */
	public void setLocations(String locations) {
		this.locations = locations;
	}
}
