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

import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Klasse mit Konfigurationsdaten zu den einzelnen Fraktionen.
 * @author Christopher Jung
 *
 */
public class Faction {
	private static final Log log = LogFactory.getLog(Faction.class);
	
	private static Map<Integer,Faction> factionList = new LinkedHashMap<Integer,Faction>();
	
	/**
	 * Die Spieler-ID der GTU.
	 */
	public static final int GTU = -2;
	
	/**
	 * Die Spieler-ID von Demolition Inc.
	 */
	public static final int DI = -19;
	
	/**
	 * Die Spieler-ID von Ito.
	 */
	public static final int ITO = -26;
	
	/**
	 * Die Spieler-ID des Piraten-Accounts.
	 */
	public static final int PIRATE = -15;
	
	/**
	 * Die Spieler-ID des Regulus Syndikats.
	 */
	public static final int XR = -32;
		
	/**
	 * Gibt die angegebene Fraktion zurueck. Sollte keine passende Fraktion existieren, so wird <code>null</code> zurueckgegeben.
	 * @param id Die ID der Fraktion 
	 * @return Die angegebene Fraktion oder <code>null</code>
	 */
	public static Faction get(int id) {
		return factionList.get(id);
	}
	
	/**
	 * Gibt die Liste aller bekannten Fraktionen zurueck.
	 * @return Die Liste der bekannten Fraktionen
	 */
	public static Map<Integer,Faction> getFactions() {
		return Collections.unmodifiableMap(factionList);
	}
	
	private FactionPages pages = null;
	private int id = 0;
	
	private Faction(int id) {
		this.id = id;
	}
	
	/**
	 * Gibt die Beschreibung der Fraktionsseite zurueck.
	 * @return Die Fraktionsseite
	 */
	public FactionPages getPages() {
		return pages;
	}
	
	/**
	 * Gibt die ID der Fraktion zurueck.
	 * @return Die ID
	 */
	public int getID() {
		return this.id;
	}
	
	static {
		/*
		 * factions.xml parsen
		 */
		try {	
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"factions.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "factions/faction");
			for( int i=0; i < nodes.getLength(); i++ ) {
				FactionPages fp = FactionPages.fromXML(nodes.item(i));
				Faction fac = new Faction(fp.getID());
				fac.pages = fp;
				
				factionList.put(fac.getID(), fac);
			}
		}
		catch( Exception e ) {
			log.fatal("FAILED: Kann Items nicht laden",e);
		}
	}
}
