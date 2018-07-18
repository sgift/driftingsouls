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
package net.driftingsouls.ds2.server.entities.npcorders;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Die Bestellung eines Schiffes duch einen NPC (NPC-Order).
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("ship")
public class OrderShip extends Order
{
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="order_ship_fk_ship_type")
	private ShipType shipType;
	private String flags;
	
	protected OrderShip()
	{
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param user Der User, fuer den der Auftrag abgewickelt werden soll
	 * @param type Der Typ, der abgewickelt werden soll
	 */
	public OrderShip(User user, ShipType type)
	{
		setUser(user);
		this.shipType = type;
		this.flags = "";
	}
	
	/**
	 * Gibt den bestellten Schiffstyp zurueck.
	 * @return Der Schiffstyp
	 */
	public ShipType getShipType()
	{
		return shipType;
	}

	/**
	 * Setzt den bestellten Schiffstyp.
	 * @param type Der Schiffstyp
	 */
	public final void setShipType(final ShipType type)
	{
		this.shipType = type;
	}

	/**
	 * Gibt alle Flags als konkatenierten String zurueck.
	 * @return Die Flags
	 */
	public String getFlags()
	{
		return flags;
	}
	
	/**
	 * Fuegt ein Flag zur Flagliste hinzu.
	 * @param flag Das Flag
	 */
	public void addFlag(String flag)
	{
		if( flags.length() == 0 )
		{
			flags = flag;
		}
		else
		{
			flags += " "+flag;
		}
	}
}
