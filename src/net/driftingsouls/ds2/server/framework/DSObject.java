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
package net.driftingsouls.ds2.server.framework;

/**
 * Basisklasse fuer Objekte innerhalb von DS.
 * @author Christopher Jung
 *
 */
public class DSObject {
	/**
	 * Lognachrichten der zuletzt aufgerufenen Methoden. Die Nachrichten sind Thread-Lokal.
	 */
	public final ContextLocalMessage MESSAGE = new ContextLocalMessage();

	/**
	 * Gibt die Lognachrichten der zuletzt aufgerufenen Methode zurueck.
	 * Der Nachrichtenpuffer wird anschliessend zurueckgesetzt.
	 * @return Die Nachrichten
	 */
	public String getMessage()
	{
		return MESSAGE.getMessage();
	}
}
