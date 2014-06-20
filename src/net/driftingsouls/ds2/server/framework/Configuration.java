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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration kann Konfigurationsdateien parsen und die darin enthaltenen Einstellungen in die
 * Einstellungsliste laden. Bereits vorhandene Einstellungen gehen dabei jedoch verloren. Es koennen
 * aber auch nachtraeglich Konfigurationseinstellungen gesetzt werden. Diese werden jedoch nicht in
 * der Konfigurationsdatei, aus der die Einstellungen geladen wurden, geschrieben.
 * 
 * @author Christopher Jung
 * 
 */
@Component
@Lazy
public class Configuration
{
	private static Map<String, String> config = new HashMap<>();

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

		config.put("configdir", configdir);

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
				config.put(name, value);
			}
			else
			{
				throw new Exception("Illegal configuration setting type '" + type + "'");
			}
		}
	}

	/**
	 * Gibt zurueck, ob es sich um ein Produktivsetup handelt
	 * @return <code>true</code>, falls dem so ist
	 */
	public static boolean isProduction()
	{
		return config.containsKey("PRODUCTION") && "true".equals(config.get("PRODUCTION"));
	}

	/**
	 * Gibt die JDBC-Url fuer DS zurueck.
	 * @return Die Url
	 */
	public static String getDbUrl()
	{
		return config.get("db_url");
	}

	/**
	 * Gibt den Datenbank-User fuer DS zurueck.
	 * @return Der User
	 */
	public static String getDbUser()
	{
		return config.get("db_user");
	}

	/**
	 * Gibt das Passwort fuer den Datenbank-User fuer DS zurueck.
	 * @return Das Passwort
	 */
	public static String getDbPassword()
	{
		return config.get("db_password");
	}

	/**
	 * Gibt den Dateisystempfad zu DS zurueck. Der Pfad endet mit einem Pfadseparator.
	 * @return Der Dateisystempfad
	 */
	public static String getAbsolutePath()
	{
		return config.get("ABSOLUTE_PATH");
	}

	/**
	 * Gibt den Dateisystempfad zum Logverzeichnis zurueck. Der Pfad endet mit einem Pfadseparator.
	 * @return Der Dateisystempfad
	 */
	public static String getLogPath()
	{
		return config.get("LOXPATH");
	}

	/**
	 * Gibt den Dateisystempfad zum Konfigurationsverzeichnis zurueck. Der Pfad endet mit einem Pfadseparator.
	 * @return Der Dateisystempfad
	 */
	public static String getConfigPath()
	{
		return config.get("configdir");
	}

	/**
	 * Gibt den Namen des SMTP-Servers fuer den Mailversandt zurueck.
	 * @return Der Servername
	 */
	public static String getSmtpServer()
	{
		return config.get("SMTP-SERVER");
	}

	/**
	 * Gibt die Email-Adresse zurueck, an die Fehler- oder Statusmeldungen
	 * gesendet werden sollen. Mehrere Adressen koennen ueber ein <code>;</code>
	 * separiert werden.
	 * @return Die Email-Adresse
	 */
	public static String getExceptionMailAddress()
	{
		return config.get("EXCEPTION_MAIL");
	}

	/**
	 * Gibt den Prefix zurueck, der vor den Titel von Fehler- oder Statusemails
	 * gesetzt werden soll.
	 * @return Der Prefix
	 */
	public static String getExceptionMailPrefix()
	{
		return config.get("EXCEPTION_MAIL_PREFIX");
	}
}
