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
package net.driftingsouls.ds2.server.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;

/**
 * Beschreibt einen Sektor oder eine Gruppe von Sektoren.
 * Die Position legt dabei fest welche Sektoren gemeint sind.
 * Die Werte koennen dabei normale Positionsdaten oder -1
 * (fuer alle moeglichen Werte - vergleichbar einer Wildcard)
 * sein.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="sectors")
public class Sector implements Locatable {
	@Id
	private MutableLocation loc;
	private int objects;
	@Lob
	@Column(name="onenter", nullable = false)
	private String onEnter;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Sector() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Sektoreintrag.
	 * @param loc Der Sektor
	 */
	public Sector(MutableLocation loc) {
		this.loc = loc;
		this.onEnter = "";
	}

	/**
	 * Gibt die Position zurueck.
	 * @return Die Position
	 */
	@Override
	public Location getLocation() {
		return loc.getLocation();
	}

	/**
	 * Gibt die Objektdaten zurueck (?).
	 * @return Die Objektdaten
	 */
	public int getObjects() {
		return objects;
	}

	/**
	 * Setzt die Objektdaten (?).
	 * @param object Die Objektdaten
	 */
	public void setObjects(int object) {
		this.objects = object;
	}

	/**
	 * Gibt die Daten des OnEnter-Ereignisses zurueck.
	 * @return Die Daten
	 */
	public String getOnEnter() {
		return onEnter;
	}

	/**
	 * Setzt die Daten des OnEnter-Ereignisses.
	 * @param onEnter Die Daten
	 */
	public void setOnEnter(String onEnter) {
		this.onEnter = onEnter;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
