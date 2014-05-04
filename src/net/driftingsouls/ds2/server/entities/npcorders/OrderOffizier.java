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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Die Bestellung eines Offiziers duch einen NPC (NPC-Order).
 * @author Christopher Jung
 *
 */
@Entity
@DiscriminatorValue("offizier")
public class OrderOffizier extends Order
{
	private int type;
	
	protected OrderOffizier()
	{
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param user Der User, fuer den der Auftrag abgewickelt werden soll
	 * @param type Der Typ, der abgewickelt werden soll
	 */
	public OrderOffizier(User user, int type)
	{
		setUser(user);
		setType(type);
	}
	
	/**
	 * Gibt den abzuwickelnden Auftragstyp an.
	 * @return Der Auftragstyp
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Setzt den abzuwickelnden Auftragstyp.
	 * @param type Der Typ
	 */
	public final void setType(final int type)
	{
		this.type = type;
	}

}
