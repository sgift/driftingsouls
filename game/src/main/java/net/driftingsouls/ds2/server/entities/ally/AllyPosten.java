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
package net.driftingsouls.ds2.server.entities.ally;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;

/**
 * Ein Allianzposten.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ally_posten")
public class AllyPosten {
	@Id @GeneratedValue
	private int id;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="ally", nullable = false)
	@ForeignKey(name = "ally_posten_fk_ally")
	private Ally ally;

	@Column(nullable = false)
	private String name;
	
	@OneToOne(mappedBy="allyposten",optional=true,fetch=FetchType.LAZY)
	private User user;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public AllyPosten() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param ally Die Allianz, zu der der Posten gehoert
	 * @param name Der Name des Postens
	 */
	public AllyPosten(Ally ally, String name) {
		setAlly(ally);
		setName(name);
	}

	/**
	 * Gibt die Allianz zurueck, der der Allyposten gehoert.
	 * @return Die Allianz
	 */
	public Ally getAlly() {
		return ally;
	}

	/**
	 * Setzt die Allianz, der der Allyposten gehoert.
	 * @param ally Die neue Allianz
	 */
	public final void setAlly(final Ally ally) {
		this.ally = ally;
	}

	/**
	 * Gibt den Namen des Postens zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Postens.
	 * @param name Der neue Name
	 */
	public final void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gibt die ID des Postens zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den assoziierten Benutzer zurueck.
	 * @return Der Benutzer
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den assoziierten User.
	 * Diese Operation aktuallisiert <b>nicht</b> die Datenbank.
	 * @param user Der neue User
	 * @see net.driftingsouls.ds2.server.entities.User#setAllyPosten(AllyPosten)
	 */
	public void setUser(User user) {
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
