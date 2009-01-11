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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>Repraesentiert die Liste aller bekannten Resourcen in DS. Resourcen
 * sind einfache Waren ohne Eigenschaften. Es handelt sich dabei nicht um Items!</p>
 * <p>Die Liste wird beim Start von DS aus der Datei <code>resources.xml</code> geladen.</p>
 * 
 * @author Christopher Jung
 *
 */
public class ResourceConfig {
	private static final Log log = LogFactory.getLog(ResourceConfig.class);
	
	/**
	 * Die Konfigurationsdaten einer einzelnen Resource (Ware).
	 */
	public static class Entry {
		private int id;
		private String name;
		private String image;
		private boolean hidden;
		private String tag;
		
		/**
		 * Gibt zurueck, ob die Resource versteckt (d.h. fuer Spieler nicht sichtbar) ist.
		 * @return <code>true</code>, falls sie nicht sichtbar ist.
		 */
		public boolean isHidden() {
			return hidden;
		}

		/**
		 * Gibt die ID der Resource zurueck.
		 * @return Die ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * Gibt den Pfad zum Bild der Resource zurueck.
		 * @return Der Pfad
		 */
		public String getImage() {
			return image;
		}

		/**
		 * Gibt den Namen der Resource zurueck.
		 * @return Der Name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gibt den Tag zurueck, der der Resource in XML-Dokumenten zugeordnet ist.
		 * @return Der Tag
		 */
		public String getTag() {
			return tag;
		}

		Entry() {
			// EMPTY
		}
	}
	
	private static Map<String,Integer> tagMap = new HashMap<String,Integer>();
	
	private static List<Entry> resources = new ArrayList<Entry>();
	
	/**
	 * @return In DS bekannte Resourcen
	 */
	public static Collection<ResourceConfig.Entry> getResources() {
		return Collections.unmodifiableCollection(resources);
	}
	
	/**
	 * Liefert den Namen einer Resource zurueck.
	 * @param id Die ID der Resource
	 * @return Der Name
	 */
	public static String getResourceName(int id) {
		return resources.get(id).getName();
	}
	
	/**
	 * Liefert den Pfad zum Bild einer Resource zurueck.
	 * @param id Die ID der Resource
	 * @return Der Pfad zum Bild
	 */
	public static String getResourceImage(int id) {
		return resources.get(id).getImage();
	}
	
	/**
	 * Gibt zurueck, ob eine Resource sichtbar (<code>false</code>) oder versteckt (<code>true</code>) ist.
	 * 
	 * @param id Die ID der Resource
	 * @return true, falls sie versteckt ist
	 */
	public static boolean getResourceHidden(int id) {
		return resources.get(id).isHidden();
	}
	
	/**
	 * Gibt den XML-Tag einer Resource zurueck, sofern diese einen besitzt.
	 * @param id Die ID der Resource
	 * @return Der Name des XML-Tags oder <code>null</code>
	 */
	public static String getResourceTag(int id) {
		return resources.get(id).getTag();
	}
	
	/**
	 * Gibt fuer einen Resource-Tag die Resource-ID zurueck.
	 * @param tag der Tag
	 * @return Die Resourcen-ID oder <code>null</code>
	 */
	public static Integer getResourceIDByTag(String tag) {
		return tagMap.get(tag);
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
				tagMap.put(entry.tag, entry.id);
				
				resources.add(entry.id, entry);
			}
		}
		catch(Exception e) {
			log.fatal(e, e);
		}
	}
}
