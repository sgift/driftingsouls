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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Ein Baueintrag fuer einen Schiffstyp.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships_baubar")
public class ShipBaubar {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="type", nullable=false)
	@ForeignKey(name="ships_baubar_type_fk")
	private ShipType type;

	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo costs;
	private int crew;
	private int dauer;
	@Column(name="ekosten", nullable = false)
	private int eKosten;
	private int race;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="ships_baubar_fk_forschung1")
	private Forschung res1;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="ships_baubar_fk_forschung2")
	private Forschung res2;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="ships_baubar_fk_forschung3")
	private Forschung res3;
	@Column(name="werftslots", nullable = false)
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
	 * Konstruktor.
	 * @param shiptype der Schiffstyp
	 */
	public ShipBaubar(ShipType shiptype) {
		this.type = shiptype;
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
	 * Gibt alle benoetigten Forschungen zurueck.
	 * @return Die Forschungen
	 */
	public Set<Forschung> getBenoetigteForschungen()
	{
		Set<Forschung> result = new HashSet<>();
		if( this.res1 != null )
		{
			result.add(this.res1);
		}
		if( this.res2 != null )
		{
			result.add(this.res2);
		}
		if( this.res3 != null )
		{
			result.add(this.res3);
		}
		return result;
	}

	/**
	 * Gibt die benoetigte Forschung zurueck.
	 * @param res Die Nummer der Forschung (1-3)
	 * @return Die Forschung
	 */
	public Forschung getRes(int res) {
		switch(res) {
		case 1:
			return res1;
		case 2:
			return res2;
		case 3:
			return res3;
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
	 * Setzt den Schiffstyp, welcher hiermit gebaut werden kann.
	 * @param type Der Schiffstyp
	 */
	public void setType(ShipType type)
	{
		this.type = type;
	}

	/**
	 * Gibt die zum Bau benoetigte Werftslots zurueck.
	 * @return Die Werft-Requirements
	 */
	public int getWerftSlots() {
		return werftSlots;
	}
	
	/**
	 * Setzt die Baukosten.
	 * @param costs Die Baukosten
	 */
	public void setCosts(Cargo costs) {
		this.costs = costs;
	}
	
	/**
	 * Setzt die benoetigte Crew.
	 * @param crew Die Crew
	 */
	public void setCrew(int crew) {
		this.crew = crew;
	}
	
	/**
	 * Setzt die Dauer des Baus.
	 * @param dauer Die Dauer
	 */
	public void setDauer(int dauer) {
		this.dauer = dauer;
	}
	
	/**
	 * Setzt die Energiekosten.
	 * @param eKosten Die Energiekosten
	 */
	public void setEKosten(int eKosten) {
		this.eKosten = eKosten;
	}
	
	/**
	 * Setzt, ob das Schiff ein Flagschiff ist, oder nicht.
	 * @param isFlagschiff <code>true</code> wenn es sich um ein Flagschiff handelt ansonsten <code>false</code>
	 */
	public void setFlagschiff(boolean isFlagschiff) {
		this.flagschiff = isFlagschiff;
	}
	
	/**
	 * Setzt die zum Bau benoetigte Rasse.
	 * @param race Die Rasse
	 */
	public void setRace(int race) {
		this.race = race;
	}
	
	/**
	 * Setzt die Forschung Nummer 1.
	 * @param res Die ID der neuen Forschung
	 */
	public void setRes1(Forschung res) {
		this.res1 = res;
	}
	
	/**
	 * Setzt die Forschung Nummer 2.
	 * @param res Die ID der neuen Forschung
	 */
	public void setRes2(Forschung res) {
		this.res2 = res;
	}
	
	/**
	 * Setzt die Forschung Nummer 3.
	 * @param res Die ID der neuen Forschung
	 */
	public void setRes3(Forschung res) {
		this.res3 = res;
	}
	
	/**
	 * Setzt die Anzahl der benoetigten Werftslots.
	 * @param werftSlots Die Anzahl der Werftslots
	 */
	public void setWerftSlots(int werftSlots) {
		this.werftSlots = werftSlots;
	}
}
