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
package net.driftingsouls.ds2.server.framework.pipeline;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Standardinterface fuer die Antwort auf die Request.
 * @author Christopher Jung
 *
 */
public interface Response {
	/**
	 * Aktiviert das Zwischenspeichern der via Writer geschriebenen Ausgabe.
	 * Diese Methode kann nur so lange aufgerufen werden, wie noch keine Ausgabe
	 * erfolgt ist.
	 * @throws IllegalStateException Falls bereits die Ausgabe geschrieben wurde
	 */
    void activateOutputCache() throws IllegalStateException;
	
	/**
	 * Setzt den aktuellen Content-Typ.
	 * @param contentType der neue Content-Typ
	 */
    void setContentType(String contentType);
	
	/**
	 * Setzt den aktuellen Content-Typ.
	 * @param contentType der neue Content-Typ
	 * @param charSet Setzt das aktuelle Character Set
	 */
    void setContentType(String contentType, String charSet);
	
	/**
	 * Setzt die erwartete Content-Laenge.
	 * @param length die erwartete Content-Laenge
	 */
    void setContentLength(int length);

	/**
	 * Gibt einen Writer zum Schreiben der Ausgabe zurueck.
	 * @return Der Writer
	 * @throws IOException 
	 */
    Writer getWriter() throws IOException;
	
	/**
	 * Liefert den Ausgabestrom.
	 * @return Der Ausgabestrom der Response
	 * @throws IOException
	 */
    OutputStream getOutputStream() throws IOException;
	
	/**
	 * Setzt den HTTP-Statuscode der Antwort.
	 * @param status der Statuscode
	 */
    void setStatus(int status);
	
	/**
	 * Setzt einen Wert im Kopf der Antwort.
	 * @param name Name des Wertes
	 * @param value Der Wert selbst
	 */
    void setHeader(String name, String value);
	
	/**
	 * Sendet die Antwort.
	 * @throws IOException
	 */
    void send() throws IOException;

	/**
	 * Setzt den internen Status auf manuelles senden.
	 * In diesem Fall sendet das Objekt selbst keine Daten mehr.
	 *
	 */
    void setManualSendStatus();
	
	/**
	 * Antwortet dem Client mit einer Weiterleitung auf die angegebene
	 * URL.
	 * @param url Die Ziel-URL
	 */
    void redirectTo(String url);
	
	/**
	 * Setzt einen Cookie.
	 * 
	 * @param name Name des Cookie.
	 * @param value Wert des Cookie.
	 * @param expiry Lebenszeit des Cookie.
	 */
    void setCookie(String name, String value, int expiry);
}
