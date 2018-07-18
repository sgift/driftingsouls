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

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.Location;

/**
 * Klasse zum Ermitteln vom Flugrouten.
 * @author Christopher Jung
 *
 */
public class RouteFactory {
	/**
	 * Gibt eine Flugoperation als Route zurueck.
	 * @param direction Die Flugrichtung
	 * @param distance Die Entfernung
	 * @return Die Flugoperation als Route
	 */
	public List<Waypoint> getMovementRoute(int direction, int distance) {
		List<Waypoint> route = new ArrayList<>();
		route.add(new Waypoint(Waypoint.Type.MOVEMENT, direction, distance));
		
		return route;
	}
	/**
	 * Ermittelt einen Weg vom Startpunkt zum Zielpunkt.
	 * Wenn Start- und Endpunkt gleich sind, wird eine Route mit 0
	 * Elementen zurueckgegebene.
	 * 
	 * @param from Der Startpunkt
	 * @param to Der Zielpunkt
	 * @return Der Weg vom Start zum Ziel.
	 */
	public List<Waypoint> findRoute(Location from, Location to) {
		return findRoute(from, to, 10000);
	}
	
	/**
	 * Ermittelt einen Weg vom Startpunkt zum Zielpunkt.
	 * Wenn Start- und Endpunkt gleich sind, wird eine Route mit 0
	 * Elementen zurueckgegebene.
	 * 
	 * @param from Der Startpunkt
	 * @param to Der Zielpunkt
	 * @param maxdistance Die maximale Anzahl an zu fliegenden Feldern
	 * @return Der Weg vom Start zum Ziel.
	 */
	public List<Waypoint> findRoute(Location from, Location to, int maxdistance) {
		if( from.getSystem() == to.getSystem() ) {
			return findPlainRoute(from, to, maxdistance);
		}
		throw new RuntimeException("Es werden bisher nur flache Routen (ohne Sprungpunkte) unterstuetzt");
	}
	
	/**
	 * Ermittelt eine Flugroute innerhalb eines Systems.
	 * @param from Der Startpunkt
	 * @param to Der Endpunkt
	 * @param maxcount die maximale Anzahl an zu fliegenden Feldern
	 * @return Die Flugroute
	 */
	private List<Waypoint> findPlainRoute(Location from, Location to, int maxcount) {
		List<Waypoint> route = new ArrayList<>();
		
		int deltax = to.getX()-from.getX();
		int deltay = to.getY()-from.getY();
					
		if( (deltax == 0) && (deltay == 0) ) {
			return route;
		}
				
		int direction = -1;
		int count = 0;

		while( true ) {
			int newdirection = 5;
			if( deltax > 0 ) {
				newdirection += 1;
			}
			else if( deltax < 0 ) {
				newdirection -= 1;
			}
						
			if( deltay > 0 ) {
				newdirection += 3;
			}
			else if( deltay < 0 ) {
				newdirection -= 3;
			}
						
			if( ((direction != -1) && (direction != newdirection)) || (maxcount == 0) ) {
				route.add(new Waypoint(Waypoint.Type.MOVEMENT, direction, count));
	
				if( newdirection == 5 ) {
					break;
				}
				count = 1;
				if( maxcount > 0 ) {
					maxcount--;	
				} 
				else {
					break;	
				}
				direction = newdirection;
			} 
			else {
				count++;
				if( maxcount > 0 ) {
					maxcount--;	
				}
				else {
					count--;
					if( count == 0 ) {
						break;
					}	
				}
				direction = newdirection;
			}
			int xOffset = 0;
			int yOffset = 0;
			
			if( direction == 1 ) { xOffset--; yOffset--;}
			else if( direction == 2 ) { yOffset--;}
			else if( direction == 3 ) { xOffset++; yOffset--;}
			else if( direction == 4 ) { xOffset--;}
			else if( direction == 6 ) { xOffset++;}
			else if( direction == 7 ) { xOffset--; yOffset++;}
			else if( direction == 8 ) { yOffset++;}
			else if( direction == 9 ) { xOffset++; yOffset++;}
			
			from = new Location(from.getSystem(), from.getX()+xOffset, from.getY()+yOffset);

			deltax = to.getX()-from.getX();
			deltay = to.getY()-from.getY();
		}
		
		return route;
	}
}
