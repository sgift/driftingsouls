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

import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repraesentiert die Liste aller bekannten Waffen in DS sowie einige
 * Hilfsfunktionen.
 * @author Christopher Jung
 *
 */
public class Weapons implements Iterable<Weapon> {
	private static Weapons instance = new Weapons();
	
	private Weapons() {
		// EMPTY
	}
	
	/**
	 * Gibt eine Instanz der Waffenliste zurueck.
	 * @return die Instanz der Waffenliste
	 */
	public static Weapons get() {
		return instance;
	}

	@Override
	public Iterator<Weapon> iterator() {
		Session db = ContextMap.getContext().getDB();
		List<Weapon> wpn = Common.cast(db.createCriteria(Weapon.class).list());
		return wpn.iterator();
	}

	/**
	 * Gibt die Instanz einer Waffe mit der angegebenen Waffen-ID zurueck.
	 * Sollte keine passende Waffe bekannt sein, so wird eine {@link NoSuchWeaponException} geworfen.
	 * @param wpn Die Waffen-ID
	 * @return Die Instanz der Waffe
	 * @throws NoSuchWeaponException Falls die angeforderte Waffe nicht existiert
	 */
	public Weapon weapon(String wpn) throws NoSuchWeaponException {
		Weapon weapon = (Weapon)ContextMap.getContext().getDB().get(Weapon.class, wpn);
		if( weapon == null )
		{
			throw new NoSuchWeaponException(wpn);
		}
		return weapon;
	}
	
	/**
	 * Splittet einen Waffen-String in eine Map auf. Schluessel ist der Waffenname.
	 * @param weaponlist der Waffen-String
	 * @return die Waffen-Map
	 */
	public static Map<String,Integer> parseWeaponList(String weaponlist) {
		Map<String,Integer> result = new LinkedHashMap<>();
		String[] weaponArray = StringUtils.split(weaponlist, '|');

		for (String aWeaponArray : weaponArray)
		{
			String[] weapon = StringUtils.split(aWeaponArray, '=');
			if (!weapon[0].equals(""))
			{
				result.put(weapon[0], Integer.valueOf(weapon[1]));
			}
		}

		return result;
	}
	
	/**
	 * Packt eine Waffen-Map wieder in einen String zusammen.
	 * @param weapons die Waffen-Map
	 * @return der Waffen-String
	 */
	public static String packWeaponList(Map<String,Integer> weapons) {
		List<String> weaponlist = weapons.entrySet().stream().map(wpnEntry -> wpnEntry.getKey() + '=' + wpnEntry.getValue()).collect(Collectors.toList());

		return Common.implode("|",weaponlist);
	}
}
