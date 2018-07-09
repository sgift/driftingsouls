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
package net.driftingsouls.ds2.server.user.authentication;

import net.driftingsouls.ds2.server.framework.authentication.AuthenticationException;

/**
 * Fehler beim Authentifizieren weil der Tick gerade den Account gesperrt hat.
 * @author Christopher Jung
 *
 */
public class TickInProgressException extends AuthenticationException {
	private static final long serialVersionUID = 7683711830944450958L;

	/**
	 * Konstruktor.
	 *
	 */
	public TickInProgressException() {
		super("Es werden gerade einige Tickberechnungen durchgeführt");
	}
}
