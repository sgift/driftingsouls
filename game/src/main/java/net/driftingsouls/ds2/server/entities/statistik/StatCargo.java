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

import net.driftingsouls.ds2.server.cargo.Cargo;

import org.hibernate.annotations.Type;

/**
 * Ein Stats-Eintrag, der den Gesamtcargo im Spiel zu einem Zeitpunkt (Tick) 
 * festhaelt.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="stats_cargo")
public class StatCargo {
	@Id
	private int tick;
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo cargo;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected StatCargo() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Eintrag.
	 * @param tick Der Tick
	 * @param cargo Der Cargo
	 */
	public StatCargo(int tick, Cargo cargo) {
		this.tick = tick;
		this.cargo = cargo;
	}

	/**
	 * Gibt den Cargo zurueck.
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return cargo;
	}

	/**
	 * Setzt den Cargo.
	 * @param cargo Der Cargo
	 */
	public void setCargo(Cargo cargo) {
		this.cargo = cargo;
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
