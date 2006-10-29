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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repraesentiert die Liste aller in DS bekannten Itemtypen.
 * Die Liste wird beim Start von DS aus der <code>items.xml</code>
 * geladen
 * 
 * @author Christopher Jung
 *
 */
public class Items implements Loggable,Iterable<Item> {
	private static Items itemList = new Items();
	private Map<Integer, Item> list = new HashMap<Integer, Item>();
	
	private Items() {
		// EMPTY
	}

	private void addItem( Item item ) {
		list.put(item.getID(), item);
	}
	
	/**
	 * Gibt die Instanz der Item-Liste zurueck
	 * @return Die Item-Listen-Instanz
	 */
	public static Items get() {
		return itemList;
	}
	
	public Iterator<Item> iterator() {
		return list.values().iterator();
	}
	
	/**
	 * Gibt das Item mit der angegebenen ID zurueck.
	 * Falls kein Item mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Items
	 * @return Das Item oder <code>null</code>
	 */
	public Item item( int id ) {
		return list.get(id);
	}
	
	static {
		/*
		 * items.xml parsen
		 */
		try {
			Database db = new Database();
			String imagepath = User.getDefaultImagePath(db)+"data/items/";
			db.close();
			
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"items.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "items/item");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = (int)XMLUtils.getLongAttribute(node, "id");
				
				String name = XMLUtils.getStringByXPath(node, "name/text()");
				String picture = XMLUtils.getStringByXPath(node, "picture/text()");
				if( picture == null || picture.equals("") ) {
					picture = "open.gif";
				}
				
				Item item = new Item(id, name, imagepath+picture);
				item.cargo = (int)XMLUtils.getLongAttribute(node, "cargo");
				
				String largePicture = XMLUtils.getStringByXPath(node, "large-picture/text()");
				if( largePicture != null && !"".equals(largePicture) ) {
					item.largepicture = imagepath+largePicture;
				}
				
				String description = XMLUtils.getStringByXPath(node, "description/text()");
				if( description != null && !"".equals(description) ) {
					item.description = Common.trimLines(description);
				}
				
				item.effect = ItemEffect.fromXML(XMLUtils.getNodeByXPath(node, "effect"));
				
				Boolean handel = XMLUtils.getBooleanByXPath(node, "@handel");
				if( handel != null ) {
					item.handel = handel;
				}
				
				item.accesslevel = (int)XMLUtils.getLongAttribute(node, "accesslevel");
				
				item.quality = Item.Quality.fromString(XMLUtils.getStringByXPath(node, "quality/text()"));
				
				Boolean unknownItem = XMLUtils.getBooleanByXPath(node, "@unknownItem");
				if( unknownItem != null ) {
					item.unknownItem = unknownItem;
				}
				
				itemList.addItem(item);
			}
		}
		catch( Exception e ) {
			LOG.fatal("FAILED: Kann Items nicht laden",e);
		}
	}
}
