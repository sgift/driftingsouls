/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

/**
 * Ein Nebel
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="nebel")
@Immutable
@Cache(usage=CacheConcurrencyStrategy.READ_ONLY)
public class Nebel implements Locatable {
	@Id
	private MutableLocation loc;
	private int type;
	
	/**
	 * Konstruktor
	 *
	 */
	public Nebel() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Nebel
	 * @param loc Die Position des Nebels
	 * @param type Der Typ
	 */
	public Nebel(MutableLocation loc, int type) {
		this.loc = loc;
		this.type = type;
	}
		
	/**
	 * Gibt das System des Nebels zurueck
	 * @return Das System
	 */
	public int getSystem() {
		return loc.getSystem();
	}
	
	/**
	 * Gibt den Typ des Nebels zurueck
	 * @return Der Typ
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Gibt die X-Koordinate zurueck
	 * @return Die X-Koordinate
	 */
	public int getX() {
		return loc.getX();
	}
	
	/**
	 * Gibt die Y-Koordinate zurueck
	 * @return Die Y-Koordinate
	 */
	public int getY() {
		return loc.getY();
	}
	
	/**
	 * Gibt die Position des Nebels zurueck
	 * @return Die Position
	 */
	public Location getLocation() {
		return loc.getLocation();
	}
}
