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
package net.driftingsouls.ds2.server;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Eine Positionsklasse
 * @author Christopher Jung
 *
 */
public class Location {
	private int x = 0;
	private int y = 0;
	private int system = 0;
	private int hashCode = 0;
	
	/**
	 * Erstellt ein neues Positionsobjekt fuer den Ort 0:0/0
	 *
	 */
	public Location() {
	}

	/**
	 * Erstellt ein neues Positionsobjekt bestehend aus einem Sternensystem, einer x- sowie einer y-Position
	 * 
	 * @param system Das Sternensystem (id)
	 * @param x Die x-Position
	 * @param y Die y-Position
	 */
	public Location( int system, int x, int y ) {
		this.x = x;
		this.y = y;
		this.system = system;
	}

	/**
	 * Gibt die ID des Sternensystems zurueck
	 * @return Die ID des Sternensystems
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Gibt die X-Position zurueck
	 * @return Die X-Position
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gibt die Y-Position zurueck
	 * @return Die Y-Position
	 */
	public int getY() {
		return y;
	}
	
	@Override
	public String toString() {
		return system+":"+x+"/"+y;
	}
	
	/**
	 * Erzeugt eine neue Location aus dieser mit den angegebenen X-Wert
	 * @param x der neue X-Wert
	 * @return Das neue Location-Objekt
	 */
	public Location setX(int x) {
		return new Location(system, x, y);
	}
	
	/**
	 * Erzeugt eine neue Location aus dieser mit den angegebenen Y-Wert
	 * @param y der neue Y-Wert
	 * @return Das neue Location-Objekt
	 */
	public Location setY(int y) {
		return new Location(system, x, y);
	}
	
	/**
	 * Erzeugt eine neue Location aus dieser mit der angegebenen System-ID
	 * @param system die neue System-ID
	 * @return Das neue Location-Objekt
	 */
	public Location setSystem(int system) {
		return new Location(system, x, y);
	}
	
	/**
	 * Generiert ein Location-Objekt aus einem Positionsstring des
	 * Formats system:x/y oder x/y
	 * 
	 * @param loc Der Positionsstring
	 * @return Das zum Positionsstring gehoerende Location-Objekt
	 */
	public static Location fromString(String loc) {
		int separator = loc.indexOf(':');
		int system = 0;
		
		if( separator > -1 ) {
			system = Integer.parseInt(loc.substring(0, separator));
			loc = loc.substring(separator+1);
		}
		
		separator = loc.indexOf('/');
		if( separator == -1 ) {
			throw new RuntimeException("Illegales Koordinatenformat '"+loc+"'! Separator / fehlt");
		}
		int x = Integer.parseInt(loc.substring(0, separator));
		int y = Integer.parseInt(loc.substring(separator+1));
		
		return new Location(system, x, y);
	}
	
	/**
	 * Generiert ein Location-Objekt aus einer SQL-Ergebniszeile, welche die Spalten
	 * 'x', 'y' und 'system' enthaelt
	 * @param result Die SQL-Ergebniszeile
	 * @return Das zur Ergebniszeile gehoerende Location-Objekt
	 */
	public static Location fromResult(SQLResultRow result) {
		return new Location(result.getInt("system"), result.getInt("x"), result.getInt("y"));
	}
	
	@Override
	public boolean equals(Object obj) {
		if( !(obj instanceof Location) ) {
			return false;
		}
		Location loc = (Location)obj;
		if( getSystem() != loc.getSystem() ) {
			return false;
		}
		if( getX() != loc.getX() ) {
			return false;
		}
		
		if( getY() != loc.getY() ) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		if( hashCode == 0 ) {
			hashCode = system*10000+x*100+y;
		}
		
		return hashCode;
	}
	
	/**
	 * Prueft, ob zwei Koordinaten unter Beruecksichtigung ihrer Radien einen oder mehrere
	 * gemeinsame Sektoren haben (sie sich also in gewisser Weise im selben Sektor befinden)
	 * 
	 * @param ownRadius Der eigene Radius (bzw 0 wenn das Objekt keine Ausdehnung hat)
	 * @param object Das Objekt mit dem es einen gemeinsamen Sektor haben soll
	 * @param objectRadius Der Radius des Objekts
	 * @return true, falls ein gemeinsamer Sektor existiert
	 */
	public boolean sameSector(int ownRadius, Location object, int objectRadius) {
		if( getSystem() != object.getSystem() ) {
			return false;
		}
		
		if( Math.floor(Math.sqrt((getX()-object.getX())*(getX()-object.getX())+(getY()-object.getY())*(getY()-object.getY()))) > ownRadius+objectRadius ) {
			return false;
		}
		
		return true;
	}
}
