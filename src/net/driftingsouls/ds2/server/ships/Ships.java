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
package net.driftingsouls.ds2.server.ships;

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Diverse Funktionen rund um Schiffe in DS.
 * TODO: Ja, ich weiss, das ist nicht besonders schoen. Besser waeren richtige Schiffsobjekte...
 * @author Christopher Jung
 *
 */
public class Ships {

	/**
	 * Leert den Cache fuer Schiffsdaten.
	 *
	 */
	public static void clearShipCache() {
		// TODO - Schiffcache implementieren
	}
	
	/**
	 * Berechnet das Status-Feld des Schiffes neu. Diese Aktion sollte nach jeder
	 * Operation angewendet werden, die das Schiff in irgendeiner Weise veraendert.
	 * @param shipID die ID des Schiffes
	 * @return der neue Status-String
	 */
	public static String recalculateShipStatus(int shipID) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		Ship ship = (Ship)db.get(Ship.class, shipID);
		
		return ship.recalculateShipStatus();
	}

	/**
	 * Gibt den Positionstext unter Beruecksichtigung von Nebeleffekten zurueck.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt).
	 * @param system Die System-ID
	 * @param x Die X-Koordinate
	 * @param y Die Y-Koordinate
	 * @param noSystem Soll das System angezeigt werden?
	 * @return der Positionstext
	 */
	public static String getLocationText(int system, int x, int y, boolean noSystem) {
		Nebel.Typ nebel = getNebula(new Location(system, x, y));
		
		StringBuilder text = new StringBuilder(8);
		if( !noSystem ) {
			text.append(system);
			text.append(":");
		}
		
		if( nebel == Nebel.Typ.LOW_EMP ) {
			text.append(x / 10);
			text.append("x/");
			text.append(y / 10);
			text.append('x');
			
			return text.toString();
		}
		else if( (nebel == Nebel.Typ.MEDIUM_EMP) || (nebel == Nebel.Typ.STRONG_EMP) ) {
			text.append(":??/??");
			return text.toString();
		}
		text.append(x);
		text.append('/');
		text.append(y);
		return text.toString();
	}
	
	/**
	 * Gibt den Positionstext fuer die Position zurueck.
	 * Beruecksichtigt werden Nebeleffekten.
	 * Dadurch kann der Positionstext teilweise unleserlich werden (gewuenschter Effekt).
	 * @param loc Die Position
	 * @param noSystem Soll die System-ID angezeigt werden?
	 * @return Der Positionstext
	 */
	public static String getLocationText(Location loc, boolean noSystem) {
		return getLocationText(loc.getSystem(), loc.getX(), loc.getY(), noSystem);
	}
		
	/**
	 * Gibt den Nebeltyp an der angegebenen Position zurueck. Sollte sich an der Position kein
	 * Nebel befinden, wird <code>null</code> zurueckgegeben.
	 * @param loc Die Position
	 * @return Der Nebeltyp oder <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static synchronized Nebel.Typ getNebula(Location loc) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		// Hibernate cachet nur Ergebnisse, die nicht leer waren.
		// Da es jedoch viele Positionen ohne Nebel gibt wuerden viele Abfragen
		// mehrfach durchgefuehrt. Daher wird in der Session vermerkt, welche
		// Positionen bereits geprueft wurden
		
		Map<Location,Boolean> map = (Map<Location,Boolean>)context.getVariable(Ships.class, "getNebula(Location)#Nebel");
		if( map == null ) {
			map = new HashMap<Location,Boolean>();
			context.putVariable(Ships.class, "getNebula(Location)#Nebel", map);
		}
		if( !map.containsKey(loc) ) {
			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(loc));
			if( nebel == null ) {
				map.put(loc, Boolean.FALSE);
				return null;
			}
			
			map.put(loc, Boolean.TRUE);
			return nebel.getType();		
		}
		
		Boolean val = map.get(loc);
		if(val) {
			Nebel nebel = (Nebel)db.get(Nebel.class, new MutableLocation(loc));
			return nebel.getType();
		}
			
		return null;
	}
}
