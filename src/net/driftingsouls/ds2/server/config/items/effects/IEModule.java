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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.driftingsouls.ds2.server.config.ModuleSlots;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.ships.ShipTypeChangeset;

import org.apache.commons.lang.StringUtils;

/**
 * Item-Effekt "Modul".
 * @author Christopher Jung
 *
 */
public class IEModule extends ItemEffect {
	private List<String> slots;
	private int set;
	private ShipTypeChangeset mods;
	
	protected IEModule(List<String> slots, ShipTypeChangeset mods, int set) {
		super(ItemEffect.Type.MODULE);
		
		this.slots = Collections.unmodifiableList(slots);
		this.mods = mods;
		this.set = set;
	}
	
	/**
	 * Gibt die Liste der Slots zurueck, in die das Modul hinein passt.
	 * @return die Liste der Slots
	 */
	public List<String> getSlots() {
		return slots;
	}
	
	/**
	 * Gibt die ID des zugehoerigen Sets zurueck oder <code>-1</code>, falls
	 * das Modul zu keinem Set gehoert.
	 * @return Die Set-ID oder <code>-1</code>
	 */
	public int getSetID() {
		return set;
	}
	
	/**
	 * Gibt das Aenderungsobjekt mit den durch das Modul geaenderten Schiffstypendaten zurueck.
	 * @return Das Aenderungsobjekt
	 */
	public ShipTypeChangeset getMods() {
		return mods;
	}
	
	/**
	 * Laedt einen Effect aus einem String.
	 * @param effectString Der Effect als String
	 * @return Der Effect
	 * @throws Exception falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String effectString) throws Exception {
		List<String> slots = new ArrayList<String>();
		String[] effects = StringUtils.split(effectString, "&");
		String[] theslots = StringUtils.split(effects[0], ";");
		for( int i=0; i < theslots.length; i++) {
			
			// Sicherstellen, dass der Slot existiert
			// sonst -> NoSuchSlotException
			ModuleSlots.get().slot(theslots[i]);
			
			slots.add(theslots[i]);
		}
		
		int setId = Integer.parseInt(effects[1]);
		
		ShipTypeChangeset mods = new ShipTypeChangeset(effects[2]);
		
		return new IEModule(slots, mods, setId);
	}
	
	/**
	 * Laedt ein Modul aus dem angegbenen Context.
	 * @param context Der Context
	 * @return Der Effect
	 */
	public static ItemEffect fromContext(Context context) {
		List<String> slots = new ArrayList<String>();
		String slotstring = context.getRequest().getParameterString("slots");
		String[] theslots = StringUtils.split(slotstring, ";");
		for( int i=0; i < theslots.length; i++) {
			
			ModuleSlots.get().slot(theslots[i]);
			
			slots.add(theslots[i]);
		}
		
		int setId = context.getRequest().getParameterInt("setid");
		
		ShipTypeChangeset mods = new ShipTypeChangeset(context, "");
		
		return new IEModule(slots, mods, setId);
	}
	
	/**
	 * Gibt das passende Fenster fuer das Adminmenue aus.
	 * @param echo Der Writer des Adminmenues
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	public void getAdminTool(Writer echo) throws IOException {
		
		echo.append("<input type=\"hidden\" name=\"type\" value=\"module\" >");
		echo.append("<tr><td class=\"noBorderS\">SetId (0=kein Set): </td><td><input type=\"text\" name=\"setid\" value=\"" + getSetID() + "\"></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Slots: </td><td><input type=\"text\" name=\"slots\" value=\"" + Common.implode(";", getSlots()) + "\"></td></tr>\n");
		getMods().getAdminTool(echo, "");
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	public String toString() {
		String itemstring = "module:" + Common.implode(";", getSlots()) + "&" + getSetID() + "&" + getMods().toString();
		return itemstring;
	}
}
