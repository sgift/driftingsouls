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
 * A limit for a single resource.
 *
 * @author Sebastian Gift
 */
@Entity
@Table(name = "tradepost_buy_limit", uniqueConstraints = {@UniqueConstraint(name="ship_itemid_uniq", columnNames = {"shipid", "resourceid"})})
public class ResourceLimit
{
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "shipid")
	@ForeignKey(name = "tradepost_buy_limit_fk_ships")
	private Ship ship;

	private int resourceid;

	@Column(name = "maximum", nullable = false)
	private long limit;
	@Column(name = "min_rank", nullable = false)
	private int minRank;

	@Version
	private int version;

	/**
	 * Konstruktor.
	 */
	protected ResourceLimit()
	{
		//Empty
	}

	/**
	 * generates an new ResourceLimit with key and limit as parameters.
	 *
	 * @param ship Das Schiff zu dem das Limit gehoert
	 * @param resourceid Die ID des Items
	 * @param limit the limit of this kind of resources
	 * @param rank Der minimale Rang ab dem die Ressource verkauft werden darf
	 */
	public ResourceLimit(Ship ship, ItemID resourceid, long limit, int rank)
	{
		this.ship = ship;
		this.resourceid = resourceid.getItemID();
		this.limit = limit;
		this.minRank = rank;
	}

	/**
	 * Gibt das Schiff zurueck, zu dem das Limit gehoert.
	 *
	 * @return Das Schiff
	 */
	public Ship getShip()
	{
		return ship;
	}

	/**
	 * Setzt das Schiff, zu dem das Limit gehoert.
	 *
	 * @param ship Das Schiff
	 */
	public void setShip(Ship ship)
	{
		this.ship = ship;
	}

	/**
	 * Gibt die ID des limitierten Items zurueck.
	 *
	 * @return Die ID
	 */
	public ItemID getResourceId()
	{
		return new ItemID(resourceid);
	}

	/**
	 * Setzt die ID des limitierten Items.
	 *
	 * @param resourceid Die ID
	 */
	public void setResourceId(ItemID resourceid)
	{
		this.resourceid = resourceid.getItemID();
	}

	/**
	 * Gibt das Limit der Resource zurueck.
	 *
	 * @return Das Limit
	 */
	public long getLimit()
	{
		return limit;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 *
	 * @return Die Nummer
	 */
	public int getVersion()
	{
		return this.version;
	}

	/**
	 * Sets the limit for this ResourceLimit.
	 *
	 * @param limit the limit for this kind of resource
	 */
	public void setLimit(long limit)
	{
		this.limit = limit;
	}

	/**
	 * @param rank The rank a player needs with the seller to buy this resource.
	 */
	public void setMinRank(int rank)
	{
		this.minRank = rank;
	}

	/**
	 * @return The rank a player needs with the seller to buy this resource.
	 */
	public int getMinRank()
	{
		return this.minRank;
	}

	/**
	 * @param buyer The owner of the trade post.
	 * @param seller The user who wants to buy the resource.
	 * @return <code>true</code>, if the seller can sell the resource to the buyer.
	 */
	public boolean willBuy(User buyer, User seller)
	{
		int rank = seller.getRank(buyer).getRank();
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
	public static ResourceLimit fuerSchiffUndItem(Ship ship, ResourceID resource)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (ResourceLimit) db.createQuery("from ResourceLimit where ship=:ship and resourceid=:resourceid")
				.setEntity("ship", ship)
				.setInteger("resourceid", resource.getItemID())
				.uniqueResult();
	}
}
