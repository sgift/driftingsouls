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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.annotations.ForeignKey;

/**
 * Ein Sprung eines Schiffes durch den Subraum (Subraumspalte).
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="jumps")
public class Jump implements Locatable {
	@Id @GeneratedValue
	private int id;
	@OneToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="shipid", nullable = false)
	@ForeignKey(name="jumps_fk_ships")
	private Ship ship;
	private int x;
	private int y;
	private int system;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Jump() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param ship Das springende Schiff
	 * @param loc Der Zielpunkt
	 */
	public Jump(Ship ship, Location loc) {
		setShip(ship);
		setX(loc.getX());
		setY(loc.getY());
		setSystem(loc.getSystem());
	}

	/**
	 * Gibt das springende Schiff zurueck.
	 * @return Das Schiff
	 */
	public Ship getShip() {
		return ship;
	}

	/**
	 * Setzt das springende Schiff.
	 * @param ship Das Schiff
	 */
	public final void setShip(final Ship ship) {
		this.ship = ship;
	}

	/**
	 * Gibt das Zielsystem zurueck.
	 * @return Das Zielsystem
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Setzt das Zielsystem.
	 * @param system Das System
	 */
	public final void setSystem(final int system) {
		this.system = system;
	}

	/**
	 * Gibt die Ziel-X-Koordinate zurueck.
	 * @return Die Ziel-X-Koordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Setzt die Ziel-X-Koordinate.
	 * @param x Die Ziel-X-Koordinate
	 */
	public final void setX(final int x) {
		this.x = x;
	}

	/**
	 * Gibt die Ziel-Y-Koordinate zurueck.
	 * @return Die Ziel-Y-Koordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Setzt die Ziel-Y-Koordinate.
	 * @param y Die Ziel-Y-Koordinate
	 */
	public final void setY(final int y) {
		this.y = y;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
