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
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Repraesentiert einen gedockten Frachtcontainer in DS.
 * @author Christopher Jung
 *
 */
public class ModuleContainerShip extends Module {
	private int slot;
	private long cargo;
	private int shipid;

	protected ModuleContainerShip(int slot, String data) {
		this.slot = slot;
		String[] owndata = StringUtils.split(data, '_');
		this.cargo = Long.parseLong(owndata[1]);
		this.shipid = Integer.parseInt(owndata[0]);
	}

	@Override
	public boolean isSame( ModuleEntry entry ) {
		if( entry.getSlot() != this.slot ) {
			return false;
		}
		else if( entry.getModuleType() != ModuleType.CONTAINER_SHIP ) {
			return false;
		}
		String[] dataArray = StringUtils.split(entry.getData(), '_');

		return Integer.valueOf(dataArray[0]) == this.shipid;
	}

	@Override
	public String getName() {
		return "Schiffscontainer";
	}

	@Override
	public ShipTypeData modifyStats(ShipTypeData stats, List<Module> moduleobjlist) {
		return new ShipTypeDataCargoWrapper(stats, this.cargo);
	}

	@Override
	public void setSlotData(String data) {
		// EMPTY
	}

	private static class ShipTypeDataCargoWrapper extends AbstractShipTypeDataWrapper {
		private long cargo;

		ShipTypeDataCargoWrapper(ShipTypeData inner, long cargo) {
			super(inner);
			this.cargo = cargo;
		}

		@Override
		public long getCargo() {
			return this.getInner().getCargo()+this.cargo;
		}
	}
}
