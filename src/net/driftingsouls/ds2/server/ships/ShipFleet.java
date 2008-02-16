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
package net.driftingsouls.ds2.server.ships;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Eine Flotte aus Schiffen
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ship_fleets")
public class ShipFleet {
	@Id @GeneratedValue
	private int id;
	private String name;
	
	/**
	 * Konstruktor
	 *
	 */
	public ShipFleet() {
		// EMPTY
	}
	
	/**
	 * <p>Konstruktor</p>
	 * Erstellt eine neue Flotte
	 * @param name Der Name der Flotte
	 */
	public ShipFleet(String name) {
		this.name = name;
	}

	/**
	 * Gibt den Namen der Flotte zurueck
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen der Flotte
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt die ID der Flotte zurueck
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
}
