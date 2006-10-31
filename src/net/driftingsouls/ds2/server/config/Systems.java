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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repraesentiert die Liste aller Sternensysteme in DS. Die Liste
 * wird beim Start von DS aus der Datei <code>systems.xml</code>
 * geladen.
 * 
 * @author Christopher Jung
 *
 */
public class Systems implements Iterable<StarSystem>,Loggable {
	private static Systems systemList = new Systems();
	private Map<Integer, StarSystem> list = new HashMap<Integer, StarSystem>();
	
	private Systems() {
		// EMPTY
	}

	private void addSystem( StarSystem system ) {
		list.put(system.getID(), system);
	}
	
	/**
	 * Liefert die Instanz der Sternensystem-Liste zurueck
	 * @return Die Sternensystem-Listen-Instanz
	 */
	public static Systems get() {
		return systemList;
	}
	
	public Iterator<StarSystem> iterator() {
		return list.values().iterator();
	}
	
	/**
	 * Gibt das Sternensystem mit der angegebenen ID zurueck.
	 * Sollte kein Sternensystem mit der ID bekannt sein, wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Sternensystems
	 * @return Das Sternensystem oder <code>null</code>
	 */
	public StarSystem system( int id ) {
		return list.get(id);
	}

	static {
		/*
		 * systems.xml parsen
		 */
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"systems.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "systems/system");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = XMLUtils.getNumberByXPath(node, "@id").intValue();
				int width = XMLUtils.getNumberByXPath(node, "@width").intValue();
				int height = XMLUtils.getNumberByXPath(node, "@height").intValue();
				
				boolean allowMilitary = "true".equals(XMLUtils.getStringByXPath(node, "@allow-military"));
				
				String name = XMLUtils.getStringByXPath(node, "name/text()");
				String accessStr = XMLUtils.getStringByXPath(node, "access/text()");
				int access = StarSystem.AC_NORMAL;
				if( "admin".equals(accessStr) ) {
					access = StarSystem.AC_ADMIN;
				}
				else if( "npc".equals(accessStr) ) {
					access = StarSystem.AC_NPC;
				}
				
				StarSystem system = new StarSystem(id, name, width, height, allowMilitary, access );
				
				// @max-colonies verarbeiten
				if( XMLUtils.getNumberByXPath(node, "max-colonies") != null ) {
					system.setMaxColonys(XMLUtils.getNumberByXPath(node, "max-colonies").intValue());
				}
				
				// <description> verarbeiten
				if( XMLUtils.getStringByXPath(node, "description/text()") != null ) {
					system.setDescription(Common.trimLines(XMLUtils.getStringByXPath(node, "description/text()")));
				}
				
				// <drop-zone> verarbeiten
				if( XMLUtils.getNodeByXPath(node, "drop-zone") != null ) {
					int x = XMLUtils.getNumberByXPath(node, "drop-zone/@x").intValue();
					int y = XMLUtils.getNumberByXPath(node, "drop-zone/@y").intValue();
					system.setDropZone(new Location(id, x, y));
				}
				
				// <order-location> verarbeiten
				NodeList orderlocs = XMLUtils.getNodesByXPath(node, "order-location");
				for( int j=0; j < orderlocs.getLength(); j++ ) {
					int x = XMLUtils.getNumberByXPath(orderlocs.item(j), "@x").intValue();
					int y = XMLUtils.getNumberByXPath(orderlocs.item(j), "@y").intValue();
					system.addOrderLocation(new Location(id, x, y));
				}
				
				systemList.addSystem(system);
			}
		}
		catch( Exception e ) {
			LOG.fatal("FAILED: Kann Rassen nicht laden",e);
		}
	}
}
