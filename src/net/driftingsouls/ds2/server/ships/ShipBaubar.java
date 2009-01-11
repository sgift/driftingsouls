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
package net.driftingsouls.ds2.server.ships;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import net.driftingsouls.ds2.server.cargo.Cargo;

/**
 * Ein Baueintrag fuer einen Schiffstyp.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships_baubar")
@Immutable
public class ShipBaubar {
	@Id
	private int id;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="type", nullable=false)
	private ShipType type;
	@Type(type="cargo")
	private Cargo costs;
	private int crew;
	private int dauer;
	@Column(name="ekosten")
	private int eKosten;
	private int race;
	@Column(name="systemreq")
	private int systemReq;
	private int tr1;
	private int tr2;
	private int tr3;
	@Column(name="werftslots")
	private int werftSlots;
	private boolean flagschiff;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ShipBaubar() {
		// EMPTY
	}

	/**
	 * Gibt die Baukosten zurueck.
	 * @return Die Baukosten
	 */
	public Cargo getCosts() {
		return costs;
	}

	/**
	 * Gibt die zum Bau benoetigte Crew zurueck.
	 * @return Die Crew
	 */
	public int getCrew() {
		return crew;
	}

	/**
	 * Gibt die Baudauer zurueck.
	 * @return Die Dauer
	 */
	public int getDauer() {
		return dauer;
	}

	/**
	 * Gibt die Energiekosten zurueck.
	 * @return Die Energiekosten
	 */
	public int getEKosten() {
		return eKosten;
	}

	/**
	 * Gibt zurueck, ob hergestellte Schiffe Flagschiffe sind.
	 * @return <code>true</code>, falls es Flagschiffe sind
	 */
	public boolean isFlagschiff() {
		return flagschiff;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die Rasse zurueck, die diesen Baueintrag nutzen kann.
	 * @return Die Rasse
	 */
	public int getRace() {
		return race;
	}

	/**
	 * Gibt zurueck, ob das Schiff nur in militaerischen Systenem gebaut werden kann.
	 * @return != 0 falls es nur in nicht militaerischen System gebaut werden kann
	 */
	public int getSystemReq() {
		return systemReq;
	}

	/**
	 * Gibt die benoetigte Forschung zurueck.
	 * @param res Die Nummer der Forschung (1-3)
	 * @return Die Forschung
	 */
	public int getRes(int res) {
		switch(res) {
		case 1:
			return tr1;
		case 2:
			return tr2;
		case 3:
			return tr3;
		}
		throw new IllegalArgumentException("Ungueltige Forschungsnummer '"+res+"'");
	}

	/**
	 * Gibt den Schiffstyp zurueck, welcher hiermit gebaut werden kann.
	 * @return Der Schiffstyp
	 */
	public ShipType getType() {
		return type;
	}

	/**
	 * Gibt die zum Bau benoetigte Werftslots zurueck.
	 * @return Die Werft-Requirements
	 */
	public int getWerftSlots() {
		return werftSlots;
	}
}
