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


/**
 * Authentifizierungsfehler, da sich der Account noch im Vacationmodus befindet.
 * @author Christopher Jung
 *
 */
public class AccountInVacationModeException extends RuntimeException {
	private static final long serialVersionUID = 4226868012372360527L;
	
	private int dauer;
	
	/**
	 * Konstruktor.
	 * @param dauer Die verbleibende Vacationdauer in Ticks
	 */
	public AccountInVacationModeException(int dauer) {
		super("Der Account befindet sich im Vacationmodus");
		this.dauer = dauer;
	}

	/**
	 * Gibt die noch verbleibende Zeit im Vacationmodus in Ticks zurueck.
	 * @return Die Restdauer in Ticks
	 */
	public int getDauer() {
		return this.dauer;
	}
}
