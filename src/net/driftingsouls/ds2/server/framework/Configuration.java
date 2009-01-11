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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.xml.XMLUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Configuration kann Konfigurationsdateien parsen und die darin enthaltenen Einstellungen in die
 * Einstellungsliste laden. Bereits vorhandene Einstellungen gehen dabei jedoch verloren. Es koennen
 * aber auch nachtraeglich Konfigurationseinstellungen gesetzt werden. Diese werden jedoch nicht in
 * der Konfigurationsdatei, aus der die Einstellungen geladen wurden, geschrieben.
 * 
 * @author Christopher Jung
 * 
 */
public class Configuration
{
	private static final Log log = LogFactory.getLog(Configuration.class);

	private static Map<String, String> config = new HashMap<String, String>();
	private static Map<String, Integer> configInt = new HashMap<String, Integer>();

	/**
	 * Laedt alle Konfigurationseinstellungen aus der config.xml im angegebenen Verzeichnis. Alle
	 * bereits geladenen Konfigurationseinstellungen werden vorher geloescht. Das
	 * Konfigurationsverzeichnis wird unter "configdir" in der Konfiguration abgelegt.
	 * 
	 * @param configdir Hauptkonfigurationsverzeichnis von DS
	 * @throws Exception
	 */
	public static synchronized void init(String configdir) throws Exception
	{
		if( !new File(configdir + "config.xml").isFile() )
		{
			throw new FileNotFoundException("Configuraton konnte config.xml nicht im Verzeichnis "
					+ configdir + " finden");
		}
		config.clear();
		configInt.clear();

		putSetting("configdir", configdir);

		Document doc = XMLUtils.readFile(configdir + "config.xml");
		NodeList nodes = XMLUtils.getNodesByXPath(doc, "/config/setting");
		for( int i = 0; i < nodes.getLength(); i++ )
		{
			String type = nodes.item(i).getAttributes().getNamedItem("type").getNodeValue();
			String name;
			String value;

			if( nodes.item(i).getAttributes().getNamedItem("name") != null )
			{
				name = nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
				value = nodes.item(i).getAttributes().getNamedItem("value").getNodeValue();
			}
			else
			{
				name = XMLUtils.getStringByXPath(nodes.item(i), "name/text()");
				value = XMLUtils.getStringByXPath(nodes.item(i), "value/text()");
			}

			if( "string".equalsIgnoreCase(type) )
			{
				Configuration.putSetting(name, value);
			}
			else if( "number".equalsIgnoreCase(type) )
			{
				Configuration.putIntSetting(name, Integer.parseInt(value));
			}
			else
			{
				throw new Exception("Illegal configuration setting type '" + type + "'");
			}
		}
	}

	/**
	 * Liefert eine Konfigurationseinstellung als String zurueck.
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @return Wert der Konfigurationseinstellung
	 * @deprecated Bitte die Konfiguration injizieren lassen
	 */
	@Deprecated
	public static String getSetting(String setting)
	{
		if( config.containsKey(setting) )
		{
			return config.get(setting);
		}
		Integer value = configInt.get(setting);
		if( value != null )
		{
			return value.toString();
		}
		log.error("couldn't read " + setting + " from Configuration");
		return null;
	}

	/**
	 * Liefert eine Konfigurationseinstellung als Integer zurueck, sofern diese bereits als Integer
	 * vorliegt. Es wird keine Umwandlung String nach Integer durchgefuehrt!
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @return Wert der Konfigurationseinstellung
	 * @deprecated Bitte die Konfiguration injizieren lassen
	 */
	@Deprecated
	public static int getIntSetting(String setting)
	{
		Integer val = configInt.get(setting);
		if( val == null )
		{
			log.error("couldn't read integer " + setting + " from Configuration");
			throw new RuntimeException("couldn't read integer " + setting + " from Configuration");
		}

		return val;
	}

	/**
	 * Setzt eine Konfigurationseinstellung auf einen String-Wert.
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @param value Wert der Konfigurationseinstellung
	 */
	private static synchronized void putSetting(String setting, String value)
	{
		config.put(setting, value);
		configInt.remove(setting);
	}

	/**
	 * Setzt eine Konfigurationseinstellung auf einen Integer-Wert.
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @param value Wert der Konfigurationseinstellung
	 */
	private static synchronized void putIntSetting(String setting, int value)
	{
		configInt.put(setting, value);
		config.remove(setting);
	}

	/**
	 * Liefert eine Konfigurationseinstellung als String zurueck.
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @return Wert der Konfigurationseinstellung
	 */
	public String get(String setting)
	{
		return getSetting(setting);
	}

	/**
	 * Liefert eine Konfigurationseinstellung als Integer zurueck, sofern diese bereits als Integer
	 * vorliegt. Es wird keine Umwandlung String nach Integer durchgefuehrt!
	 * 
	 * @param setting Name der Konfigurationseinstellung
	 * @return Wert der Konfigurationseinstellung
	 */
	public int getInt(String setting)
	{
		return getIntSetting(setting);
	}
}
