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

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.IEModuleSetMeta;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.ships.ShipTypeChangeset;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.lang.StringUtils;

/**
 * Repraesentiert ein Modul auf Item-Basis
 * @author Christopher Jung
 *
 */
public class ModuleItemModule extends Module implements Loggable {
	private int slot;
	private int itemid;
	private String[] weaponrepl;
	private boolean calculateSetEffect;
	
	protected ModuleItemModule( int slot, String data ) {
		this.slot = slot;

		this.itemid = Integer.parseInt(data);
		this.weaponrepl = new String[0];
		this.calculateSetEffect = true;
	}
	
	@Override
	public ShipTypeData modifyStats(ShipTypeData stats, List<Module> moduleobjlist) {
		ItemEffect itemEffect = Items.get().item(this.itemid).getEffect();
		if( itemEffect.getType() != ItemEffect.Type.MODULE ) {
			LOG.warn("WARNUNG: Ungueltiger Itemeffect in CModuleItemModule ("+this.itemid+")<br />\n");
			return stats;
		}
		IEModule effect = (IEModule)itemEffect;
		
		stats = effect.getMods().applyTo(stats, this.weaponrepl);
		
		if( calculateSetEffect && (effect.getSetID() != 0) ) {
			// Set-Item-Count ausrechnen
			List<Integer> gotitems = new ArrayList<Integer>();
			int count = 0;
			
			for( Module moduleobj : moduleobjlist ) {
				if( !(moduleobj instanceof ModuleItemModule) ) {
					continue;
				}
				ModuleItemModule itemModule = (ModuleItemModule)moduleobj;
				
				if( ((IEModule)Items.get().item(itemModule.getItemID().getItemID()).getEffect()).getSetID() != effect.getSetID() ) {
					continue;
				}
				
				itemModule.disableSetEffect();
				
				if( gotitems.contains(itemModule.getItemID().getItemID()) ) {
					continue;
				}
				
				gotitems.add(itemModule.getItemID().getItemID());
				count++;
			}
			
			ShipTypeChangeset[] mods = ((IEModuleSetMeta)Items.get().item(effect.getSetID()).getEffect()).getCombo(count);
			for( int i=0; i < mods.length; i++ ) {
				stats = mods[i].applyTo(stats, this.weaponrepl);
			}
		}
				
		return stats;
	}

	@Override
	public boolean isSame(int slot, int moduleid, String data) {
		if( slot != this.slot ) {
			return false;	
		}	
		else if( moduleid != Modules.MODULE_ITEMMODULE ) {
			return false;	
		}
				
		if( this.itemid != Integer.parseInt(data) ) {
			return false;	
		}
		
		return true;
	}

	@Override
	public void setSlotData(String data) {
		this.weaponrepl = StringUtils.split(data,',');
	}
	
	@Override
	public String getName() {
		return "<a class=\"forschinfo\" href=\"./ds?module=iteminfo&amp;action=details&amp;item="+this.itemid+"\">"+Items.get().item(this.itemid).getName()+"</a>";
	}
	
	/**
	 * Gibt die ID des zum ItemModul gehoerenden Items zurueck
	 * @return die Item-ID des Moduls
	 */
	public ResourceID getItemID() {
		return new ItemID(itemid, 0, 0);	
	}

	/**
	 * Deaktiviert die Set-Effekte des Moduls
	 *
	 */
	public void disableSetEffect() {
		this.calculateSetEffect = false;
	}
}
