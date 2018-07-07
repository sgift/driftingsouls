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

import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item-Effekt "Meta-Effekt fuer Item-Modul-Sets".
 * @author Christopher Jung
 *
 */
public class IEModuleSetMeta extends ItemEffect {
	private String name;
	private Map<Integer,SchiffstypModifikation> combos = new HashMap<>();
	
	public IEModuleSetMeta(String name, Map<Integer,SchiffstypModifikation> combos) {
		super(ItemEffect.Type.MODULE_SET_META);
		
		this.combos.putAll(combos);
		this.name = name;
	}

	/**
	 * Gibt den Namen des Sets zurueck.
	 * @return Der Name des Sets
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gibt die Aenderungseffekte fuer die angegebene Anzahl an Items des Sets zurueck.
	 * Sollten keine Aenderungen vorliegen, wird eine leere Liste zurueckgegeben.
	 * 
	 * @param itemCount Die Anzahl an Items des Sets
	 * @return Die Aenderungseffekte fuer die Anzahl
	 */
	public SchiffstypModifikation[] getCombo(int itemCount) {
		List<SchiffstypModifikation> combos = new ArrayList<>();
		for( int i=1; i <= itemCount; i++ ) {
			SchiffstypModifikation currentCombo = this.combos.get(i);
			if( currentCombo == null ) {
				continue;
			}
			combos.add(currentCombo);
		}
		return combos.toArray(new SchiffstypModifikation[combos.size()]);
	}
	
	/**
	 * Gibt die Liste aller Combos zurueck. Der Index ist die 
	 * Anzahl der jeweils benoetigten Items. Der Effekt der jeweiligen
	 * Stufe bildet sich durch kummulieren der Effekte der vorherigen
	 * Stufen sowie des Effekts der Stufe selbst.
	 * @return Die Combos
	 */
	public Map<Integer,SchiffstypModifikation> getCombos() {
		return Collections.unmodifiableMap(combos);
	}

	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		Map<Integer, SchiffstypModifikation> combos = getCombos();
		String itemstring = "module-set-meta:" + getName() + "&";
		for(int i = 1; i <= 10; i++) {
			if( combos.containsKey(i)) {
				itemstring = itemstring + i + "\\" + combos.get(i).toString() + "&";
			}
		}
		itemstring = itemstring.substring(0, itemstring.length()-1);
		return itemstring;
	}
}
