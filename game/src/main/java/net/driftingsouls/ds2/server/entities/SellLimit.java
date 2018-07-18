/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

/**
 * A sell limit for a single resource.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="tradepost_sell", uniqueConstraints = {@UniqueConstraint(name="ship_itemid_uniq", columnNames = {"shipid", "resourceid"})})
public class SellLimit {
	@Id @GeneratedValue
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name="shipid")
	@ForeignKey(name="tradepost_sell_fk_ships")
	private Ship ship;

	private int resourceid;

	@Column(name="minimum", nullable = false)
	private long limit;
	private long price;
    @Column(name="min_rank", nullable = false)
    private int minRank;
	
	@Version
	private int version;
	

	/**
	 * Konstruktor.
	 */
	protected SellLimit() {
		// EMPTY
	}
	/**
	 * Konstruktor.
	 * @param ship Das Schiff zu dem das Limit gehoert
	 * @param resourceid Die ID des Items
	 * @param price the price for this kind of resource
	 * @param limit the limit for this kind of resource
	 * @param sellRank Der Mindestrang, ab dem die Ressource gekauft werden kann
	 */
	public SellLimit(Ship ship, ItemID resourceid, long price, long limit, int sellRank) {
		this.ship = ship;
		this.resourceid = resourceid.getItemID();
		this.price = price;
		this.limit = limit;
        this.minRank = sellRank;
	}

	/**
	 * Gibt das Schiff zurueck, zu dem das Limit gehoert.
	 * @return Das Schiff
	 */
	public Ship getShip()
	{
		return ship;
	}

	/**
	 * Setzt das Schiff, zu dem das Limit gehoert.
	 * @param ship Das Schiff
	 */
	public void setShip(Ship ship)
	{
		this.ship = ship;
	}

	/**
	 * Gibt die ID des limitierten Items zurueck.
	 * @return Die ID
	 */
	public ItemID getResourceId()
	{
		return new ItemID(resourceid);
	}

	/**
	 * Setzt die ID des limitierten Items.
	 * @param resourceid Die ID
	 */
	public void setResourceId(ItemID resourceid)
	{
		this.resourceid = resourceid.getItemID();
	}

	/**
	 * Gibt das Limit der Resource zurueck.
	 * @return Das Limit
	 */
	public long getLimit() {
		return limit;
	}
	
	/**
	 * Gibt den Verkaufspreis der Ware zurueck.
	 * @return Der Verkaufspreis in RE
	 */
	public long getPrice() {
		return price;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * set the price for this limit.
	 * @param price the price of the resource
	 */
	public void setPrice(long price) {
		this.price = price;
	}

	/**
	 * set the limit.
	 * @param limit the limit for this resource
	 */
	public void setLimit(long limit) {
		this.limit = limit;
	}

    /**
     * @param rank The rank a player needs with the buyer to sell this resource.
     */
    public void setMinRank(int rank)
    {
        this.minRank = rank;
    }

    /**
     * @return The rank a player needs with the buyer to sell this resource.
     */
    public int getMinRank()
    {
        return this.minRank;
    }

    /**
     * @param seller The owner of the tradepost.
     * @param buyer The user who wants to buy the resource.
     * @return <code>true</code>, if the seller can sell the resource to the buyer.
     */
    public boolean willSell(User seller, User buyer)
    {
        int rank = buyer.getRank(seller).getRank();
        return rank >= this.minRank;
    }

	/**
	 * Laedt ein einzelnes Limit fuer ein bestimmtes Schiff und ein bestimmtes Item.
	 * Falls kein Limit existiert wird <code>null</code> zurueckgegeben.
	 *
	 * @param ship Das Schiff
	 * @param resource Die ID des Items
	 * @return Das Limit oder <code>null</code>
	 */
	public static SellLimit fuerSchiffUndItem(Ship ship, ResourceID resource)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (SellLimit)db.createQuery("from SellLimit where ship=:ship and resourceid=:resourceid")
			.setEntity("ship", ship)
			.setInteger("resourceid", resource.getItemID())
			.uniqueResult();
	}
}
