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

import net.driftingsouls.ds2.server.cargo.Cargo;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Die Warenkurse an einem Ort.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="gtu_warenkurse")
public class GtuWarenKurse {
	@Id
	private String place;
	@Column(nullable = false)
	private String name;
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo kurse;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected GtuWarenKurse() {
		// EMPTY
	}
	

	/**
	 * Konstruktor.
	 * @param place the place of the tradepost
	 * @param name the name of the tradepost
	 * @param kurse cargo with pricces
	 */
	public GtuWarenKurse(String place, String name, Cargo kurse) {
		this.place = place;
		this.name = name;
		this.kurse = kurse;
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
	 * Sets the price for the items in cargo.
	 * @param kurse cargo with prices
	 */
	public void setKurse(Cargo kurse) {
		this.kurse = kurse;
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
