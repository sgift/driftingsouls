/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.scripting;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextLocalMessage;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.SchiffFlugService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.Waypoint;

import java.util.List;

/**
 * <p>Hilfsfunktionen zum Umgang mit Schiffen fuer das Scripting-System.</p>
 * <p>Vorerst hier gesammelt, bis eine bessere Loesung gefunden ist.</p>
 * @author Christopher Jung
 *
 */
public class ShipUtils {
	/**
	 * Objekt mit Funktionsmeldungen.
	 */
	public static final ContextLocalMessage MESSAGE = new ContextLocalMessage();
	
	private ShipUtils() {
		// EMPTY
	}
	
	/**
	 * <p>Fliegt ein Schiff zur angegebenen Position im selben System. EMP-Nebel werden
	 * dabei beruecksichtigt und durchflogen.</p>
	 * <p>Zudem wird eine maximale Anzahl an zu fliegenden Feldern nicht ueberschritten
	 * (hier kommt es noch zu Fehlern im Zusammenspiel mit EMP-Nebeln).</p>
	 * <p>Logmeldungen werden nach {@link #MESSAGE} geschrieben</p>
	 * @param ship Das zu fliegende Schiff
	 * @param target Das Ziel
	 * @param maxcount Die maximale Anzahl zu fliegender Felder
	 * @return <code>false</code>, falls der Flug vorzeitig beendet werden musste
	 */
	public static boolean move(Ship ship, Location target, int maxcount) {
		MESSAGE.get().setLength(0);
		
		if( ship.getLocation().getSystem() != target.getLocation().getSystem() ) {
			throw new IllegalArgumentException("Start- und Zielsystem muessen identisch sein");
		}
		
		// TODO maxcount wird beim Flug durch EMP-Nebel falsch interpretiert
		
		int oldMaxCount = maxcount;
		SchiffFlugService.FlugErgebnis result;
		SchiffFlugService flugService = ContextMap.getContext().getBean(SchiffFlugService.class, null);

		while( true ) {
			int deltax = target.getX()-ship.getX();
			int deltay = target.getY()-ship.getY();
			
			if( (deltax == 0) && (deltay == 0) ) {
				MESSAGE.get().append("Zielposition bereits erreicht!\n\n");
				return true;
			}
						
			if( ship.getHeat() > 100 ) {
				MESSAGE.get().append("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return false;
			}
			
			RouteFactory router = new RouteFactory();
			List<Waypoint> route = router.findRoute(ship.getLocation(), target, maxcount);
			
			result = flugService.fliege(ship, route, true);
			MESSAGE.get().append(Common._stripHTML(result.getMeldungen()));
			
			if( result.getStatus() == SchiffFlugService.FlugStatus.BLOCKED_BY_EMP ) {
				maxcount = 1;
			}
			else if( result.getStatus() != SchiffFlugService.FlugStatus.SUCCESS ) {
				MESSAGE.get().append("Ausfuehrung bis zum naechsten Tick angehalten\n\n");
				return false;
			}
			// Nach einem Flug durch einen EMP-Nebel den Flug normal weiterfuehren
			else if( oldMaxCount != maxcount ) {
				maxcount = oldMaxCount;

			}
			else {
				break;
			}
		}
		
		if( !ship.getLocation().equals(target) ) {
			MESSAGE.get().append("Position nicht korrekt - Ausfuehrung bis zum naechsten Tick angehalten\n\n");
			return false;
		}
		
		return true;
	}
}
