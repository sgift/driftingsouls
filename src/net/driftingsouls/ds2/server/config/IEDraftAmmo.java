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

import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

/**
 * <h1>Item-Effekt "Ammo-Bauplan"</h1>
 * <p>Der Effekt kann ein allianzweiter Effekt sein. In diesem Fall besitzen alle 
 * Allianzmitglieder automatisch in allen Waffenfabriken diesen Bauplan.</p>
 *  
 * <p>Der Effekt besitzt lediglich das Attribut <code>ammo</code>, welches die ID
 * des zugehoerigen Ammo-Eintrags in der Datenbank enthaelt.</p>
 * 
 * <pre>
 *   &lt;effect type="draft-ammo" ally-effect="true" ammo="14" /&gt;
 * </pre> 
 * @author Christopher Jung
 *
 */
public class IEDraftAmmo extends ItemEffect {
	private int ammoId;
	
	protected IEDraftAmmo(boolean allyEffect, int ammo) {
		super(ItemEffect.Type.DRAFT_AMMO, allyEffect);
		this.ammoId = ammo;
	}
	
	/**
	 * Gibt die zugehoerigen Ammodaten zurueck
	 * @return Die Ammodaten
	 */
	public Ammo getAmmo() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (Ammo)db.get(Ammo.class, this.ammoId);
	}
	
	protected static ItemEffect fromXML(Node effectNode) throws Exception {
		int ammo = (int)XMLUtils.getLongAttribute(effectNode, "ammo");
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Ammo ammoEntry = (Ammo)db.get(Ammo.class, ammo);
		if( ammoEntry == null ) {
			throw new Exception("Illegaler Ammo-Typ '"+ammo+"' im Item-Effekt 'Munitionsbauplan'");
		}
		
		Boolean allyEffect = XMLUtils.getBooleanByXPath(effectNode, "@ally-effect");
		if( allyEffect != null ) {
			return new IEDraftAmmo(allyEffect, ammo);
		}
		return new IEDraftAmmo(false, ammo);
	}
}
