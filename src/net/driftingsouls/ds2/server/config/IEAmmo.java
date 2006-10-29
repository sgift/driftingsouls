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

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

/**
 * <h1>Item-Effekt "Ammo"</h1>
 * Repraesentiert eine Munitionseinheit als Wareneinheit innerhalb von Drifting Souls.<br>
 * Der Effekt besitzt lediglich das Attribut <code>ammo</code>, welches die ID
 * des zugehoerigen Ammo-Eintrags in der Datenbank enthaelt.
 * 
 * <pre>
 *   &lt;effect type="ammo" ammo="14" /&gt;
 * </pre> 
 * @author Christopher Jung
 *
 */
public class IEAmmo extends ItemEffect {
	private int ammoId;
	
	protected IEAmmo(int ammoid) {
		super(ItemEffect.Type.AMMO);
		this.ammoId = ammoid;
	}
	
	/**
	 * Gibt die ID des zugehoerigen Ammo-Datenbankeintrags zurueck
	 * @return Die Ammo-ID
	 */
	public int getAmmoID() {
		return ammoId;
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		int ammo = XMLUtils.getNumberByXPath(effectNode, "@ammo").intValue();
		
		return new IEAmmo(ammo);
	}
}
