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
package net.driftingsouls.ds2.server.modules.ks;

import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Entlaedt alle Battieren auf eigenen Schiffen einer bestimmten Klasse.
 * @author Christopher Jung
 *
 */
public class KSDischargeBatteriesClassAction extends KSDischargeBatteriesAllAction {
	private ShipClasses battsclass;
	
	/**
	 * Konstruktor.
	 *
	 */
	public KSDischargeBatteriesClassAction() {
		
		this.battsclass = ShipClasses.values()[ContextMap.getContext().getRequest().getParameterInt("battsclass")];
	}

	@Override
	protected boolean validateShipExt(BattleShip ship, ShipTypeData shiptype) {
        return shiptype.getShipClass() == this.battsclass;
    }
}
