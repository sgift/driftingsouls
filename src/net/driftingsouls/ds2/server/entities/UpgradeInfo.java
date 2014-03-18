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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Daten zu moeglichen Basis-Ausbauten.
 * @author Christoph Peltz
 *
 */
@Entity
@Table(name="upgrade_info")
public class UpgradeInfo {
	@Id @GeneratedValue
	private int id;
	private int type;
	private int mod;
	private boolean cargo;
	private int price;
	@Column(name="miningexplosive", nullable = false)
	private int miningExplosive;
	private int ore;

	/**
	 * Konstruktor.
	 *
	 */
	public UpgradeInfo() {
		// EMPTY
	}
	
	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}	

	/**
	 * Setzt die ID.
	 * @param id Die neue ID
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gibt die Klasse der Asteroiden zurueck, fuer die der Ausbau zutrifft.
	 * @return Die Klasse des Asteroiden
	 */
	public int getType() {
		return type;
	}

	/**
	 * Setzt die Klasse.
	 * @param type Die neue Klasse des Asteroiden
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gibt den Zahlenwert der Modifikation zurueck.
	 * @return Zahlenwert der Modifikation
	 */
	public int getMod() {
		return mod;
	}

	/**
	 * Setzt den Zahlenwert der Modifikation.
	 * @param mod Zahlenwert der Modifikation
	 */
	public void setMod(int mod) {
		this.mod = mod;
	}

	/**
	 * Weist aus, ob die Modifikation die Felder oder den Cargo betrifft.
	 * @return Cargo-Flag
	 */
	public boolean getCargo() {
		return cargo;
	}

	/**
	 * Setzt das Cargo-oder-Felder-Flag.
	 * @param cargo Cargo-Flag
	 */
	public void setCargo(boolean cargo) {
		this.cargo = cargo;
	}

	/**
	 * Gibt den Preis zurueck.
	 * @return Der Preis
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * Setzt den Preis.
	 * @param price Preis
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * Gibt die Anzahl noetigen BergBauSprengstoffes zurueck.
	 * @return Anzahl BBS
	 */
	public int getMiningExplosive() {
		return miningExplosive;
	}

	/**
	 * Setzt die noetige Menge BBS.
	 * @param miningExplosive Anzahl BBS
	 */
	public void setMiningExplosive(int miningExplosive) {
		this.miningExplosive = miningExplosive;
	}

	/**
	 * Gibt die Anzahl noetigen Erzes zurueck.
	 * @return Anzahl Erz
	 */
	public int getOre() {
		return ore;
	}

	/**
	 * Setzt die Menge des benoetigten Erzes.
	 * @param ore Anzahl Erz
	 */
	public void setOre(int ore) {
		this.ore = ore;
	}
}
