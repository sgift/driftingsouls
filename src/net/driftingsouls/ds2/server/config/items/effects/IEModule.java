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
package net.driftingsouls.ds2.server.config.items.effects;

import net.driftingsouls.ds2.server.config.items.SchiffsmodulSet;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;

import java.util.Collections;
import java.util.Set;

/**
 * Item-Effekt "Modul".
 * @author Christopher Jung
 *
 */
public class IEModule extends ItemEffect {
	private Set<String> slots;
	private SchiffsmodulSet set;
	private SchiffstypModifikation mods;
	
	public IEModule(Set<String> slots, SchiffstypModifikation mods, SchiffsmodulSet set) {
		super(ItemEffect.Type.MODULE);
		
		this.slots = Collections.unmodifiableSet(slots);
		this.mods = mods;
		this.set = set;
	}
	
	/**
	 * Gibt die Liste der Slots zurueck, in die das Modul hinein passt.
	 * @return die Liste der Slots
	 */
	public Set<String> getSlots() {
		return slots;
	}
	
	/**
	 * Gibt Set-Item des zugehoerigen Sets zurueck oder <code>null</code>, falls
	 * das Modul zu keinem Set gehoert.
	 * @return Das Set-Item oder <code>null</code>
	 */
	public SchiffsmodulSet getSet() {
		return set;
	}
	
	/**
	 * Gibt das Aenderungsobjekt mit den durch das Modul geaenderten Schiffstypendaten zurueck.
	 * @return Das Aenderungsobjekt
	 */
	public SchiffstypModifikation getMods() {
		return mods;
	}

	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		return "module:" + Common.implode(";", getSlots()) + "&" + (getSet() != null ? getSet().getID() : "-1") + "&" + getMods().toString();
	}
}
