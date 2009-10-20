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
package net.driftingsouls.ds2.server.framework;

import java.util.Date;
import java.util.Locale;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Hauptklasse von Drifting Souls.
 * Dient im Moment primaer dazu das die Hauptklassen zu initalisieren und in einen
 * vernuempfigen Zustand zu ueberfuehren.
 * Als erster Schritt in jedem Programm sollte zuerst diese Klasse instanziiert werden.
 * 
 * @author Christopher Jung
 *
 */
public class DriftingSouls {
	private Log LOG = null; 
	/**
	 * Startet eine neue Instanz von Drifting Souls.
	 * Dabei werden Konfiguration und Datenbankverbindungen sowie
	 * weitere bootbare Klassen initalisiert.
	 * 
	 * @param log Logger, mit dem die Initalisierung protokolliert werden soll
	 * @param configdir Das DS-Konfigurationsverzeichnis
	 * @param boot Sollen die boot.xml abgearbeitet werden?
	 * @throws Exception
	 */
	public DriftingSouls(Log log, String configdir, boolean boot) throws Exception {
		LOG = log;
		LOG.info("----------- DS2 Startup "+new Date()+" -----------");
		LOG.info("Reading "+configdir+"config.xml");		
		Configuration.init(configdir);
		
		LOG.info("Setting up Boot Context...");
		BasicContext context = new BasicContext(new CmdLineRequest(new String[0]), new SimpleResponse());

		Common.setLocale(Locale.GERMAN); 
		
		if( boot ) {
			LOG.info("Booting Classes...");
	
			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"boot.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "/bootlist/boot");
			for( int i=0; i < nodes.getLength(); i++ ) {
				String className = XMLUtils.getStringByXPath(nodes.item(i), "@class");
				String type = XMLUtils.getStringByXPath(nodes.item(i), "@type");
				LOG.info("["+type+"] Booting "+className);
	
				if( type.equals("static") ) {
					Class.forName(className);
				}
				else if( type.equals("singleton") ) {
					Class<?> cls = Class.forName(className);
					cls.getMethod("getInstance").invoke(null);
				}
				else {
					throw new Exception("Kann Klasse '"+className+"' nicht booten: Unbekannter Boot-Typ '"+type+"'");
				}
			}
			org.hibernate.Session db = ContextMap.getContext().getDB();
			User nulluser = (User)db.get(User.class, 0);
		}

		context.free();
	}
}
