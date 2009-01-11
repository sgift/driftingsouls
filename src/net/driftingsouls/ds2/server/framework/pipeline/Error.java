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

/**
 * Error repraesentiert einen Fehler innerhalb des Frameworks. Jeder Fehler besteht mindestens
 * aus der Fehlerbeschreibung. Optional kann noch eine Ausweich-URL angeben werden, welche
 * dann als Link dem User zur Verf?gung gestellt wird.
 * 
 * @author bktheg
 *
 */
public class Error {
	private String description;
	private String url;
	
	/**
	 * Erstellt ein neues Fehlerobject.
	 * 
	 * @param description Die Beschreibung des Fehlers
	 */
	public Error( String description ) {
		this.url = null;
		this.description = description;
	}
	
	/**
	 * Erstellt ein neues Fehlerobjekt.
	 * 
	 * @param description Die Beschreibung des Fehlers
	 * @param url Die Ausweich-URL
	 */
	public Error( String description, String url ) {
		this(description);
		this.url = url;
	}
	
	/**
	 * Liefert die Beschreibung des Fehlers zurueck.
	 * 
	 * @return Die Fehlerbeschreibung
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Liefert die zum Fehler gehoerende Ausweich-URL oder, falls keine URL gesetzt wurde, null.
	 * 
	 * @return Ausweich-URL oder null
	 */
	public String getUrl() {
		return url;
	}
}
