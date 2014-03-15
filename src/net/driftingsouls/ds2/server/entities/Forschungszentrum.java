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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.bases.Base;
import org.hibernate.annotations.ForeignKey;

/**
 * Ein Forschungszentrum.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="fz")
public class Forschungszentrum {
	@Id @GeneratedValue
	private int id;
	@OneToOne(fetch=FetchType.LAZY, mappedBy="forschungszentrum")
	private Base base;
	private int type;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="forschung", nullable=true)
	@ForeignKey(name="fz_fk_forschungen")
	private Forschung forschung;
	private int dauer;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Forschungszentrum() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param base Die Basis
	 */
	public Forschungszentrum(Base base) {
		this.base = base;
	}

	/**
	 * Gibt die Basis zurueck, auf der das Forschungszentrum steht.
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis, auf der das Forschungszentrum steht.
	 * @param base Die Basis
	 */
	public void setBase(Base base) {
		this.base = base;
	}

	/**
	 * Gibt die verbleibende Forschungsdauer zurueck.
	 * @return Die verbleibende Dauer
	 */
	public int getDauer() {
		return dauer;
	}

	/**
	 * Setzt die verbleibende Forschungsdauer.
	 * @param dauer Die verbleibende Dauer
	 */
	public void setDauer(int dauer) {
		this.dauer = dauer;
	}

	/**
	 * Gibt die aktuelle Forschung zurueck.
	 * @return Die Forschung
	 */
	public Forschung getForschung() {
		return forschung;
	}

	/**
	 * Setzt die aktuelle Forschung.
	 * @param forschung Die Forschung
	 */
	public void setForschung(Forschung forschung) {
		this.forschung = forschung;
	}

	/**
	 * ???.
	 * @return Der Typ
	 */
	public int getType() {
		return type;
	}

	/**
	 * ??? .
	 * @param type Der Typ
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
