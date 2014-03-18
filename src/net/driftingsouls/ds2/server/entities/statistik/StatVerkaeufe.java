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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.cargo.Cargo;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

/**
 * Die Verkaufsstatistik an einem Verkaufsort (z.B. Handelsposten, Kommandozentrale)
 * in einem System waehrend eines Ticks.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stats_verkaeufe")
@org.hibernate.annotations.Table(
		appliesTo = "stats_verkaeufe",
		indexes = {@Index(name = "place", columnNames = {"place", "system"})}
)
public class StatVerkaeufe {
	@Id @GeneratedValue
	private int id;
	@Index(name = "tick")
	private int tick;
	@Column(nullable = false)
	private String place;
	@Index(name = "system")
	private int system;
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo stats;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public StatVerkaeufe() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Stat-Eintrag.
	 * @param tick Der Tick
	 * @param system Das System
	 * @param place Der Typ des Verkaufsorts
	 */
	public StatVerkaeufe(int tick, int system, String place) {
		this.tick = tick;
		this.system = system;
		this.place = place;
		this.stats = new Cargo();
	}

	/**
	 * Gibt den Ort zurueck.
	 * @return Der Ort
	 */
	public String getPlace() {
		return place;
	}

	/**
	 * Setzt den Ort.
	 * @param place Der Ort
	 */
	public final void setPlace(String place) {
		this.place = place;
	}

	/**
	 * Gibt die Verkaufsdaten zurueck.
	 * @return Die Verkaufsdaten
	 */
	public Cargo getStats() {
		return stats;
	}

	/**
	 * Setzt die Verkaufsdaten.
	 * @param stats Die Verkaufsdaten
	 */
	public final void setStats(Cargo stats) {
		this.stats = stats;
	}

	/**
	 * Gibt das System zurueck.
	 * @return Das System
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Setzt das System.
	 * @param system Das System
	 */
	public final void setSystem(int system) {
		this.system = system;
	}

	/**
	 * Gibt den Tick zurueck.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick.
	 * @param tick Der Tick
	 */
	public final void setTick(int tick) {
		this.tick = tick;
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
