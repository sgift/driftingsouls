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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.UnmodifiableCargo;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

/**
 * Die Warenkurse an einem Ort.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="gtu_warenkurse")
@Immutable
public class GtuWarenKurse {
	@Id
	private String place;
	private String name;
	@Type(type="cargo")
	private Cargo kurse;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public GtuWarenKurse() {
		// EMPTY
	}

	/**
	 * <p>Gibt die Kurse fuer einzelne Waren zurueck.</p>
	 * Achtung! Die Kurse sind in RE * 1000 angegeben!
	 * @return Die Kurse
	 */
	public Cargo getKurse() {
		return kurse;
	}

	/**
	 * Gibt den Namen des Handelsorts zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Der Handelsort.
	 * @return Der Handelsort
	 */
	public String getPlace() {
		return place;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
