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
package net.driftingsouls.ds2.server;

import java.io.Serializable;

import javax.persistence.Embeddable;

/**
 * Eine Version von Location, deren Inhalt veraenderbar ist und die als komplexes Attribut fuer Hibernate
 * verwendet werden kann.
 * @author Christopher Jung
 *
 */
@Embeddable
public class MutableLocation implements Locatable, Serializable {
	private static final long serialVersionUID = -8484096947032118472L;
	
	private int x;
	private int y;
	private int system;
	
	/**
	 * Konstruktor.
	 *
	 */
	public MutableLocation() {
		// EMPTY
	}

	/**
	 * Erstellt ein neues Positionsobjekt bestehend aus einem Sternensystem, einer x- sowie einer y-Position.
	 * 
	 * @param system Das Sternensystem (id)
	 * @param x Die x-Position
	 * @param y Die y-Position
	 */
	public MutableLocation(int system, int x, int y) {
		this.system = system;
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Erstellt ein neues Positionsobjekt aus einem Locatable-Objekt.
	 * @param loc Das Location-Objekt
	 */
	public MutableLocation(Locatable loc) {
		this.system = loc.getLocation().getSystem();
		this.x = loc.getLocation().getX();
		this.y = loc.getLocation().getY();
	}
	
	/**
	 * Gibt die ID des Sternensystems zurueck.
	 * @return Die ID des Sternensystems
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Gibt die X-Position zurueck.
	 * @return Die X-Position
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gibt die Y-Position zurueck.
	 * @return Die Y-Position
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * Setzt die ID des Sternensystems.
	 * @param system Die ID des Sternensystems
	 */
	public void setSystem(int system) {
		this.system = system;
	}

	/**
	 * Setzt die X-Position.
	 * @param x Die X-Position
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Setzt die Y-Position.
	 * @param y Die Y-Position
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	@Override
	public int hashCode() {
		int result = 31 + system;
		result = 31 * result + x;
		result = 31 * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if( this == obj ) {
			return true;
		}
		if( (obj == null) || (getClass() != obj.getClass()) ) {
			return false;
		}
		
		final MutableLocation other = (MutableLocation)obj;
		if( system != other.system ) {
			return false;
		}
		if( x != other.x ) {
			return false;
		}
		return y == other.y;
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}
	
	/**
	 * Generiert ein Location-Objekt aus einem Positionsstring des
	 * Formats system:x/y oder x/y.
	 * 
	 * @param loc Der Positionsstring
	 * @return Das zum Positionsstring gehoerende Location-Objekt
	 */
	public static MutableLocation fromString(String loc) {
		return new MutableLocation(Location.fromString(loc));
	}
	
	@Override
	public String toString() {
		return this.system+":"+this.x+"/"+this.y;
	}
}
