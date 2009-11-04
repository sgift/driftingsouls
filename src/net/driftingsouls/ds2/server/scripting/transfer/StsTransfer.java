/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Sebastian Gift
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
package net.driftingsouls.ds2.server.scripting.transfer;

import net.driftingsouls.ds2.server.cargo.Transfering;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Transferstrategie fuer Schiff zu Schiff.
 * @author Sebastian Gift
 *
 */
class StsTransfer extends TransferStrategy {
	/**
	 * Konstruktor.
	 * @param fromId Die ID des Quellschiffs
	 * @param toId Die ID des Zielschiffs
	 * @param forceSameOwner <code>true</code>, falls der selbe Besitzer erwzungen werden soll
	 */
	public StsTransfer(int fromId, int toId, boolean forceSameOwner) {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Transfering from = (Transfering) db.get(Ship.class, fromId);
		Transfering to = (Transfering) db.get(Ship.class, toId);
		setFrom(from);
		setTo(to);
		setForceSameOwner(forceSameOwner);
	}
}
