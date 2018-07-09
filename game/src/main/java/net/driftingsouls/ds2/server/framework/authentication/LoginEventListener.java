/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.framework.BasicUser;

/**
 * Listener fuer ein Login-Ereignis.
 * Ein Listener wird aufgerufen, bevor der Loginvorgang abgeschlossen
 * und die Session angelegt wurde. Er hat daher die Moeglichkeit den
 * Loginvorgang mit einem Fehler abzubrechen. 
 * @author Christopher Jung
 *
 */
public interface LoginEventListener {
	/**
	 * Callback fuer das OnLogin-Ereignis. Wird aufgerufen, nachdem
	 * der Login einem Account zugeordet wurde, aber bevor die Session
	 * angelegt ist.
	 * @param user Der Useraccount
	 * @throws AuthenticationException Falls die Authentifizierung fehlschlagen soll
	 */
    void onLogin(BasicUser user) throws AuthenticationException;
}
