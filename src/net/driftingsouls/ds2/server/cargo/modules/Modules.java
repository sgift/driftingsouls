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
package net.driftingsouls.ds2.server.cargo.modules;

import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Allgmeine (Schiffs)Modulfunktionen und Konstanten.
 * @author Christopher Jung
 *
 */
public class Modules {
	/**
	 * Ein Frachtkontainer-Modul.
	 * @see ModuleContainerShip
	 */
	public static final int MODULE_CONTAINER_SHIP = 1;
	/**
	 * Ein Schiffsbild-Modul.
	 * @see ModuleShipPicture
	 */
	public static final int MODULE_SHIP_PICTURE = 2;
	/**
	 * Ein Item-Modul.
	 * @see ModuleItemModule
	 */
	public static final int MODULE_ITEMMODULE = 3;
	
	/**
	 * Gibt zu den Moduldaten eines Slots auf einem Schiff eine passende Modul-Instanz
	 * zurueck.
	 * @param moduledata Die Moduldaten fuer einen Slot
	 * @return eine Modul-Instanz oder <code>null</code>, falls keine passende Instanz erzeugt werden konnte
	 */
	public static Module getShipModule( Ship.ModuleEntry moduledata ){
		switch( moduledata.moduleType ) {
		case MODULE_CONTAINER_SHIP:
			return new ModuleContainerShip( moduledata.slot, moduledata.data );
		case MODULE_SHIP_PICTURE:
			return new ModuleShipPicture( moduledata.slot, moduledata.data );
		case MODULE_ITEMMODULE:
			return new ModuleItemModule( moduledata.slot, moduledata.data );
		default:
			throw new RuntimeException("Unknown Module >"+moduledata.moduleType+"< in "+moduledata);
		}
	}
}
