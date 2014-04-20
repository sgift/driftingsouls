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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.ships.ShipTypeChangeset;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item-Effekt "Meta-Effekt fuer Item-Modul-Sets".
 * @author Christopher Jung
 *
 */
public class IEModuleSetMeta extends ItemEffect {
	private String name;
	private Map<Integer,ShipTypeChangeset> combos = new HashMap<>();
	
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
		List<ShipTypeChangeset> combos = new ArrayList<>();
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
	
	/**
	 * Laedt einen Effect aus einem String.
	 * @param itemeffectString Der Effect als String
	 * @return Der Effect
	 * @throws IllegalArgumentException falls der Effect nicht richtig geladen werden konnte
	 */
	public static ItemEffect fromString(String itemeffectString) throws IllegalArgumentException {
		String[] effects = StringUtils.split(itemeffectString, "&");
		
		IEModuleSetMeta effect = new IEModuleSetMeta(effects[0]);
		
		for( int i=1; i< effects.length; i++)
		{
			String[] combo = StringUtils.split(effects[i], "\\");
			effect.addCombo(Integer.parseInt(combo[0]), new ShipTypeChangeset(combo[1]));
		}
		
		return effect;
	}
	
	/**
	 * Laedt einen Effekt aus dem angegebenen Context.
	 * @param context Der Context
	 * @return Der Effekt
	 */
	public static ItemEffect fromContext(Context context) {
		
		IEModuleSetMeta effect = new IEModuleSetMeta(context.getRequest().getParameterString("setname"));
		effect.name = context.getRequest().getParameterString("setname");
		
		for( int i = 1; i <= 10; i++)
		{
			if(context.getRequest().getParameterString("used"+i).equals("true"))
			{
				effect.addCombo(i, new ShipTypeChangeset(context, ""+i));
			}
		}
		return effect;
	}
	
	/**
	 * Gibt das passende Fenster fuer das Adminmenue aus.
	 * @param echo Der Writer des Adminmenues
	 * @throws IOException Exception falls ein fehler auftritt
	 */
	@Override
	public void getAdminTool(StringBuilder echo) throws IOException {
		Map<Integer, ShipTypeChangeset> combos = getCombos();
		
		echo.append("<input type=\"hidden\" name=\"type\" value=\"module-set-meta\" >");
		echo.append("<tr><td class=\"noBorderS\">Set-Name: </td><td><input type=\"text\" name=\"setname\" value=\""+getName()+"\"></td></tr>\n");
		for(int i = 1; i <= 10; i++) {
			if( combos.containsKey(i)) {
				echo.append("<tr><td class=\"noBorderS\">Benutze Combo "+i+": </td><td><input type=\"text\" name=\"used"+i+"\" value=\"true\"></td></tr>\n");
				combos.get(i).getAdminTool(echo, ""+i);
			}
			else
			{
				echo.append("<tr><td class=\"noBorderS\">Benutze Combo "+i+": </td><td><input type=\"text\" name=\"used"+i+"\" value=\"false\"></td></tr>\n");
				new ShipTypeChangeset().getAdminTool(echo, ""+i);
			}
		}
	}
	
	/**
	 * Gibt den Itemeffect als String aus.
	 * @return der Effect als String
	 */
	@Override
	public String toString() {
		Map<Integer, ShipTypeChangeset> combos = getCombos();
		String itemstring = "module-set-meta:" + getName() + "&";
		for(int i = 1; i <= 10; i++) {
			if( combos.containsKey(i)) {
				itemstring = itemstring + i + "\\" + combos.get(i).toString() + "&";
			}
		}
		itemstring = itemstring.substring(0, itemstring.length()-1);
		return itemstring;
	}
}
