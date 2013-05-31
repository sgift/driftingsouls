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

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.Date;
import java.util.Locale;

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
	private static final Log LOG = LogFactory.getLog(DriftingSouls.class);

	/**
	 * Startet eine neue Instanz von Drifting Souls.
	 * Dabei werden Konfiguration und Datenbankverbindungen sowie
	 * weitere bootbare Klassen initalisiert.
	 *
	 * @param configdir Das DS-Konfigurationsverzeichnis
	 * @param boot Sollen die boot.xml abgearbeitet werden?
	 * @throws Exception
	 */
	public DriftingSouls(String configdir, boolean boot) throws Exception {
		LOG.info("----------- DS2 Startup "+new Date()+" -----------");
		LOG.info("Reading "+configdir+"config.xml");
		Configuration.init(configdir);

		Common.setLocale(Locale.GERMAN);

		if( boot ) {
			LOG.info("Booting Classes...");

			Document doc = XMLUtils.readFile(Configuration.getSetting("configdir")+"boot.xml");
			NodeList nodes = XMLUtils.getNodesByXPath(doc, "/bootlist/boot");
			for( int i=0; i < nodes.getLength(); i++ ) {
				String className = XMLUtils.getStringByXPath(nodes.item(i), "@class");
				String type = XMLUtils.getStringByXPath(nodes.item(i), "@type");
				LOG.info("["+type+"] Booting "+className);

				switch (type)
				{
					case "static":
						Class.forName(className);
						break;
					case "singleton":
						Class<?> cls = Class.forName(className);
						cls.getMethod("getInstance").invoke(null);
						break;
					default:
						throw new Exception("Kann Klasse '" + className + "' nicht booten: Unbekannter Boot-Typ '" + type + "'");
				}
			}
		}
	}
}
