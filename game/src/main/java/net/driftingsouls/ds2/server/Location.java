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

import net.driftingsouls.ds2.server.entities.Nebel;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Eine Positionsklasse.
 * @author Christopher Jung
 *
 */
@Embeddable
public final class Location implements Serializable, Locatable, Comparable<Location> {
	private static final long serialVersionUID = -5144442902462679539L;

	private final int x;
	private final int y;
	private final int system;
	private transient int hashCode = 0;

	/**
	 * Erstellt ein neues Positionsobjekt fuer den Ort 0:0/0.
	 *
	 */
	public Location() {
		this.x = 0;
		this.y = 0;
		this.system = 0;
	}

	/**
	 * Erstellt ein neues Positionsobjekt bestehend aus einem Sternensystem, einer x- sowie einer y-Position.
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
	 * Exportiert die Position als String, welcher mittels
	 * {@link #fromString(String)} wieder geparst werden kann.
	 * @return Die serialisierte Position
	 */
	public String asString() {
		return this.system+":"+this.x+"/"+this.y;
	}

	@Override
	public String toString() {
		return "THIS METHOD IGNORES FIELD TYPES IN PRINTING: USE displayCoordinates() INSTEAD";
	}

	/**
	 * Erzeugt eine neue Location aus dieser mit den angegebenen X-Wert.
	 * @param x der neue X-Wert
	 * @return Das neue Location-Objekt
	 */
	public Location setX(int x) {
		return new Location(system, x, y);
	}

	/**
	 * Erzeugt eine neue Location aus dieser mit den angegebenen Y-Wert.
	 * @param y der neue Y-Wert
	 * @return Das neue Location-Objekt
	 */
	public Location setY(int y) {
		return new Location(system, x, y);
	}

	/**
	 * Erzeugt eine neue Location aus dieser mit der angegebenen System-ID.
	 * @param system die neue System-ID
	 * @return Das neue Location-Objekt
	 */
	public Location setSystem(int system) {
		return new Location(system, x, y);
	}

	/**
	 * Berechnet die Entfernung von dieser Position zu einem anderen Position.
	 * Die Entfernung wird nicht als Flugdistanz sondern als tatsaechliche Entfernung (Pythagoras)
	 * berechnet. Liegen die beiden Positionen nicht im selben Sternensystem wird ein Fehler geworfen.
	 * @param loc Die Position zu der die Distanz berechnet werden soll
	 * @return Die Distanz
	 * @throws IllegalArgumentException Falls beide Positionen nicht im selben Sternensystem liegen
	 */
	public double distanzZu(Location loc) throws IllegalArgumentException
	{
		if( loc.getSystem() != this.system )
		{
			throw new IllegalArgumentException("Die beiden Positionen liegen in verschiedenen Sternensystemen");
		}

		return Math.sqrt(Math.pow(loc.getX()-this.x,2)+Math.pow(loc.getY()-this.y, 2));
	}

	/**
	 * Generiert ein Location-Objekt aus einem Positionsstring des.
	 * Formats system:x/y oder x/y
	 *
	 * @param loc Der Positionsstring
	 * @return Das zum Positionsstring gehoerende Location-Objekt
	 */
	public static Location fromString(String loc) {
		String parseLoc = loc;
		int separator = parseLoc.indexOf(':');
		int system = 0;

		if( separator > -1 ) {
			system = Integer.parseInt(loc.substring(0, separator));
			parseLoc = parseLoc.substring(separator+1);
		}

		separator = parseLoc.indexOf('/');
		if( separator == -1 ) {
			throw new RuntimeException("Illegales Koordinatenformat '"+loc+"'! Separator / fehlt");
		}
		int x = Integer.parseInt(parseLoc.substring(0, separator));
		int y = Integer.parseInt(parseLoc.substring(separator+1));

		return new Location(system, x, y);
	}

