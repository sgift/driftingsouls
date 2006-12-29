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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Common;

import org.apache.commons.lang.StringUtils;

/**
 * Repraesentiert die Liste aller bekannten Waffen in DS sowie einige
 * Hilfsfunktionen
 * @author Christopher Jung
 *
 */
public class Weapons implements Iterable<Weapon> {
	private Map<String, Weapon> list = new LinkedHashMap<String, Weapon>();
	private static Weapons instance = new Weapons();
	
	private Weapons() {
		// EMPTY
	}
	
	/**
	 * Gibt eine Instanz der Waffenliste zurueck
	 * @return die Instanz der Waffenliste
	 */
	public static Weapons get() {
		return instance;
	}
	
	public Iterator<Weapon> iterator() {
		return list.values().iterator();
	}
	
	/**
	 * Gibt die Instanz einer Waffe mit der angegebenen Waffen-ID zurueck.
	 * Sollte keine passende Waffe bekannt sein, so wird <code>null</code> zurueckgegeben.
	 * @param wpn Die Waffen-ID
	 * @return Die Instanz der Waffe oder <code>null</code>
	 */
	public Weapon weapon(String wpn) {
		return list.get(wpn);
	}
	
	/**
	 * Splittet einen Waffen-String in eine Map auf. Schluessel ist der Waffenname.
	 * @param weaponlist der Waffen-String
	 * @return die Waffen-Map
	 */
	public static Map<String,String> parseWeaponList(String weaponlist) {
		Map<String,String> result = new LinkedHashMap<String,String>();
		String[] weaponArray = StringUtils.split(weaponlist, '|');

		for( int i=0; i < weaponArray.length; i++ ) {
			String[] weapon = StringUtils.split(weaponArray[i], '=');
			if( !weapon[0].equals("") ) {
				result.put(weapon[0], weapon[1]);
			}
		}

		return result;
	}
	
	/**
	 * Packt eine Waffen-Map wieder in einen String zusammen
	 * @param weapons die Waffen-Map
	 * @return der Waffen-String
	 */
	public static String packWeaponList(Map<String,String> weapons) {
		List<String> weaponlist = new ArrayList<String>();

		for( String wpnKey : weapons.keySet() ) {
			weaponlist.add(wpnKey + '=' + weapons.get(wpnKey));
		}
		String weaponstring = Common.implode("|",weaponlist);

		return weaponstring;
	}
	
	static {
		// TODO
		Common.stub();
	}
}
