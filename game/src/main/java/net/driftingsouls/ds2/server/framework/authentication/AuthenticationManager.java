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
 * Interface fuer Authentifizierungsdienste.
 * @author Christopher Jung
 *
 */
public interface AuthenticationManager {

	/**
	 * Loggt einen Benutzer ein.
	 * @param username Der Benutzername
	 * @param password Das Passwort im Klartext
	 * @param rememberMe <code>true</code>, wenn der Spieler eingeloggt bleiben will.
	 * @return Der Account des eingeloggten Benutzers
	 * @throws AuthenticationException Falls der Loginvorgang nicht erfolgreich ist
	 */
    BasicUser login(String username, String password, boolean rememberMe)
			throws AuthenticationException;

	/**
	 * Loggt den mit dem aktuellen Context verknuepften Benutzer aus.
	 */
    void logout();
	
	/**
	 * Loggt einen Admin in einen Account ein. Es findet dabei keine Ueberpruefung
	 * des Accesslevels statt.
	 * @param user Der Account, in den der Admin eingeloggt werden soll
	 * @param attach <code>true</code>, falls die Adminrechte an die neue Session "angeklebt" werden sollen
	 * @return Der Account des eingeloggten Benutzers
	 * @throws AuthenticationException Falls der Loginvorgang nicht moeglich ist
	 */
    BasicUser adminLogin(BasicUser user, boolean attach) throws AuthenticationException;

	/**
	 * Authentifiziert die genutzte Session. Falls die Session ungueltig ist,
	 * oder ein sonstiger Fehler auftritt wird ein entsprechender Fehlertext dem 
	 * Context hinzugefuegt.
	 * Setzt Inaktivitaet zurueck und prueft, ob eine permanente Session vorliegt.
	 * Diese Funktion kann mit automaticAccess gesteuert werden.
	 * 
	 * @return <code>true</code>, if the user is logged in, <code>false</code> otherwise.
	 */
    boolean authenticateCurrentSession();
	
	/**
	 * Checks, if the player is remembered by ds.
	 * 
	 * @return <code>true</code> if ds remembers the player, <code>false</code> otherwise.
	 */
    boolean isRemembered();
}
