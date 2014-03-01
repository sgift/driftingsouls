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

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.Index;

/**
 * Ein Eintrag im Shop einer Fraktion.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="factions_shop_entries")
public class FactionShopEntry {
	public enum Type
	{
		ITEM,
		SHIP,
		TRANSPORT
	}

	@Id @GeneratedValue
	private int id;
	@Column(name="faction_id", nullable = false)
	@Index(name="faction_id")
	private int faction;
	@Enumerated
	@Column(nullable = false)
	private Type type;
	@Column(nullable = false)
	private String resource;
	private long price;
	private long lpKosten;
	private int availability;
    @Column(name="min_rank", nullable = false)
    private int minRank;

	@OneToMany(mappedBy="shopEntry",cascade = CascadeType.ALL)
	private Set<FactionShopOrder> orders;

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
	public FactionShopEntry(int faction, Type type, String resource) {
		this.faction = faction;
		this.type = type;
		this.resource = resource;
		this.orders = new HashSet<FactionShopOrder>();
	}

	/**
	 * Gibt alle zu diesem Shopeintrag vorhandenen Bestellungen
	 * zurueck. Dies umfasst auch alle bereits abgeschlossenen
	 * Bestellungen.
	 * @return Die Bestellungen
	 */
	public Set<FactionShopOrder> getBestellungen()
	{
		return this.orders;
	}

	/**
	 * Gibt die Anzahl aller noch offenen Bestellungen zurueck.
	 * @return Die Anazhl der offenen Bestellungen
	 */
	public int getAnzahlOffeneBestellungen()
	{
		int counter = 0;
		for (FactionShopOrder order : this.orders)
		{
			if( order.getStatus() < 4 )
			{
				counter++;
			}
		}
		return counter;
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
	 * Gibt die LP-Kosten fuer das Produkt zurueck.
	 * @return Die LP-Kosten
	 */
	public long getLpKosten()
	{
		return lpKosten;
	}

	/**
	 * Setzt die LP-Kosten fuer das Produkt.
	 * @param lpKosten Die LP-Kosten
	 */
	public void setLpKosten(long lpKosten)
	{
		this.lpKosten = lpKosten;
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
	public Type getType() {
		return type;
	}

	/**
	 * Setzt den Typ des Produkts.
	 * @param type Der Typ
	 */
	public final void setType(final Type type) {
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
	 * Gibt den Mindestrang bei dem Shopbesitzer zurueck, den ein Spieler fuer den
	 * Kauf dieser Ware haben muss.
	 * @return Der Rang
	 */
	public int getMinRank()
	{
		return this.minRank;
	}

	/**
	 * Setzt den Mindestrang bei dem Shopbesitzer, den ein Spieler fuer den
	 * Kauf dieser Ware haben muss.
	 * @param minRank Der Rang
	 */
	public void setMinRank(int minRank)
	{
		this.minRank = minRank;
	}

    /**
     * Gibt zurueck, ob der angegebene Spieler die Ware kaufen kann.
     * @param buyer Der Spieler, der die Ware kaufen moechte
     * @return <code>true</code>, wenn der Spieler die Ware kaufen kann, <code>false</code> ansonsten.
     */
    public boolean canBuy(User buyer)
    {
    	if( buyer.getId() == this.faction )
    	{
    		return true;
    	}
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
