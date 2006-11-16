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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

/**
 * Repraesentiert die Liste aller in DS bekannten Modul-Slots.
 * Die Liste wird beim Start von DS aus der <code>moduleslots.xml</code>
 * geladen
 * 
 * @author Christopher Jung
 *
 */
public class ModuleSlots implements Loggable,Iterable<ModuleSlot> {
	private static ModuleSlots moduleList = new ModuleSlots();
	private Map<String, ModuleSlot> list = new HashMap<String, ModuleSlot>();
	
	private ModuleSlots() {
		// EMPTY
	}

	private void addModuleSlot( ModuleSlot mslot ) {
		list.put(mslot.getSlotType(), mslot);
	}
	
	/**
	 * Gibt die Instanz der ModuleSlot-Liste zurueck
	 * @return Die ModuleSlot-Listen-Instanz
	 */
	public static ModuleSlots get() {
		return moduleList;
	}
	
	public Iterator<ModuleSlot> iterator() {
		return list.values().iterator();
	}
	
	/**
	 * Gibt den Modul-Slot mit der angegebenen ID zurueck.
	 * Falls kein Modul-Slot mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Modul-Slots
	 * @return Der Modul-Slot oder <code>null</code>
	 */
	public ModuleSlot slot( String id ) {
		return list.get(id);
	}
	
	static {
		/*
		 * items.xml parsen
		 */
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"moduleslots.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "slots/slot");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				String id = XMLUtils.getStringAttribute(node, "id");
				String name = XMLUtils.getStringAttribute(node, "name");
				if( id == null || name == null ) {
					throw new Exception("Jeder Slot muss ueber eine ID und einen Namen verfuegen");
				}
				
				String parent = XMLUtils.getStringAttribute(node, "parent");
				
				ModuleSlot slot = new ModuleSlot(id, name, parent);
				
				moduleList.addModuleSlot(slot);
			}
		}
		catch( Exception e ) {
			LOG.fatal("FAILED: Kann Items nicht laden",e);
		}
	}
}
