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
package net.driftingsouls.ds2.server.config;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import java.util.Iterator;
import java.util.List;

/**
 * Repraesentiert die Liste aller in DS bekannten Modul-Slots.
 *
 * @author Christopher Jung
 *
 */
public class ModuleSlots implements Iterable<ModuleSlot> {
	private static final Log log = LogFactory.getLog(ModuleSlots.class);
	private static ModuleSlots moduleList = new ModuleSlots();
	
	private ModuleSlots() {
		// EMPTY
	}

	/**
	 * Gibt die Instanz der ModuleSlot-Liste zurueck.
	 * @return Die ModuleSlot-Listen-Instanz
	 */
	public static ModuleSlots get() {
		return moduleList;
	}
	
	@Override
	public Iterator<ModuleSlot> iterator() {
		Session db = ContextMap.getContext().getDB();
		List<ModuleSlot> list = Common.cast(db.createCriteria(ModuleSlot.class).list());
		return list.iterator();
	}
	
	/**
	 * Gibt den Modul-Slot mit der angegebenen ID zurueck.
	 * Falls kein Modul-Slot mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Modul-Slots
	 * @return Der Modul-Slot oder <code>null</code>
	 * @throws NoSuchSlotException Falls der angeforderte Slottyp nicht existiert
	 */
	public ModuleSlot slot( String id ) throws NoSuchSlotException {
		Session db = ContextMap.getContext().getDB();
		ModuleSlot slot = (ModuleSlot) db.get(ModuleSlot.class, id);
		if( slot == null ) {
			throw new NoSuchSlotException(id);
		}
		return slot;
	}
}
