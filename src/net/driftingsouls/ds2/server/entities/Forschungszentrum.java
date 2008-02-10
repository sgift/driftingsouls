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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.bases.Base;

/**
 * Ein Forschungszentrum
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="fz")
public class Forschungszentrum {
	@Id
	private int col;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Base base;
	private int type;
	private int forschung;
	private int dauer;
	
	/**
	 * Konstruktor
	 *
	 */
	public Forschungszentrum() {
		// EMPTY
	}
	
	/**
	 * Konstruktor
	 * @param base Die Basis
	 */
	public Forschungszentrum(Base base) {
		this.base = base;
		this.col = base.getId();
	}

	/**
	 * Gibt die Basis zurueck, auf der das Forschungszentrum steht
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis, auf der das Forschungszentrum steht
	 * @param base Die Basis
	 */
	public void setBase(Base base) {
		this.col = base.getId();
		this.base = base;
	}

	/**
	 * Gibt die verbleibende Forschungsdauer zurueck
	 * @return Die verbleibende Dauer
	 */
	public int getDauer() {
		return dauer;
	}

	/**
	 * Setzt die verbleibende Forschungsdauer
	 * @param dauer Die verbleibende Dauer
	 */
	public void setDauer(int dauer) {
		this.dauer = dauer;
	}

	/**
	 * Gibt die aktuelle Forschung zurueck
	 * @return Die Forschung
	 */
	public int getForschung() {
		return forschung;
	}

	/**
	 * Setzt die aktuelle Forshcung
	 * @param forschung Die Forschung
	 */
	public void setForschung(int forschung) {
		this.forschung = forschung;
	}

	/**
	 * ???
	 * @return Der Typ
	 */
	public int getType() {
		return type;
	}

	/**
	 * ??? 
	 * @param type Der Typ
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gibt die ID der Basis zurueck, auf der das Forschungszentrum steht
	 * @return Die ID Der Basis
	 */
	public int getBaseId() {
		return col;
	}
}
