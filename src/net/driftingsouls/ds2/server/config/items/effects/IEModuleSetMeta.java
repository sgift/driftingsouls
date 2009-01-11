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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import net.driftingsouls.ds2.server.ships.ShipTypeChangeset;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Item-Effekt "Meta-Effekt fuer Item-Modul-Sets".
 * @author Christopher Jung
 *
 */
public class IEModuleSetMeta extends ItemEffect {
	private String name;
	private Map<Integer,ShipTypeChangeset> combos = new HashMap<Integer,ShipTypeChangeset>();
	
	protected IEModuleSetMeta(String name) {
		super(ItemEffect.Type.MODULE_SET_META);
		
		this.name = name;
	}
	
	protected void addCombo(int itemCount, ShipTypeChangeset combo) {
		if( itemCount < 1 ) {
			throw new IndexOutOfBoundsException("Die Anzahl der Items muss groesser oder gleich 1 sein!");
		}
		combos.put(itemCount, combo);
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
	public ShipTypeChangeset[] getCombo(int itemCount) {
		List<ShipTypeChangeset> combos = new ArrayList<ShipTypeChangeset>();
		for( int i=1; i <= itemCount; i++ ) {
			ShipTypeChangeset currentCombo = this.combos.get(i);
			if( currentCombo == null ) {
				continue;
			}
			combos.add(currentCombo);
		}
		return combos.toArray(new ShipTypeChangeset[combos.size()]);
	}
	
	/**
	 * Gibt die Liste aller Combos zurueck. Der Index ist die 
	 * Anzahl der jeweils benoetigten Items. Der Effekt der jeweiligen
	 * Stufe bildet sich durch kummulieren der Effekte der vorherigen
	 * Stufen sowie des Effekts der Stufe selbst.
	 * @return Die Combos
	 */
	public Map<Integer,ShipTypeChangeset> getCombos() {
		return Collections.unmodifiableMap(combos);
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		String name = XMLUtils.getStringByXPath(effectNode, "name/text()");
		
		IEModuleSetMeta effect = new IEModuleSetMeta(name);
		NodeList nodes = XMLUtils.getNodesByXPath(effectNode, "combo");
		for( int i=0, length=nodes.getLength(); i < length; i++ ) {
			int count = XMLUtils.getNumberByXPath(nodes.item(i), "@item-count").intValue();
			ShipTypeChangeset combo = new ShipTypeChangeset(nodes.item(i));
			effect.addCombo(count, combo);
		}
		
		return effect;
	}
}
