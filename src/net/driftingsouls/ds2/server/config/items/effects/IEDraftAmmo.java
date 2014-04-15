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

import java.io.IOException;

import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang.StringUtils;

/**
 * <h1>Item-Effekt "Ammo-Bauplan".</h1>
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
	 * Gibt die zugehoerigen Ammodaten zurueck.
	 * @return Die Ammodaten
	 */
	public Ammo getAmmo() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		return (Ammo)db.get(Ammo.class, this.ammoId);
	}
	
	/**
	 * Laedt einen Effect aus einem String.
	 * @param effectString Der Effect als String
	 * @return Der Effect
	 * @throws IllegalArgumentException falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String effectString) throws IllegalArgumentException {
		
		String[] effects = StringUtils.split(effectString, "&");
		int ammo = Integer.parseInt(effects[0]);
		Boolean allyEffect = effects[1].equals("true");
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Ammo ammoEntry = (Ammo)db.get(Ammo.class, ammo);
		if( ammoEntry == null ) {
			throw new IllegalArgumentException("Illegaler Ammo-Typ '"+ammo+"' im Item-Effekt 'Munitionsbauplan'");
		}
		
		if( allyEffect ) {
			return new IEDraftAmmo(allyEffect, ammo);
		}
		return new IEDraftAmmo(false, ammo);
	}
	
	/**
	 * Laedt einen ItemEffect aus einem Context.
	 * @param context Der Context
	 * @return Der Effect
	 */
	public static ItemEffect fromContext(Context context) {
		
		int ammoid = context.getRequest().getParameterInt("ammoid");
		Boolean allyEffect = context.getRequest().getParameterString("allyeffect").equals("true");
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		Ammo ammoEntry = (Ammo)db.get(Ammo.class, ammoid);
		if( ammoEntry == null) {
			return new IENone();
		}
		if(allyEffect) {
			return new IEDraftAmmo(allyEffect, ammoid);
		}
		return new IEDraftAmmo(false,ammoid);
		
	}
	
	/**
	 * Gibt das passende Fenster fuer das Adminmenue aus.
	 * @param echo Der Writer des Adminmenues
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	@Override
	public void getAdminTool(StringBuilder echo) throws IOException {
		
		echo.append("<input type=\"hidden\" name=\"type\" value=\"draft-ammo\" >");
		echo.append("<tr><td class=\"noBorderS\">AmmoId: </td><td><input type=\"text\" name=\"ammoid\" value=\"" + ammoId + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Ally-Effekt (true/false): </td><td><input type=\"text\" name=\"allyeffect\" value=\"" + hasAllyEffect() + "\"></td></tr>\n");
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		String itemstring = "draft-ammo:" + ammoId + "&" + hasAllyEffect();
		return itemstring;
	}
}
