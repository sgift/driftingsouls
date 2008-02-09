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
 * Eine Akademie
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="academy")
public class Academy {
	@Id
	private int col;
	@OneToOne(fetch=FetchType.LAZY)
	@PrimaryKeyJoinColumn
	private Base base;
	private int train;
	private int remain;
	private String upgrade;
	
	/**
	 * Konstruktor
	 *
	 */
	public Academy() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Akademie
	 * @param base Die Basis auf der die Akademie steht
	 */
	public Academy(Base base) {
		this.col = base.getId();
		this.base = base;
		this.upgrade = "";
	}

	/**
	 * Gibt die Basis zurueck
	 * @return Die Basis
	 */
	public Base getBase() {
		return base;
	}

	/**
	 * Setzt die Basis auf der sich die Akademie befindet
	 * @param col Die Basis
	 */
	public void setBase(Base col) {
		this.base = col;
	}

	/**
	 * Gibt die verbleibende Ausbildungszeit zurueck
	 * @return Die verbleibende Ausbildungszeit
	 */
	public int getRemain() {
		return remain;
	}

	/**
	 * Setzt die verbleibende Ausbildungszeit
	 * @param remain Die verbleibende Ausbildungszeit
	 */
	public void setRemain(int remain) {
		this.remain = remain;
	}

	/**
	 * Gibt den Typ des auszubildenden Offiziers zurueck
	 * @return Der Typ
	 */
	public int getTrain() {
		return train;
	}

	/**
	 * Setzt den Typ des auszubildenden Offiziers
	 * @param train Der Typ
	 */
	public void setTrain(int train) {
		this.train = train;
	}

	/**
	 * Gibt die Weiterbildungsdaten zurueck
	 * @return Die Weiterbildungsdaten
	 */
	public String getUpgrade() {
		return upgrade;
	}

	/**
	 * Setzt die Weiterbildungsdaten
	 * @param upgrade Die Weiterbildungsdaten
	 */
	public void setUpgrade(String upgrade) {
		this.upgrade = upgrade;
	}

	/**
	 * Gibt die ID der Basis zurueck, auf der sich die Akademie befindet
	 * @return Die ID der Basis
	 */
	public int getBaseId() {
		return col;
	}
}
