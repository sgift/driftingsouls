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

import net.driftingsouls.ds2.server.ships.ShipType;

/**
 * Item-Effekt "Schiffsbauplan deaktivieren".
 * Deaktiviert die Moeglichkeit in einer Werft einen bestimmten Schiffstyp zu bauen.
 * <p>Der Effekt kann ein allianzweiter Effekt sein. In diesem Fall verfuegen alle 
 * Allianzmitglieder nicht mehr ueber die Moeglichkeit dieses Schiff zu bauen.</p>
 * Der Effekt verfuegt nur ueber das Attribut "shiptype", welches die Typen-ID des nicht mehr
 * baubaren Schiffes enthaelt.
 * 
 * <pre>
 *   &lt;effect type="disable-ship" ally-effect="true" shiptype="14" /&gt;
 * </pre> 
 * 
 * @author Christopher Jung
 *
 */
public class IEDisableShip extends ItemEffect {
	private ShipType shipType;
	
	public IEDisableShip(boolean allyEffect, ShipType shiptype) {
		super(ItemEffect.Type.DISABLE_SHIP, allyEffect);
		this.shipType = shiptype;
	}
	
	/**
	 * Gibt die Typen-ID des durch diesen Effekt deaktivierten Schifftyps zurueck.
	 * @return Die Schiffstypen-ID
	 */
	public ShipType getShipType() {
		return shipType;
	}

	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		return "disable-ship:" + shipType.getId() + "&" + hasAllyEffect();
	}
}
