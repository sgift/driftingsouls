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
package net.driftingsouls.ds2.server.framework;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * <h1>Ein UserValue.</h1>
 * Ein UserValue ist eine Kombination aus Name und Wert, welche an einen
 * User gebunden ist.
 * 
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="user_values")
@org.hibernate.annotations.Table(
	appliesTo = "user_values",
	indexes = {@Index(name="uservalue_id", columnNames = {"user_id", "name"})}
)
public class UserValue {
	@Id @GeneratedValue
	private int id;
	
	@ManyToOne(fetch=FetchType.EAGER, optional = false)
	@JoinColumn(name="user_id", nullable = false)
	@ForeignKey(name="user_values_fk_users")
	private BasicUser user;

	@Column(nullable = false)
	private String name;
	@Lob
	@Column(nullable = false)
	private String value;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected UserValue() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param user Der User, dem der UserValue zugeordnet werden soll
	 * @param name Der Name des UserValues
	 * @param value Der Wert
	 */
	public UserValue(BasicUser user, String name, String value) {
		this.user = user;
		this.name = name;
		this.value = value;
	}

	/**
	 * Gibt den Namen des UserValues zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des UserValues.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt den User zurueck, welchem der UserValue zugeordnet ist.
	 * @return Der User
	 */
	public BasicUser getUser() {
		return user;
	}

	/**
	 * Setzt den User, dem der UserValue zugeordnet ist.
	 * @param user Der User
	 */
	public void setUser(BasicUser user) {
		this.user = user;
	}

	/**
	 * Gibt den Wert zurueck.
	 * @return Der Wert
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Setzt den Wert.
	 * @param value Der neue Wert
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gibt die ID des UserValues zurueck.
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
