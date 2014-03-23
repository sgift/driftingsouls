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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repraesentiert die Liste aller bekannten Waffen in DS sowie einige
 * Hilfsfunktionen.
 * @author Christopher Jung
 *
 */
public class Weapons implements Iterable<Weapon> {
	private static final Log log = LogFactory.getLog(Weapons.class);
	private Map<String, Weapon> list = new LinkedHashMap<>();
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
		return list.values().iterator();
	}
	
	/**
	 * Gibt die Instanz einer Waffe mit der angegebenen Waffen-ID zurueck.
	 * Sollte keine passende Waffe bekannt sein, so wird eine {@link NoSuchWeaponException} geworfen.
	 * @param wpn Die Waffen-ID
	 * @return Die Instanz der Waffe
	 * @throws NoSuchWeaponException Falls die angeforderte Waffe nicht existiert
	 */
	public Weapon weapon(String wpn) throws NoSuchWeaponException {
		if( !list.containsKey(wpn) ) {
			throw new NoSuchWeaponException(wpn);
		}
		return list.get(wpn);
	}
	
	/**
	 * Splittet einen Waffen-String in eine Map auf. Schluessel ist der Waffenname.
	 * @param weaponlist der Waffen-String
	 * @return die Waffen-Map
	 */
	public static Map<String,String> parseWeaponList(String weaponlist) {
		Map<String,String> result = new LinkedHashMap<>();
		String[] weaponArray = StringUtils.split(weaponlist, '|');

		for (String aWeaponArray : weaponArray)
		{
			String[] weapon = StringUtils.split(aWeaponArray, '=');
			if (!weapon[0].equals(""))
			{
				result.put(weapon[0], weapon[1]);
			}
		}

		return result;
	}
	
	/**
	 * Packt eine Waffen-Map wieder in einen String zusammen.
	 * @param weapons die Waffen-Map
	 * @return der Waffen-String
	 */
	public static String packWeaponList(Map<String,String> weapons) {
		List<String> weaponlist = new ArrayList<>();

		for( Map.Entry<String, String> wpnEntry : weapons.entrySet() ) {
			weaponlist.add(wpnEntry.getKey() + '=' + wpnEntry.getValue());
		}

		return Common.implode("|",weaponlist);
	}
	
	static {
		/*
		 * items.xml parsen
		 */
		try {
			final Class<Weapon> wpnClass = Weapon.class;
			
			Document doc = XMLUtils.readFile(Configuration.getConfigPath()+"weapons.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "weapons/weapon");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				
				String id = XMLUtils.getStringAttribute(node, "id");
				
				String version = XMLUtils.getStringAttribute(node, "version");
				if( (version != null) && !version.equalsIgnoreCase(Configuration.getSetting("VERSION_TYPE")) ) {
					continue;
				}
				
				if( instance.list.containsKey(id) ) {
					throw new RuntimeException("Waffen-ID '"+id+"' bereits vergeben");
				}
				
				String cls = XMLUtils.getStringAttribute(node, "handler");
				
				Class<? extends Weapon> concreteClass = wpnClass;
				
				if( cls != null ) {
					concreteClass = Class.forName(cls).asSubclass(Weapon.class);
				}
				
				Constructor<? extends Weapon> constr = concreteClass.getConstructor(Node.class);
				Weapon wpn = constr.newInstance(node);
				
				instance.list.put(id, wpn);
			}
		}
		catch( Exception e ) {
			log.fatal("FAILED: Kann Waffen nicht laden",e);
		}
	}
}
