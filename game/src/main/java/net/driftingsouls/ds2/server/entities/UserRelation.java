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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Eine (einseitige) Beziehung zwischen zwei Usern. 
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="user_relations")
@org.hibernate.annotations.Table(
	appliesTo = "user_relations",
	indexes = {@Index(name="user_id", columnNames = {"user_id", "target_id"})}
)
public class UserRelation {
	@Id @GeneratedValue
	private int id;
	
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="user_id", nullable = false)
	@ForeignKey(name="user_relations_fk_users1")
	private User user;
	
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="target_id", nullable = false)
	@ForeignKey(name="user_relations_fk_users2")
	private User target;
	
	private int status;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public UserRelation() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param user Der Ausgangsuser
	 * @param target Der Zieluser
	 * @param status Der Status der Beziehung
	 */
	public UserRelation(User user, User target, int status) {
		setUser(user);
		setTarget(target);
		setStatus(status);
	}

	/**
	 * Gibt den Beziehungsstatus zurueck.
	 * @return Der Beziehungsstatus
	 */
	public final int getStatus() {
		return status;
	}

	/**
	 * Setzt den Beziehungsstatus.
	 * @param status Der neue Status
	 */
	public final void setStatus(final int status) {
		this.status = status;
	}

	/**
	 * Gibt den Zieluser zurueck.
	 * @return Der Zieluser
	 */
	public User getTarget() {
		return target;
	}

	/**
	 * Setzt den Zieluser.
	 * @param target Der Zieluser
	 */
	public final void setTarget(final User target) {
		this.target = target;
	}

	/**
	 * Gibt den Ausgangsuser (Besitzer dieser Beziehung) zurueck.
	 * @return Der Ausgangsuser
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Ausgangsuser (Besitzer dieser Beziehung).
	 * @param user Der neue Ausgangsuser
	 */
	public void setUser(User user) {
		this.user = user;
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
