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
package net.driftingsouls.ds2.server.framework.pipeline.actions;

import net.driftingsouls.ds2.server.framework.Context;

/**
 * Ueberprueft, ob ein Benutzer eingeloggt ist bzw (auf Wunsch), ob der eingeloggte 
 * Benutzer mit dem angegebenen uebereinstimmt.
 * @author Christopher Jung
 *
 */
public class AuthenticateAction implements Action {
	private Integer userid = null;
	
	/**
	 * Konstruktor.
	 *
	 */
	public AuthenticateAction() {
		// EMPTY
	}

	@Override
	public void reset() {
		userid = null;
	}

	@Override
	public boolean action(Context context) {
		if( context.getActiveUser() == null ) {
			return false;
		}
		return  (userid == null) || (context.getActiveUser().getId() == userid);
	}

	@Override
	public void setParameter(String name, String value) {
		if( name.equals("userid") ) {
			userid = Integer.parseInt(value);
		}
	}

}
