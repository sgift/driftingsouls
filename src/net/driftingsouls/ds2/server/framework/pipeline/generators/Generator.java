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
package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.io.IOException;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;


/**
 * Basisklasse fuer alle Controller
 * 
 * @author bktheg
 *
 */
public abstract class Generator {
	private Context context;
	private String browser;
	
	/**
	 * Konstruktor
	 * @param context Der Kontext
	 */
	public Generator(Context context) {
		this.context = context;
		
		String browser = getRequest().getHeader("user-agent");
		if( browser != null ) {		
			browser = browser.toLowerCase();
			
			if( browser.indexOf("opera") > -1  ) {
				browser = "opera";
			}
			else if( browser.indexOf("msie") > -1 ) {
				browser = "msie";
			}
			else {
				browser = "mozilla";
			}
			this.browser = browser;
		}
		else {
			this.browser = "unknown";
		}
	}
	
	/**
	 * Liefert eine Instanz der Datenbank-Klasse zurueck
	 * 
	 * @return Eine Database-Instanz
	 * @deprecated use getDB() (Hibernate)
	 */
	@Deprecated
	public final Database getDatabase() {
		return context.getDatabase();
	}
	
	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu
	 * 
	 * @param error Die Beschreibung des Fehlers
	 */
	public final void addError( String error ) {
		context.addError(error);
	}
	
	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu und bietet zudem eine Ausweich-URL an.
	 * 
	 * @param error Die Beschreibung des Fehlers
	 * @param link Die Ausweich-URL
	 */
	public final void addError( String error, String link ) {
		context.addError(error, link);
	}
	
	/**
	 * Liefert den letzten Fehler zurueck
	 * 
	 * @return Der letzte Fehlers
	 * 
	 * @see #addError(String, String)
	 * @see #addError(String)
	 */
	public final Error getLastError() {
		return context.getLastError();
	}
	
	/**
	 * Liefert eine Liste aller Fehler zurueck
	 * 
	 * @return Eine Liste aller Fehlerbeschreibungen 
	 */
	public final Error[] getErrorList() {
		return context.getErrorList();
	}
	
	/**
	 * Liefert die Request fuer diesen Aufruf
	 * @return Die Request des Aufrufs
	 */
	public final Response getResponse() {
		return context.getResponse();
	}

	/**
	 * Liefert die zum Aufruf gehoerende Response
	 * @return Die Response des Aufrufs
	 */
	public final Request getRequest() {
		return context.getRequest();
	}
	
	/**
	 * Gibt den aktuellen Kontext zurueck
	 * @return Der Kontext
	 */
	public final Context getContext() {
		return context;
	}
	
	/**
	 * Gibt die aktuelle Hibernate-Session zurueck
	 * @return Die aktuelle Hibernate-Session
	 */
	public final org.hibernate.Session getDB() {
		return context.getDB();
	}

	/**
	 * Gibt den aktiven User zurueck. Falls kein User eingeloggt ist
	 * wird <code>null</code> zurueckgegeben
	 * @return Der User oder <code>null</code>
	 */
	public BasicUser getUser() {
		return getContext().getActiveUser();
	}

	/**
	 * Fueht die angegebene Aktion aus
	 * @param action Der Name der Aktion
	 * @throws IOException 
	 */
	public abstract void handleAction( String action ) throws IOException;
	
	/**
	 * Gibt den Identifikationsstring des Browsers des Spielers zurueck
	 * @return Der Identifikationsstring des Browsers
	 */
	public final String getBrowser() {
		return browser;
	}
}