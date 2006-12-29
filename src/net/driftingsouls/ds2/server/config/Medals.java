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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

/**
 * Repraesentiert die Liste aller in DS bekannten Orden und Raenge.
 * Die Liste wird beim Start von DS aus der <code>medals.xml</code>
 * geladen
 * 
 * @author Christopher Jung
 *
 */
public class Medals implements Loggable {
	private static Medals medalList = new Medals();
	private Map<Integer, Medal> list = new LinkedHashMap<Integer, Medal>();
	private Map<Integer, Rang> raenge = new LinkedHashMap<Integer, Rang>();
	
	private Medals() {
		// EMPTY
	}

	private void addMedal( Medal mmedal ) {
		list.put(mmedal.getID(), mmedal);
	}
	
	private void addRang( Rang mrang ) {
		raenge.put(mrang.getID(), mrang );
	}
	
	/**
	 * Gibt die Instanz der Orden/Raenge-Liste zurueck
	 * @return Die Medals-Listen-Instanz
	 */
	public static Medals get() {
		return medalList;
	}
	
	/**
	 * Gibt den Rang mit der angegebenen ID zurueck.
	 * Falls kein Rang mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Ranges
	 * @return Der Rang oder <code>null</code>
	 */
	public Rang rang( int id ) {
		return raenge.get(id);
	}
	
	/**
	 * Gibt die Liste der Raenge zurueck
	 * @return die Liste der Raenge
	 */
	public Map<Integer,Rang> raenge() {
		return Collections.unmodifiableMap(raenge);
	}
	
	/**
	 * Gibt den Orden mit der angegebenen ID zurueck.
	 * Falls kein Orden mit der ID existiert wird <code>null</code>
	 * zurueckgegeben.
	 * 
	 * @param id Die ID des gewuenschten Ordens
	 * @return Der Orden oder <code>null</code>
	 */
	public Medal medal( int id ) {
		return list.get(id);
	}
	
	/**
	 * Gibt die Liste der Orden zurueck
	 * @return die Liste der Orden
	 */
	public Map<Integer,Medal> medals() {
		return Collections.unmodifiableMap(list);
	}
	
	static {
		/*
		 * medals.xml parsen
		 */
		try {
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"medals.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "medals/medal");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = (int)XMLUtils.getLongAttribute(node, "id");
				String name = XMLUtils.getStringAttribute(node, "name");
				
				Medal medal = new Medal(id, name);
				
				medalList.addMedal(medal);
				
				String adminonly = XMLUtils.getStringAttribute(node, "admin-only");
				if( adminonly != null ) {
					medal.setAdminOnly(Boolean.parseBoolean(adminonly));
				}

				String image = XMLUtils.getStringByXPath(node, "image/text()");
				String highlight = XMLUtils.getStringByXPath(node, "highlight/text()");
				String small = XMLUtils.getStringByXPath(node, "small/text()");
				medal.setImages(image, highlight, small);
			}
			nodes = XMLUtils.getNodesByXPath(doc, "medals/raenge/rang");
			for( int i=0; i < nodes.getLength(); i++ ) {
				Node node = nodes.item(i);
				int id = (int)XMLUtils.getLongAttribute(node, "id");
				String name = XMLUtils.getStringAttribute(node, "name");
				
				Rang rang = new Rang(id, name);
				
				medalList.addRang(rang);
			}
		}
		catch( Exception e ) {
			LOG.fatal("FAILED: Kann Orden/Raenge nicht laden",e);
		}
	}
}
