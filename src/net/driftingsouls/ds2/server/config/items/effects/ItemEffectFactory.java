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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Fabrikklasse fuer Itemeffekte.
 * @author Christopher Jung
 *
 */
public class ItemEffectFactory {
	private static final Logger LOG = LogManager.getLogger(ItemEffectFactory.class);

	private ItemEffectFactory() {
		// EMPTY
	}
	
	/**
	 * Erzeugt aus einem ItemEffectString einen ItemEffect.
	 * @param itemEffectString Der String in Form "typ|effectvalue1;effectvalue2"
	 * @return Der daraus erzeugte Itemeffect
	 * @throws Exception Exception falls der ItemEffectString ungueltig ist
	 */
	public static ItemEffect fromString(String itemEffectString) throws Exception {
		if( itemEffectString == null || itemEffectString.equals("")) {
			return new IENone();
		}
		String[] effects = StringUtils.split(itemEffectString, ":");
		if( effects[0] == null || effects[0].equals("")) {
			return new IENone();
		}

		try
		{
			if( effects[0].equals("draft-ship")) {
				return IEDraftShip.fromString(effects[1]);
			}
			if( effects[0].equals("draft-ammo") ) {
				return IEDraftAmmo.fromString(effects[1]);
			}
			if( effects[0].equals("module") ) {
				return IEModule.fromString(effects[1]);
			}
			if( effects[0].equals("ammo") ) {
				throw new UnsupportedOperationException();
			}
			if( effects[0].equals("disable-ship") ) {
				return IEDisableShip.fromString(effects[1]);
			}
			if( effects[0].equals("disable-iff") ) {
				return IEDisableIFF.fromString(effects[1]);
			}
			if( effects[0].equals("module-set-meta") ) {
				return IEModuleSetMeta.fromString(effects[1]);
			}
		}
		catch(RuntimeException e)
		{
			LOG.error("Konnte Effekt-String '"+itemEffectString+"' nicht parsen. Fallback auf 'Kein Effekt'. Grund: "+e.getMessage());
			return new IENone();
		}
		
		throw new Exception("Unbekannter Item-Effekttyp: '"+effects[0]+"'");
		
	}
}
