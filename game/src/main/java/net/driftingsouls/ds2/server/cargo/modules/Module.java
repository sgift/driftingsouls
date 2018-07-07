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
package net.driftingsouls.ds2.server.cargo.modules;

import java.util.List;

import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Basisklasse fuer (Schiffs)module.
 * @author Christopher Jung
 *
 */
public abstract class Module {
	/**
	 * Wendet den Effekt des Moduls auf Schiffstypen-Daten an. Die modifizierten Schiffsdaten werden zurueckgegeben.
	 * @param stats Die aktuellen Schiffstypen-Daten
	 * @param moduleobjlist Liste aller Module in diesem Schiff
	 * @return die modifizierten Schiffstypen-Daten
	 */
	public abstract ShipTypeData modifyStats( ShipTypeData stats, List<Module> moduleobjlist );

	/**
	 * Prueft, ob die angegebenen Daten das selbe Modul kennzeichnen die das aktuelle.
	 * @param entry Der Moduleintrag
	 * @return <code>true</code>, falls die Daten zum Modul passen
	 */
	public boolean isSame( ModuleEntry entry ) {
		return false;
	}

	/**
	 * Gibt den Namen des Moduls zurueck.
	 * @return der Name
	 */
	public String getName() {
		return "Noname";
	}

	/**
	 * Setzt Modul-Typ spezifische Daten.
	 * @param data Die Daten
	 */
	public abstract void setSlotData( String data );
}
