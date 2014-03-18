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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.cargo.Cargo;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

/**
 * Ein Statistikeintrag ueber den Gesamtcargo eines Spielers (zum letzten Berechnungszeitpunkt).
 * @author Chirstopher Jung
 *
 */
@Entity
@Table(name="stats_user_cargo")
public class StatUserCargo {
	@Id @GeneratedValue
	private Long id;
	
	@OneToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="user_id", nullable = false)
	@ForeignKey(name="stats_user_cargo_fk_user_id")
	private User user;
	
	@Type(type="largeCargo")
	@Column(nullable = false)
	private Cargo cargo;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public StatUserCargo() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Statistikeintrag.
	 * @param user Der User
	 * @param cargo Der Cargo
	 */
	public StatUserCargo(User user, Cargo cargo) {
		setUser(user);
		setCargo(cargo);
	}

	/**
	 * Gibt den Cargo des Spielers zurueck.
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return cargo;
	}

	/**
	 * Setzt den Cargo des Spielers.
	 * @param cargo Der Cargo
	 */
	public final void setCargo(final Cargo cargo) {
		this.cargo = cargo;
	}

	/**
	 * Gibt den Spieler zurueck.
	 * @return Der Spieler
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Spieler.
	 * @param user Der Spieler
	 */
	public final void setUser(final User user) {
		this.user = user;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
