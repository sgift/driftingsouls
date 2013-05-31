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

import java.util.List;

/**
 * Repraesentiert einen Modulslot-Typ in DS.
 * Ein Modulslot hat einen Slottypen (der ID), einen Namen (der Beschreibung) und (optional)
 * einem Elternslot (Gegenstaende, die in den Elternslot passen, passen
 * auch in diesen Slot).
 * @author Christopher Jung
 *
 */
public class ModuleSlot {
	private String slottype = null;
	private String name = null;
	private String parent = null;

	protected ModuleSlot( String slottype, String name, String parent ) {
		this.slottype = slottype;
		this.name = name;
		this.parent = parent;
	}
	
	/**
	 * Gibt die Typen-ID des Slots zurueck.
	 * @return Die Typen-ID des Slots
	 */
	public String getSlotType() {
		return slottype;
	}
	
	/**
	 * Gibt den Namen/Beschreibung des Slots zurueck.
	 * @return der Name/Beschreibung
	 */
	public String getName() {
		return name;	
	}
	
	/**
	 * Prueft, ob der Slot kompatibel (d.h., dass Module, die in diesen Slot passen,
	 * in einen anderen Slot passen) zu einem der aufgelisteten Slots ist.
	 * @param slottype Eine Liste von Slot-IDs
	 * @return <code>true</code>, falls einer der aufgelisteten Slots kompatibel ist
	 */
	public boolean isMemberIn( List<String> slottype ) {
		return isMemberIn(slottype.toArray(new String[slottype.size()]), "or");
	}
	
	/**
	 * Prueft, ob der Slot kompatibel (d.h., dass Module, die in diesen Slot passen,
	 * in einen anderen Slot passen) zu einem der aufgelisteten Slots ist.
	 * @param slottype Eine Liste von Slot-IDs
	 * @return <code>true</code>, falls einer der aufgelisteten Slots kompatibel ist
	 */
	public boolean isMemberIn( String[] slottype ) {
		return isMemberIn(slottype, "or");
	}
	
	/**
	 * Prueft, ob der Slot kompatibel (d.h., dass Module, die in diesen Slot passen,
	 * in einen anderen Slot passen) zu einem oder zu allen der aufgelisteten Slots ist.
	 * @param slottype die Liste der Slots
	 * @param cmp <code>"and"</code>, falls alle Slots kompatibel sein sollen. Andernfalls <code>"or"</code>
	 * @return <code>true</code> falls einer/alle Slots kompatibel sind.
	 */
	public boolean isMemberIn( String[] slottype, String cmp ) {
		if( slottype.length > 1 ) {
			for (String aSlottype : slottype)
			{
				boolean result = isMemberIn(new String[]{aSlottype});
				if (result && cmp.equals("and"))
				{
					return false;
				}
				else if (result && cmp.equals("or"))
				{
					return true;
				}
			}
			if( cmp.equals("and") ) {
				return true;	
			}
			else if( cmp.equals("or") ) {
				return false;
			}
		}
		
		if( this.slottype.equals(slottype[0]) ) {
			return true;
		}

		if( (parent != null) && (parent.length() > 0) ) {
			return ModuleSlots.get().slot(parent).isMemberIn( slottype );
		}
		return false;
	}
}