	@Override
	public boolean equals(Object obj) {
		if( this == obj ) {
			return true;
		}
		if( (obj == null) || (obj.getClass() != getClass()) ) {
			return false;
		}

		final Location loc = (Location)obj;
		if( this.system != loc.system ) {
			return false;
		}
		if( this.x != loc.x ) {
			return false;
		}

		return this.y == loc.y;
	}

	@Override
	public int hashCode() {
		if( hashCode == 0 ) {
			hashCode = system*100000+x*1000+y;
		}

		return hashCode;
	}

	/**
	 * Prueft, ob zwei Koordinaten unter Beruecksichtigung ihrer Radien einen oder mehrere
	 * gemeinsame Sektoren haben (sie sich also in gewisser Weise im selben Sektor befinden).
	 *
	 * @param ownRadius Der eigene Radius (bzw 0 wenn das Objekt keine Ausdehnung hat)
	 * @param object Das Objekt mit dem es einen gemeinsamen Sektor haben soll
	 * @param objectRadius Der Radius des Objekts
	 * @return true, falls ein gemeinsamer Sektor existiert
	 */
	public boolean sameSector(int ownRadius, Locatable object, int objectRadius) {
		Location loc = object.getLocation();
		if( this.system != loc.getSystem() ) {
			return false;
		}

		return !(Math.floor(Math.sqrt((this.x - loc.getX()) * (this.x - loc.getX()) + (this.y - loc.getY()) * (this.y - loc.getY()))) > ownRadius + objectRadius);
	}

	/**
	 * Gibt die angezeigten Koordinaten zurueck.
	 * EMP wird dabei beruecksichtigt.
	 *
	 * @param noSystem <code>true</code>, wenn das System nicht mit angezeigt werden soll, sonst <code>false</code>
	 * @return Anzeigbare Koordinaten.
	 */
	public String displayCoordinates(boolean noSystem)
	{
		Nebel.Typ nebulaType = Nebel.getNebula(this);

		StringBuilder text = new StringBuilder(8);
		if( !noSystem ) {
			text.append(system);
			text.append(":");
		}

		if( nebulaType == Nebel.Typ.LOW_EMP ) {
			text.append(x / 10);
			text.append("x/");
			text.append(y / 10);
			text.append('x');

			return text.toString();
		}
		else if( (nebulaType == Nebel.Typ.MEDIUM_EMP) || (nebulaType == Nebel.Typ.STRONG_EMP) ) {
			text.append("??/??");
			return text.toString();
		}
		text.append(x);
		text.append('/');
		text.append(y);

		return text.toString();
	}

	/**
	 * Gibt die fuer den Benutzer sichtbaren Koordinaten als URL-Fragment fuer die Sternenkarte zurueck
	 * (z.B. <code>4/50/51</code> fuer die Position <code>4:50/51</code>).
	 * EMP wird dabei beruecksichtigt.
	 *
	 * @return Das URL-Fragment.
	 */
	public String urlFragment()
	{
		Nebel.Typ nebulaType = Nebel.getNebula(this);

		StringBuilder text = new StringBuilder(8);
		text.append(system);
		text.append("/");

		if( nebulaType == Nebel.Typ.LOW_EMP ) {
			text.append(x / 10);
			text.append("x/");
			text.append(y / 10);
			text.append('x');

			return text.toString();
		}
		else if( (nebulaType == Nebel.Typ.MEDIUM_EMP) || (nebulaType == Nebel.Typ.STRONG_EMP) ) {
			text.append("xx/xx");
			return text.toString();
		}
		text.append(x);
		text.append('/');
		text.append(y);

		return text.toString();
	}


	@Override
	public Location getLocation() {
		return this;
	}

	/**
	 * Vergleicht den Sektor mit einem anderen.
	 * Beim Vergleich werden erst System, dann y, dann x getestet.
	 *
	 * @param o Vergleichsobjekt.
	 *
	 * @return vgl compareTo in Interface Comparable.
	 */
	@Override
	public int compareTo(@NotNull Location o)
	{
		if(system == o.system)
		{
			if(y == o.y)
			{
				return Integer.compare(x, o.x);
			}
			else if(y < o.y)
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}
		else if(system < o.system)
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}
}
