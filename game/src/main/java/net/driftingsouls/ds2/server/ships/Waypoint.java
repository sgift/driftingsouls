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

/**
 * Ein Wegpunkt in einer Flugroute.
 * @author Christopher Jung
 *
 */
public class Waypoint {
	/**
	 * Der Typ des Wegpunktes. Dieser bestimmt was fuer Fluginformationen im
	 * Wegpunkt vorliegen
	 *
	 */
	public enum Type {
		/**
		 * <p>Flug in eine Richtung</p>
		 * Der Wegpunkt enthaelt Richtung und Entfernung bis zum naechsten Wegpunkt.
		 * Die Richtungen:<br>
		 * 1 2 3<br>
		 * 4&nbsp;&nbsp;&nbsp;6<br>
		 * 7 8 9<br>
		 */
		MOVEMENT
	}
	
	/**
	 * Der Typ des Wegpunktes.
	 */
	public Type type;
	/**
	 * Die Richtung in die geflogen werden soll.
	 * Die genaue Bedeutung ist vom Typ abhaengig.
	 */
	public int direction;
	/**
	 * Die zu fliegende Distanz.
	 * Die genaue Bedeutung ist vom Typ abhaengig.
	 */
	public int distance;
	
	Waypoint(Type type, int direction, int distance) {
		this.type = type;
		this.direction = direction;
		this.distance = distance;
	}
}
