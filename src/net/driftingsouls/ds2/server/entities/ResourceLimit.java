package net.driftingsouls.ds2.server.entities;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * A limit for a single resource.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="tradepost_buy_limit")
public class ResourceLimit {
	/**
	 * Der Primaerschluessel eines Resourcenlimits.
	 */
	@Embeddable
	public static class ResourceLimitKey implements Serializable {
		private static final long serialVersionUID = 1510394179895753873L;
		
		private int shipid;
		private int resourceid;

		/**
		 * Konstruktor.
		 */
		public ResourceLimitKey() {
			// EMPTY
		}
		
		/**
		 * Erstellt einen neuen Key.
		 * @param ship Das Schiff
		 * @param resourceId Die ID der Resource
		 */
		public ResourceLimitKey(Ship ship, ResourceID resourceId) {
			this.shipid = ship.getId();
			if(resourceId.isItem())
			{
				this.resourceid = -1 * resourceId.getItemID();
			}
			else
			{
				this.resourceid = resourceId.getID();
			}
		}

		/**
		 * Gibt die ID der Resource zurueck.
		 * @return Die ID
		 */
		public int getResourceId() {
			return resourceid;
		}

		/**
		 * Gibt die ID des Schiffes zurueck.
		 * @return Die ID
		 */
		public int getShipId() {
			return shipid;
		}
	}
	
	@Id
	private ResourceLimitKey resourceLimitKey;
	private long limit;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 */
	public ResourceLimit() {
		//Empty
	}
	
	public ResourceLimit(ResourceLimitKey resourcelimitkey, long limit) {
		this.setResourceLimitKey(resourcelimitkey);
		this.setLimit(limit);
	}

	private void setResourceLimitKey(ResourceLimitKey resourcelimitkey) {
		this.resourceLimitKey = resourcelimitkey;
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
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
	
	public void setLimit(long limit) {
		this.limit = limit;
	}
}
