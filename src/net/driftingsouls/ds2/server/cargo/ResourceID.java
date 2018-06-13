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
package net.driftingsouls.ds2.server.cargo;

/**
 * Interface fuer Resourcen-IDs.
 * @author Christopher Jung
 *
 */
public interface ResourceID {
	/**
	 * Gibt die ID des Items zurueck, falls es sich um ein Item handelt.
	 * @return Die ID des Items
	 */
    int getItemID();

	/**
	 * Gibt die Anzahl der verbleibenden Benutzungen des Items zurueck, falls
	 * es sich um eine Item-Resource handelt.
	 * @return Die Anzal der verbleibenden Benutzungen des Items
	 */
    int getUses();

	/**
	 * Die ID des Quests des Items, falls
	 * es sich um eine Item-Resource handelt.
	 * @return Die ID des Quests des Items
	 */
    int getQuest();
}
