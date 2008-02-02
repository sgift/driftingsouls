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
package net.driftingsouls.ds2.server.entities;

/**
 * Spezifiziert den Aufenthaltsort des Flagschiffs eines Spielers
 * @author Christopher Jung
 *
 */
public class UserFlagschiffLocation implements Cloneable {
	/**
	 * Der Typ des Aufenthaltsortes
	 * @author Christopher Jung
	 *
	 */
	public enum Type {
		/**
		 * Das Schiff wird in einer Werft auf einer Basis gebaut
		 */
		WERFT_BASE,
		/**
		 * Das Schiff wird in einer Werft auf einem Schiff gebaut
		 */
		WERFT_SHIP,
		/**
		 * Das Flagschiff existiert bereits als fertiges Schiff
		 */
		SHIP;
	}
	private int id;
	private Type type;
		
	protected UserFlagschiffLocation(Type type, int id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * Gibt die ID des Flagschiffs bzw des Schiffs/der Basis, das es baut zurueck
	 * @return Die ID des Schiffs
	 */
	public int getID() {
		return id;
	}

	/**
	 * Der Typ des Aufenthaltsortes des Flagschiffs
	 * @return Returns the type.
	 */
	public Type getType() {
		return type;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		}
		catch( CloneNotSupportedException e ) {
			e.printStackTrace();
		}
		return null;
	}	
}
