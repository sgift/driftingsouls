/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.install.checks;

/**
 * Interface fuer Installationschecks.
 * @author Christopher Jung
 *
 */
public interface Checkable {
	/**
	 * Gibt eine kurze Beschreibung (ca 20 Zeichen) des Checks zurueck.
	 * @return Eine kurze Beschreibung
	 */
    String getDescription();
	
	/**
	 * Fuehrt den Check durch. Wenn der Check fehlschlaegt wird eine {@link CheckFailedException}
	 * geworfen.
	 * @throws CheckFailedException Falls der Check fehlschlaegt
	 */
    void doCheck() throws CheckFailedException;
}
