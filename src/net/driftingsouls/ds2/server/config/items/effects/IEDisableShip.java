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
import java.io.Writer;

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.apache.commons.lang.StringUtils;

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
	private int shipType;
	
	protected IEDisableShip(boolean allyEffect, int shiptype) {
		super(ItemEffect.Type.DISABLE_SHIP, allyEffect);
		this.shipType = shiptype;
	}
	
	/**
	 * Gibt die Typen-ID des durch diesen Effekt deaktivierten Schifftyps zurueck.
	 * @return Die Schiffstypen-ID
	 */
	public int getShipType() {
		return shipType;
	}
	
	/**
	 * Laedt einen Effect aus einem String.
	 * @param effectString Der Effect als String
	 * @return Der Effect
	 * @throws Exception falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String effectString) throws Exception {
		
		String[] effects = StringUtils.split(effectString, "&");
		Boolean allyEffect = effects[1].equals("true") ? true : false;
		int shiptype = Integer.parseInt(effects[0]);
		
		org.hibernate.Session db = ContextMap.getContext().getDB();
		ShipType shipType = (ShipType)db.get(ShipType.class, shiptype);
		if( shipType == null ) {
			throw new Exception("Illegaler Schiffstyp '"+shiptype+"' im Item-Effekt 'Schiffsbauplan deaktivieren'");
		}
		
		if( allyEffect ) {
			return new IEDisableShip(allyEffect, shiptype);
		}
		return new IEDisableShip(false, shiptype);
	}
	
	/**
	 * Laedt einen Effect aus dem angegebenen Context.
	 * @param context der context
	 * @return der Effect
	 */
	public static ItemEffect fromContext(Context context) {
		
		int shiptype = context.getRequest().getParameterInt("shiptype");
		boolean allyeffect = context.getRequest().getParameterString("allyeffect").equals("true") ? true : false;
		
		if( allyeffect ) {
			return new IEDisableShip(allyeffect, shiptype);
		}
		return new IEDisableShip(false, shiptype);
	}
	
	/**
	 * Gibt das passende Fenster fuer das Adminmenue aus.
	 * @param echo Der Writer des Adminmenues
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	public void getAdminTool(Writer echo) throws IOException {
		
		echo.append("<input type=\"hidden\" name=\"type\" value=\"disable-ship\" >");
		echo.append("<tr><td class=\"noBorderS\">SchiffsId: </td><td><input type=\"text\" name=\"shiptype\" value=\"" + getShipType() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">AllyEffect (true/false): </td><td><input type=\"text\" name=\"allyeffect\" value=\"" + hasAllyEffect() + "\"></td></tr>\n");
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	public String toString() {
		return "disable-ship:" + getShipType() + "&" + hasAllyEffect();
	}
}
