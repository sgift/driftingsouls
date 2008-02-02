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
package net.driftingsouls.ds2.server.webservices;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;

/**
 * Basisklasse fuer WebServices
 * @author Christopher Jung
 *
 */
public abstract class BasicWebService {
	private Context context;
	
	/**
	 * Konstruktor
	 *
	 */
	public BasicWebService() {
		this.context = ContextMap.getContext();
	}
	
	/**
	 * Gibt eine Instanz der Datenbankverbindung zurueck
	 * @return Eine Datenbankverbindung
	 */
	public Database getDatabase() {
		return context.getDatabase();
	}
	
	/**
	 * Gibt den Kontext zurueck
	 * @return Der Kontext
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * Gibt den aktiven Benutzer oder <code>null</code>, falls kein aktiver
	 * Benutzer existiert, zurueck
	 * @return Der aktive Benutzer oder <code>null</code>
	 */
	public User getUser() {
		return (User)context.getActiveUser();
	}
	
	/**
	 * Ueberprueft, ob ein Benutzer authentifiziert ist.
	 * Falls dies nicht der Fall ist, wird eine Exception geworfen
	 * @throws WebServiceException
	 */
	public void requireAuthentication() throws WebServiceException {
		if( getUser() == null ) {
			throw new WebServiceException("Authentication required");
		}
	}
}
