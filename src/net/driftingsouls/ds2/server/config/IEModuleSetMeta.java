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
import java.util.Map;

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.ships.ShipTypeDataChangeset;
import net.driftingsouls.ds2.server.ships.Ships;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Item-Effekt "Meta-Effekt fuer Item-Modul-Sets"
 * @author Christopher Jung
 *
 */
public class IEModuleSetMeta extends ItemEffect {
	private String name;
	private Map<Integer,ShipTypeDataChangeset> combos = new HashMap<Integer,ShipTypeDataChangeset>();
	
	protected IEModuleSetMeta(String name) {
		super(ItemEffect.Type.MODULE_SET_META);
		
		this.name = name;
	}
	
	protected void addCombo(int itemCount, ShipTypeDataChangeset combo) {
		if( itemCount < 1 ) {
			throw new IndexOutOfBoundsException("Die Anzahl der Items muss groesser oder gleich 1 sein!");
		}
		combos.put(itemCount, combo);
	}
	
	/**
	 * Gibt den Namen des Sets zurueck
	 * @return Der Name des Sets
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gibt den Aenderungseffekt fuer die angegebene Anzahl an Items des Sets zurueck.
	 * Aenderungseffekte, welche bei einer niederigeren Anzahl an Items eintreten, werden 
	 * eingerechnet.
	 * Sollten keine Aenderungen vorliegen, wird <code>null</code> zurueckgegeben.
	 * @param itemCount Die Anzahl an Items des Sets
	 * @return Die kummulierten Aenderungseffekte fuer die Anzahl
	 */
	public ShipTypeDataChangeset getCombo(int itemCount) {
		ShipTypeDataChangeset combo = null;
		for( int i=1; i <= itemCount; i++ ) {
			ShipTypeDataChangeset currentCombo = combos.get(i);
			if( currentCombo == null ) {
				continue;
			}
			if( combo == null ) {
				combo = currentCombo;
			}
			else {
				currentCombo.setBase(combo);
				combo = currentCombo;
			}
		}
		return combo;
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		String name = XMLUtils.getStringByXPath(effectNode, "name/text()");
		
		IEModuleSetMeta effect = new IEModuleSetMeta(name);
		NodeList nodes = XMLUtils.getNodesByXPath(effectNode, "combo");
		for( int i=0, length=nodes.getLength(); i < length; i++ ) {
			int count = XMLUtils.getNumberByXPath(nodes.item(i), "@item-count").intValue();
			ShipTypeDataChangeset combo = Ships.getTypeChangeSetFromXML(nodes.item(i));
			effect.addCombo(count, combo);
		}
		
		return effect;
	}
}
