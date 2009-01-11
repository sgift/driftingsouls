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
package net.driftingsouls.ds2.server.config.items;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.config.items.effects.ItemEffectFactory;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repraesentiert die Liste aller in DS bekannten Itemtypen.
 * Die Liste wird beim Start von DS aus der <code>items.xml</code>
 * geladen.
 * 
 * @author Christopher Jung
 *
 */
public class Items implements Iterable<Item> {
	private static final Log log = LogFactory.getLog(Items.class);
	private static Items itemList = new Items();
	private Map<Integer, Item> list = new LinkedHashMap<Integer, Item>();
	
	private Items() {
		// EMPTY
	}

	private void addItem( Item item ) {
		list.put(item.getID(), item);
	}
	
	/**
	 * Gibt die Instanz der Item-Liste zurueck.
	 * @return Die Item-Listen-Instanz
	 */
	public static Items get() {
		return itemList;
	}
	
	@Override
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
			String imagepath = BasicUser.getDefaultImagePath()+"data/items/";
			
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"items.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "items/item");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = (int)XMLUtils.getLongAttribute(node, "id");
				
				if( itemList.item(id) != null ) {
					throw new Exception("Item-ID "+id+" mehrfach vergeben");
				}
				
				String version = XMLUtils.getStringAttribute(node, "version");
				if( (version != null) && !version.equalsIgnoreCase(Configuration.getSetting("VERSION_TYPE")) ) {
					continue;
				}
				
				String name = XMLUtils.firstNodeByTagName(node, "name").getTextContent();
				
				String picture = "open.gif";
				Node pictureNode = XMLUtils.firstNodeByTagName(node, "picture");
				if( pictureNode != null ) {
					picture = pictureNode.getTextContent();
				}
				
				Item item = new Item(id, name, imagepath+picture);
				item.cargo = (int)XMLUtils.getLongAttribute(node, "cargo");
				
				Node largePicture = XMLUtils.firstNodeByTagName(node, "large-picture");
				if( largePicture != null ) {
					item.largepicture = imagepath+largePicture.getTextContent();
				}
				
				Node description = XMLUtils.firstNodeByTagName(node, "description");
				if( description != null ) {
					item.description = Common.trimLines(description.getTextContent());
				}
				
				item.effect = ItemEffectFactory.fromXML(XMLUtils.firstNodeByTagName(node, "effect"));
				
				Boolean handel = XMLUtils.getBooleanAttribute(node, "handel");
				if( handel != null ) {
					item.handel = handel;
				}
				
				item.accesslevel = (int)XMLUtils.getLongAttribute(node, "accesslevel");
				
				if( XMLUtils.firstNodeByTagName(node, "quality") != null ) {
					item.quality = Item.Quality.fromString(XMLUtils.firstNodeByTagName(node, "quality").getTextContent());
				}
				
				Boolean unknownItem = XMLUtils.getBooleanAttribute(node, "unknownItem");
				if( unknownItem != null ) {
					item.unknownItem = unknownItem;
				}
				
				itemList.addItem(item);
			}
		}
		catch( Exception e ) {
			log.fatal("FAILED: Kann Items nicht laden",e);
			throw new ExceptionInInitializerError(e);
		}
	}
}
