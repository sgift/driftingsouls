/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
package net.driftingsouls.ds2.server.comm;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repraesentiert einen Ordner im Postfach.
 *
 * Hinweis: Die Ordner-ID 0 hat eine spezielle Bedeutung.
 * Sie kennzeichnet den Hauptordner, in dem sich alle Unterordner
 * befinden. Der Hauptordner existiert jedoch nicht als eigenstaendiger
 * Ordner in der Datenbank.
 * @author Christoph Peltz
 * @author Christopher Jung
 */
@Entity
@Table(name="ordner")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Ordner {
	/**
	 * Ein normaler Ordner.
	 */
	public static final int FLAG_NORMAL = 0;
	/**
	 * Der Muelleimer.
	 */
	public static final int FLAG_TRASH 	= 1;

	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="playerid", nullable=false)
	@ForeignKey(name="ordner_fk_users")
	private User owner;
	private int flags;
	private int parent;
	@Version
	private int version;

	/**
	 * Konstruktor.
	 */
	public Ordner() {
		// EMPTY
	}

	public Ordner(String name, User owner, Ordner parent) {
		this.name = name;
		this.owner = owner;
		this.parent = parent.getId();
	}

	/**
	 * Gibt die ID es Ordners zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Gibt den Namen des Ordners zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Setzt den Namen des Ordners.
	 * @param name der neue Name
	 */
	public void setName( String name ) {
		this.name = name;
	}


	/**
	 * Gibt den Eltern-Ordner zurueck.
	 * @return Der Elternordner
	 */
	public int getParent() {
		return parent;
	}

	/**
	 * Setzt den Elternordner.
	 * @param parent Der Elternordner
	 */
	public void setParent(Ordner parent) {
		this.parent = parent.getId();
	}

	/**
	 * Gibt die Flags des Ordners zurueck.
	 * @return Die Flags
	 */
	public int getFlags() {
		return this.flags;
	}

	/**
	 * Setzt die Flags des Ordners.
	 * @param flags Die Flags
	 */
	public void setFlags(int flags) {
		this.flags = flags;
	}

	/**
	 * Prueft, ob der Ordner das angegebene Flag besitzt.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls der Ordner das Flag besitzt
	 */
	public boolean hasFlag(int flag) {
		return (this.flags & flag) != 0;
	}

	/**
	 * Gibt den Besitzer des Ordners zurueck.
	 * @return Der Besitzer
	 */
	public User getOwner() {
		return this.owner;
	}

	/**
	 * Setzt den Besitzer des Ordners.
	 * @param owner Der Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	@Override
	public int hashCode() {
		int result = 31 + id;
		return 31 * result + owner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == null ) {
			return false;
		}

		if( getClass() != obj.getClass() ) {
			return false;
		}

		if( this.id != ((Ordner)obj).id ) {
			return false;
		}

		return this.owner.equals(((Ordner) obj).owner);
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}

	public void setId(int id) {
		this.id = id;
	}
}
