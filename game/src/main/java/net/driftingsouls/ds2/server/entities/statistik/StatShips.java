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
package net.driftingsouls.ds2.server.entities.statistik;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Ein Statistikeintrag zur Schiffszahl und zur Crewmenge in DS
 * bei einem bestimmten Tick.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stats_ships")
public class StatShips {
	@Id
	private int tick;
	@Column(name="shipcount", nullable = false)
	private long shipCount;
	@Column(name="crewcount", nullable = false)
	private long crewCount;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public StatShips() {
		//EMPTY
	}
	
	/**
	 * Erstellt einen neuen Statistikeintrag fuer den aktuellen Tick.
	 * @param shipCount Die Schiffsanzahl
	 * @param crewCount Die Crewanzahl
	 */
	public StatShips(long shipCount, long crewCount) {
		this.tick = ContextMap.getContext().get(ContextCommon.class).getTick();
		this.shipCount = shipCount;
		this.crewCount = crewCount;
	}

	/**
	 * Gibt die Crewmenge zurueck.
	 * @return Die Crewmenge
	 */
	public long getCrewCount() {
		return crewCount;
	}

	/**
	 * Setzt die Crewmenge.
	 * @param crewCount Die Crewmenge
	 */
	public void setCrewCount(long crewCount) {
		this.crewCount = crewCount;
	}

	/**
	 * Gibt die Schiffsanzahl zurueck.
	 * @return Die Schiffsanzal
	 */
	public long getShipCount() {
		return shipCount;
	}

	/**
	 * Setzt die Schiffsanzahl.
	 * @param shipCount Die Schiffsanzahl
	 */
	public void setShipCount(long shipCount) {
		this.shipCount = shipCount;
	}

	/**
	 * Gibt den Tick zurueck, zu dem die Werte erhoben wurden.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, zu dem die Werte erhoben wurden.
	 * @param tick Der TIck
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
