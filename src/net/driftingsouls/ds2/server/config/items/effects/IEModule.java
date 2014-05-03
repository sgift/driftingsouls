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

import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.SchiffstypModifikation;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Item-Effekt "Modul".
 * @author Christopher Jung
 *
 */
public class IEModule extends ItemEffect {
	private Set<String> slots;
	private int set;
	private SchiffstypModifikation mods;
	
	public IEModule(Set<String> slots, SchiffstypModifikation mods, int set) {
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
	 * Gibt die ID des zugehoerigen Sets zurueck oder <code>-1</code>, falls
	 * das Modul zu keinem Set gehoert.
	 * @return Die Set-ID oder <code>-1</code>
	 */
	public int getSetID() {
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
	 * Laedt einen Effect aus einem String.
	 * @param effectString Der Effect als String
	 * @return Der Effect
	 * @throws IllegalArgumentException falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String effectString) throws IllegalArgumentException {
		Set<String> slots = new HashSet<>();
		String[] effects = StringUtils.split(effectString, "&");
		String[] theslots = StringUtils.split(effects[0], ";");
		for (String theslot : theslots)
		{

			// Sicherstellen, dass der Slot existiert
			// sonst -> NoSuchSlotException
			ModuleSlots.get().slot(theslot);

			slots.add(theslot);
		}
		
		int setId = Integer.parseInt(effects[1]);
		
		SchiffstypModifikation mods = new SchiffstypModifikation(effects[2]);
		
		return new IEModule(slots, mods, setId);
	}

	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		return "module:" + Common.implode(";", getSlots()) + "&" + getSetID() + "&" + getMods().toString();
	}
}
