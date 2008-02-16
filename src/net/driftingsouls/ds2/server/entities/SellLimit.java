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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
	private long limit;
	private long price;
	
	/**
	 * Gibt die ID des Resourcenlimits zurueck
	 * @return Die ID
	 */
	public ResourceLimitKey getId() {
		return this.resourceLimitKey;
	}

	/**
	 * Gibt das Limit der Resource zurueck
	 * @return Das Limit
	 */
	public long getMinimum() {
		return limit;
	}
	
	/**
	 * Gibt den Verkaufspreis der Ware zurueck
	 * @return Der Verkaufspreis in RE
	 */
	public long getPrice() {
		return price;
	}
}
