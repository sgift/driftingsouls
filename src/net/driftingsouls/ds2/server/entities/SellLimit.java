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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.entities.ResourceLimit.ResourceLimitKey;

/**
 * A sell limit for a single resource.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="tradepost_sell")
public class SellLimit {
	@Id
	private ResourceLimitKey resourceLimitKey;
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
	public SellLimit() {
		// EMPTY
	}
	/**
	 * Konstruktor.
	 * @param resourcelimitkey the key of the limit
	 * @param price the price for this kind of resource
	 * @param limit the limit for this kind of resource
	 * @param sellRank Der Mindestrang, ab dem die Ressource gekauft werden kann
	 */
	public SellLimit(ResourceLimitKey resourcelimitkey, long price, long limit, int sellRank) {
		this.resourceLimitKey = resourcelimitkey;
		this.price = price;
		this.limit = limit;
        this.minRank = sellRank;
	}

	/**
	 * Gibt die ID des Resourcenlimits zurueck.
	 * @return Die ID
	 */
	public ResourceLimitKey getId() {
		return this.resourceLimitKey;
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
}
