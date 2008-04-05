/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Node;

/**
 * Fabrikklasse fuer Itemeffekte
 * @author Christopher Jung
 *
 */
public class ItemEffectFactory {
	private ItemEffectFactory() {
		// EMPTY
	}
	
	/**
	 * Erzeugt aus einem XML-Fragment den passenden Itemeffekt
	 * @param effectNode Das XML-Fragment
	 * @return Der Effekt
	 * @throws Exception Falls das Fragment ungueltig ist
	 */
	public static ItemEffect fromXML(Node effectNode) throws Exception {
		if( effectNode == null ) {
			return new IENone();
		}
		String type = XMLUtils.getStringAttribute(effectNode, "type");
		if( type == null || "".equals(type) ) {
			return new IENone();
		}
		
		if( type.equals("draft-ship") ) {
			return IEDraftShip.fromXML(effectNode);
		}
		if( type.equals("draft-ammo") ) {
			return IEDraftAmmo.fromXML(effectNode);
		}
		if( type.equals("module") ) {
			return IEModule.fromXML(effectNode);
		}
		if( type.equals("ammo") ) {
			return IEAmmo.fromXML(effectNode);
		}
		if( type.equals("disable-ship") ) {
			return IEDisableShip.fromXML(effectNode);
		}
		if( type.equals("disable-iff") ) {
			return IEDisableIFF.fromXML(effectNode);
		}
		if( type.equals("module-set-meta") ) {
			return IEModuleSetMeta.fromXML(effectNode);
		}
		
		throw new Exception("Unbekannter Item-Effekttyp: '"+type+"'");
	}
}
