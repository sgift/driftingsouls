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
 * Standardinterface fuer die Antwort auf die Request
 * @author Christopher Jung
 *
 */
public interface Response {
	/**
	 * Liefert den aktuell gesetzten Content-Typ
	 * @return der Content-Typ
	 */
	public String getContentType();
	
	/**
	 * Setzt den aktuellen Content-Typ
	 * @param contentType der neue Content-Typ
	 */
	public void setContentType(String contentType);
	
	/**
	 * Setzt die erwartete Content-Laenge
	 * @param length die erwartete Content-Laenge
	 */
	public void setContentLength(int length);
	
	/**
	 * Liefert den Inhaltspuffer fuer Text.
	 * Dieser wird am Schluss in den Ausgabestrom geschrieben.
	 * Bitte nur dann verwenden, wenn der Inhalt auch manipuliert
	 * werden muss.
	 * 
	 * @return Der aktuelle Inhaltspuffer fuer Text
	 */
	public StringBuffer getContent();
	
	/**
	 * Gibt einen Writer zum Schreiben der Ausgabe zurueck.
	 * @return Der Writer
	 */
	public Writer getWriter();
	
	/**
	 * Setzt den Inhaltspuffer fuer Text zurueck
	 *
	 */
	public void resetContent();
	
	/**
	 * Setzt den Inhaltspuffer fuer Text
	 * @param content Der neue Inhalt
	 */
	public void setContent(String content);
	
	/**
	 * Liefert den Ausgabestrom
	 * @return Der Ausgabestrom der Response
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws IOException;
	
	/**
	 * Liefert das aktuell gesetzte CharSet (z.B. UTF-8)
	 * @return das aktuelle Character Set
	 */
	public String getCharSet();
	
	/**
	 * Setzt das aktuelle Character Set
	 * @param charSet das neue Character Set
	 */
	public void setCharSet(String charSet);
	
	/**
	 * Setzt den HTTP-Statuscode der Antwort
	 * @param status der Statuscode
	 */
	public void setStatus(int status);
	
	/**
	 * Setzt einen Wert im Kopf der Antwort
	 * @param name Name des Wertes
	 * @param value Der Wert selbst
	 */
	public void setHeader(String name, String value);
	
	/**
	 * Sendet die Antwort
	 * @throws IOException
	 */
	public void send() throws IOException;

	/**
	 * Setzt den internen Status auf manuelles senden.
	 * In diesem Fall sendet das Objekt selbst keine Daten mehr.
	 *
	 */
	public void setManualSendStatus();
	
	/**
	 * Antwortet dem Client mit einer Weiterleitung auf die angegebene
	 * URL.
	 * @param url Die Ziel-URL
	 */
	public void redirectTo(String url);
}
