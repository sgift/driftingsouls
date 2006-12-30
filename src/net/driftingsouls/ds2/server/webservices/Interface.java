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

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Standardwebservice von DS
 * @author Christopher Jung
 *
 */
public class Interface extends BasicWebService {
	private static Map<String,String> validUserValues = new HashMap<String,String>();
	static {
		validUserValues.put("TBLORDER/clients/jstarmap/bufferedoutput", "boolean");
	}
	
	/**
	 * Gibt den Namen eines Schiffes eigenen Schiffes zurueck
	 * @param shipid Die ID des Schiffes
	 * @return Der Name
	 * @throws WebServiceException
	 */
	public String identifyShip( int shipid ) throws WebServiceException {
		requireAuthentication();

		SQLResultRow name = null;
		if( shipid > 0 ) {
			name = getDatabase().first("SELECT name FROM ships WHERE owner=",getUser().getID()," AND id=",shipid);
		}
		if( name == null || name.isEmpty() ) {
			throw new WebServiceException("Ungueltige Schiffs-ID");
		}

		return name.getString("name");
	}

	/**
	 * Gibt den Namen einer eigenen Basis zurueck
	 * @param baseid Die ID der Basis
	 * @return Der Name
	 * @throws WebServiceException
	 */
	public String identifyBase( int baseid ) throws WebServiceException {
		requireAuthentication();

		SQLResultRow name = null;
		if( baseid > 0 ) {
			name = getDatabase().first("SELECT name FROM bases WHERE owner=",getUser().getID()," AND id=",baseid);
		}
		if( name == null || name.isEmpty() ) {
			throw new WebServiceException("Ungueltige Basis-ID");
		}

		return name.getString("name");
	}

	/**
	 * Prueft, ob die verwendete Session ok ist
	 * @return <code>true</code>, falls die Session OK ist
	 */
	public boolean validateSessID() {
		return getUser() != null;
	}

	/**
	 * Gibt zurueck, wieviel neue PMs der aktuelle Spieler hat
	 * @return Die Anzahl der neuen PMs
	 * @throws WebServiceException
	 */
	public int hasNewPM() throws WebServiceException {
		requireAuthentication();

		return getDatabase().first("SELECT count(*) newmsgs FROM transmissionen WHERE empfaenger=",getUser().getID()," AND gelesen=0").getInt("newmsgs");
	}

	/**
	 * Fuehrt ein Admin-Kommando aus
	 * @param command Das Kommand
	 * @return Der Rueckgabewert des Kommandos
	 * @throws WebServiceException
	 */
	public String admin_execcmd( String command ) throws WebServiceException {
		requireAuthentication();
		
		String ret = "";
		//$ret = libadmin_execadmincmd( $db, $id, $command );
		
		return ret;
	}

	/**
	 * Prueft, ob der aktuelle Spieler Adminrechte besitzt 
	 * @return <code>true</code>, falls der Spieler Adminrechte besitzt
	 */
	public boolean admin_isAdmin() {
		if( getUser() == null || getUser().getAccessLevel() < 20 ) {
			return false;
		}
		return true;
	}

	/**
	 * Gibt den Wert eines Uservalues fuer den aktuellen Spieler zurueck
	 * @param uservalue Das Uservalue
	 * @return Der Wert
	 * @throws WebServiceException
	 */
	public String getUserValue( String uservalue ) throws WebServiceException {
		requireAuthentication();
		
		if( !validUserValues.containsKey(uservalue) ) {
			throw new WebServiceException("Uservalue ungueltig");
		}
		
		return getUser().getUserValue(uservalue);
	}

	/**
	 * Setzt ein Uservalue fuer den aktuellen Spieler auf einen neuen Wert
	 * @param uservalue Das Uservalue
	 * @param newvalue Der neue Wert
	 * @throws WebServiceException
	 */
	public void setUserValue( String uservalue, String newvalue ) throws WebServiceException {
		requireAuthentication();
		
		if( !validUserValues.containsKey(uservalue) ) {
			throw new WebServiceException("Uservalue ungueltig");
		}
		
		User user = getUser();
		if( validUserValues.get(uservalue).equals("boolean") ) {
			int intvalue = Integer.parseInt(newvalue);
			if( intvalue != 0 ) {
				intvalue = 1;
			}
			newvalue = Integer.toString(intvalue);
		}
		else if( validUserValues.get(uservalue).equals("number") ) {
			newvalue = Integer.toString(Integer.parseInt(newvalue));
		}

		user.setUserValue(uservalue,newvalue);
	}
}
