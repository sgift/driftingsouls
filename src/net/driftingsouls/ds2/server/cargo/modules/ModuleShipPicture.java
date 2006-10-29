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

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Repraesentiert ein Modul, welches das Bild eines Schiffes veraendert
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
	public boolean isSame( int slot, int moduleid, String data ) {
		if( slot != this.slot ) {
			return false;	
		}	
		else if( moduleid != Modules.MODULE_SHIP_PICTURE ) {
			return false;	
		}
		
		if( this.picture != data ) {
			return false;	
		}
		return true;
	}
	
	@Override
	public String getName() { 
		return "Schiffsbild"; 
	}
	
	@Override
	public SQLResultRow modifyStats(SQLResultRow stats, SQLResultRow typestats,
			List<Module> moduleobjlist) {
		stats.put("picture", picture);	
		
		return stats;
	}

	@Override
	public void setSlotData(String data) {
		// EMPTY
	}

}
