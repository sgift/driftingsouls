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

import net.driftingsouls.ds2.server.framework.Context;

import org.apache.commons.lang.StringUtils;

/**
 * Fabrikklasse fuer Itemeffekte.
 * @author Christopher Jung
 *
 */
public class ItemEffectFactory {
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
			return IEAmmo.fromString(effects[1]);
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
		
		throw new Exception("Unbekannter Item-Effekttyp: '"+effects[0]+"'");
		
	}
	
	/**
	 * Laedt einen ItemEffect aus dem angegebenen Context.
	 * @param context Der Context
	 * @return Der Effect
	 */
	public static ItemEffect fromContext(Context context) {
		if( context.getRequest().getParameterString("type").equals("none") ) {
			return new IENone();
		}
		if( context.getRequest().getParameterString("type").equals("draft-ship") ) {
			return IEDraftShip.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("draft-ammo") ) {
			return IEDraftAmmo.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("module") ) {
			return IEModule.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("ammo") ) {
			return IEAmmo.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("disable-ship") ) {
			return IEDisableShip.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("disable-iff") ) {
			return IEDisableIFF.fromContext(context);
		}
		if( context.getRequest().getParameterString("type").equals("module-set-meta") ) {
			return IEModuleSetMeta.fromContext(context);
		}
		
		return new IENone();
	}
}
