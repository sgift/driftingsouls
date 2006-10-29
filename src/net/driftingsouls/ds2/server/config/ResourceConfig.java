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
import java.util.List;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>Repraesentiert die Liste aller bekannten Resourcen in DS. Resourcen
 * sind einfache Waren ohne Eigenschaften. Es handelt sich dabei nicht um Items!</p>
 * <p>Die Liste wird beim Start von DS aus der Datei <code>resources.xml</code> geladen</p>
 * 
 * @author Christopher Jung
 *
 */
public class ResourceConfig implements Loggable {
	private static class Entry {
		int id;
		String name;
		String image;
		boolean hidden;
		String tag;
		
		Entry() {
			// EMPTY
		}
	}
	
	private static List<Entry> resources = new ArrayList<Entry>();
	
	/**
	 * Liefert den Namen einer Resource zurueck
	 * @param id Die ID der Resource
	 * @return Der Name
	 */
	public static String getResourceName(int id) {
		return resources.get(id).name;
	}
	
	/**
	 * Liefert den Pfad zum Bild einer Resource zurueck
	 * @param id Die ID der Resource
	 * @return Der Pfad zum Bild
	 */
	public static String getResourceImage(int id) {
		return resources.get(id).image;
	}
	
	/**
	 * Gibt zurueck, ob eine Resource sichtbar (<code>false</code>) oder versteckt (<code>true</code>) ist.
	 * 
	 * @param id Die ID der Resource
	 * @return true, falls sie versteckt ist
	 */
	public static boolean getResourceHidden(int id) {
		return resources.get(id).hidden;
	}
	
	/**
	 * Gibt den XML-Tag einer Resource zurueck, sofern diese einen besitzt
	 * @param id Die ID der Resource
	 * @return Der Name des XML-Tags oder <code>null</code>
	 */
	public static String getResourceTag(int id) {
		return resources.get(id).tag;
	}
	
	static {
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"resources.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "resources/resource");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				Entry entry = new Entry();
				entry.id = XMLUtils.getNumberByXPath(node, "@id").intValue();
				entry.name = XMLUtils.getStringByXPath(node, "@name");
				entry.image = XMLUtils.getStringByXPath(node, "@image");
				entry.tag = XMLUtils.getStringByXPath(node, "@tag");
				entry.hidden = "true".equals(XMLUtils.getStringByXPath(node, "@hidden"));
				
				resources.add(entry.id, entry);
			}
		}
		catch(Exception e) {
			LOG.fatal(e, e);
		}
	}
}
