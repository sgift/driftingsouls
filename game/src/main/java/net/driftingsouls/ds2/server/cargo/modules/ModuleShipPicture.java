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

import net.driftingsouls.ds2.server.ships.AbstractShipTypeDataWrapper;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.util.List;

/**
 * Repraesentiert ein Modul, welches das Bild eines Schiffes veraendert.
 * @author Christopher Jung
 *
 */
public class ModuleShipPicture extends Module {
	private int slot;
	private String picture;

	protected ModuleShipPicture( int slot, String data ) {
		this.slot = slot;
		this.picture = data;
	}

	@Override
	public boolean isSame( ModuleEntry entry ) {
		if( entry.getSlot() != this.slot ) {
			return false;
		}
		else if( entry.getModuleType() != ModuleType.SHIP_PICTURE ) {
			return false;
		}

		return this.picture.equals(entry.getData());
	}

	@Override
	public String getName() {
		return "Schiffsbild";
	}

	@Override
	public ShipTypeData modifyStats(ShipTypeData stats, List<Module> moduleobjlist) {
		return new ShipTypeDataPictureWrapper(stats, this.picture);
	}

	@Override
	public void setSlotData(String data) {
		// EMPTY
	}

	private static class ShipTypeDataPictureWrapper extends AbstractShipTypeDataWrapper {
		private String picture;

		ShipTypeDataPictureWrapper(ShipTypeData inner, String picture) {
			super(inner);
			this.picture = picture;
		}

		@Override
		public String getPicture() {
			return this.picture;
		}
	}
}
