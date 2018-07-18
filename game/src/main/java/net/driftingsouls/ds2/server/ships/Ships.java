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

import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Diverse Funktionen rund um Schiffe in DS.
 * TODO: Ja, ich weiss, das ist nicht besonders schoen. Besser waeren richtige Schiffsobjekte...
 * @author Christopher Jung
 *
 */
public class Ships {

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
}
