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

import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert einen gedockten Frachtcontainer in DS
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
	public boolean isSame( int slot, int moduleid, String data ) {
		if( slot != this.slot ) {
			return false;	
		}	
		else if( moduleid != Modules.MODULE_CONTAINER_SHIP ) {
			return false;	
		}
		String[] dataArray = StringUtils.split(data, '_');

		if( Integer.parseInt(dataArray[0]) != this.shipid ) {
			return false;	
		}
		return true;
	}
	
	@Override
	public String getName() { 
		return "Schiffscontainer"; 
	}
	
	@Override
	public SQLResultRow modifyStats(SQLResultRow stats, SQLResultRow typestats,
			List<Module> moduleobjlist) {
		stats.put("cargo", stats.getLong("cargo")+this.cargo);	
		return stats;
	}

	@Override
	public void setSlotData(String data) {
		// EMPTY
	}

}
