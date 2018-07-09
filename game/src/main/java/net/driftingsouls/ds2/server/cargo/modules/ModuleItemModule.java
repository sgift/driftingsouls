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

import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.ResourceID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Schiffsmodul;
import net.driftingsouls.ds2.server.config.items.SchiffsmodulSet;
import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repraesentiert ein Modul auf Item-Basis.
 * @author Christopher Jung
 *
 */
public class ModuleItemModule extends Module {
	private static final Log log = LogFactory.getLog(ModuleItemModule.class);

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
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Schiffsmodul item = (Schiffsmodul)db.get(Schiffsmodul.class, this.itemid);
		if( item == null ) {
			log.warn("WARNUNG: Ungueltiges Schiffsmodul ("+this.itemid+")");
			return stats;
		}
		IEModule effect = item.getEffect();

		stats = effect.getMods().applyTo(stats, this.weaponrepl);

		if( calculateSetEffect && (effect.getSet() != null) ) {
			// Set-Item-Count ausrechnen
			List<Integer> gotitems = new ArrayList<>();
			int count = 0;

			for( Module moduleobj : moduleobjlist ) {
				if( !(moduleobj instanceof ModuleItemModule) ) {
					continue;
				}
				ModuleItemModule itemModule = (ModuleItemModule)moduleobj;

				Schiffsmodul moduleItem = (Schiffsmodul)db.get(Schiffsmodul.class, itemModule.getItemID().getItemID());

				if( moduleItem.getEffect().getSet() != effect.getSet() ) {
					continue;
				}

				itemModule.disableSetEffect();

				if( gotitems.contains(itemModule.getItemID().getItemID()) ) {
					continue;
				}

				gotitems.add(itemModule.getItemID().getItemID());
				count++;
			}

			SchiffsmodulSet effectSet = effect.getSet();
			SchiffstypModifikation[] mods = effectSet.getEffect().getCombo(count);
			for (SchiffstypModifikation mod : mods)
			{
				stats = mod.applyTo(stats, this.weaponrepl);
			}
		}

		return stats;
	}

	@Override
	public boolean isSame(ModuleEntry entry) {
		if( entry.getSlot() != this.slot ) {
			return false;
		}
		else if( entry.getModuleType() != ModuleType.ITEMMODULE ) {
			return false;
		}

		return this.itemid == Integer.parseInt(entry.getData());

	}

	@Override
	public void setSlotData(String data) {
		this.weaponrepl = StringUtils.split(data,',');
	}

	@Override
	public String getName() {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Item item = (Item)db.get(Item.class, this.itemid);
		if(item == null) {
			return "Noname";
		}

		return "<a class=\"forschinfo\" href=\"./ds?module=iteminfo&amp;action=details&amp;item="+this.itemid+"\">"+item.getName()+"</a>";
	}

	/**
	 * Gibt die ID des zum ItemModul gehoerenden Items zurueck.
	 * @return die Item-ID des Moduls
	 */
	public ResourceID getItemID() {
		return new ItemID(itemid, 0, 0);
	}

	/**
	 * Deaktiviert die Set-Effekte des Moduls.
	 *
	 */
	public void disableSetEffect() {
		this.calculateSetEffect = false;
	}
}
