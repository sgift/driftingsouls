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

import net.driftingsouls.ds2.server.framework.ContextMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein Eintrag im Shop einer Fraktion.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="factions_shop_entries")
public class FactionShopEntry {
	@Id @GeneratedValue
	private int id;
	@Column(name="faction_id")
	private int faction;
	private int type;
	private String resource;
	private long price;
	private int availability;
    @Column(name="min_rank")
    private int minRank;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public FactionShopEntry() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param faction Die Fraktion, zu deren Shop der Eintrag gehoert
	 * @param type Der Typ des Eintrags
	 * @param resource Die Eintragsdaten
	 */
	public FactionShopEntry(int faction, int type, String resource) {
		setFaction(faction);
		setType(type);
		setResource(resource);
	}

	/**
	 * Gibt die Verfuegbarkeit des Produkts zurueck.
	 * @return Die Verfuegbarkeit
	 */
	public int getAvailability() {
		return availability;
	}

	/**
	 * Setzt die Verfuegbarkeit des Produkts.
	 * @param availability Die Verfuegbarkeit
	 */
	public void setAvailability(int availability) {
		this.availability = availability;
	}

	/**
	 * Gibt die Fraktion zurueck, der der Eintrag gehoert.
	 * @return Die Fraktions-ID
	 */
	public int getFaction() {
		return faction;
	}

	/**
	 * Setzt die Fraktion, der der Eintrag gehoert.
	 * @param faction Die Fraktions-ID
	 */
	public final void setFaction(final int faction) {
		this.faction = faction;
	}

	/**
	 * Gibt den Preis des Produkts in RE zurueck.
	 * @return Der Preis
	 */
	public long getPrice() {
		return price;
	}

	/**
	 * Setzt den Preis des Produkts in RE.
	 * @param price Der neue Preis
	 */
	public void setPrice(long price) {
		this.price = price;
	}

	/**
	 * Gibt die Produktdaten zurueck.
	 * @return Die Produktdaten
	 */
	public String getResource() {
		return resource;
	}

	/**
	 * Setzt die Produktdaten.
	 * @param resource Die Produktdaten
	 */
	public final void setResource(final String resource) {
		this.resource = resource;
	}

	/**
	 * Gibt den Typ des Produkts zurueck.
	 * @return Der Typ
	 */
	public int getType() {
		return type;
	}

	/**
	 * Setzt den Typ des Produkts.
	 * @param type Der Typ
	 */
	public final void setType(final int type) {
		this.type = type;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

    /**
     * @return <code>true</code>, wenn der Spieler die Ware kaufen kann, <code>false</code> ansonsten.
     */
    public boolean canBuy(User buyer)
    {
        org.hibernate.Session db = ContextMap.getContext().getDB();
        User owner = (User)db.get(User.class, this.faction);
        UserRank rank = buyer.getRank(owner);
        return rank.getRank() >= minRank;
    }

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
