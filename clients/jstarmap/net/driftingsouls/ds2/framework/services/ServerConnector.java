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

package net.driftingsouls.ds2.framework.services;

import java.util.HashMap;


/**
 * Gruppiert saemtliche Klassen, welche Zugriff auf bestimmte Dienste auf dem Server bereitstellen,
 * wie z.B. soap und Kartenzugriff. Jeder Dienst muss dafuer das ServerConnectable-Interface
 * implementieren und ueber einen eindeutigen Namen identifizierbar sein (Name des Service).
 * Anbieten tut sich hierfuer eine Konstante SERVICE, welche den Klassennamen (inkl. Packages) enthaelt, in
 * der jeweiligen Service-Klasse
 * 
 * @author Christopher Jung
 * 
 */
public class ServerConnector {
	private HashMap<String,ServerConnectable> connectors;
	private String url;
	private String session;
	
	private static ServerConnector instance = null;
	
	/**
	 * Erstellt eine neue Instanz des ServerConnectors unter Verwendung
	 * des DS-Server-Pfads und einer Session-ID
	 * 
	 * @param url Pfad zum DS-Server (z.B. "http://www.server.com/ds/")
	 * @param session Session-ID
	 */
	private ServerConnector(String url, String session) {		
		connectors = new HashMap<String,ServerConnectable>();
		this.url = url;
		this.session = session;
	}
	
	/**
	 * Erzeugt eine Instanz des ServerConnectors und registriert einen Soap-Service {@link SoapConnector},
	 * einen XML-Service {@link XMLConnector} und einen Karten-Service {@link MapConnector}
	 * 
	 * @param url Pfad zum DS-Server (z.B. "http://www.server.com/ds/")
	 * @param session Die zu verwendende Session-ID
	 * 
	 * @throws Exception falls bereits eine Instanz erzeugt wurde
	 */
	public static void createInstance(String url, String session) throws Exception {
		if( instance != null ) {
			throw new Exception("Es ist nur eine Instanz des ServerConnectors erlaubt");
		}
		
		instance = new ServerConnector(url, session);
		instance.registerService(SoapConnector.SERVICE, new SoapConnector());
		instance.registerService(XMLConnector.SERVICE, new XMLConnector());
		instance.registerService(MapConnector.SERVICE, new MapConnector());
	}
	
	/**
	 * Liefert eine Instanz des ServerConnectors oder null, wenn noch keine Instanz
	 * erzeugt wurde 
	 * @return Instanz des ServerConnectors
	 */
	public static ServerConnector getInstance() {
		return instance;
	}
	
	/**
	 * Registriert einen neuen Service
	 * 
	 * @param service Name des Service
	 * @param con ServerConnectable, welches den Service repräsentiert
	 */
	public void registerService( String service, ServerConnectable con ) {
		connectors.put(service, con);
	}
	
	/**
	 * Liefert ein zu einem Service gehoerendes ServerConnectable-Objekt zurueck
	 * oder null, sollte kein solcher Service existieren
	 * 
	 * @param service Name des Service
	 * @return Objekt bzw null
	 */
	public ServerConnectable getService( String service ) {
		return connectors.get(service);
	}
	
	/**
	 * Liefert den Pfad zum DS-Server zurueck
	 * 
	 * @return Pfad
	 */
	public String getServerURL() {
		return url;
	}
	
	/**
	 * Liefert die aktuelle Session-ID
	 * @return Session-ID
	 */
	public String getSession() {
		return session;
	}
	
	/**
	 * Setzt die zu verwendende Session-ID
	 * @param session Die Session-ID
	 */
	public void setSession(String session) {
		this.session = session;
	}
}
