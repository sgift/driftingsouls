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
package net.driftingsouls.ds2.server.scripting.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Repraesentiert ein in der DB gespeichertes Script.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="scripts")
public class Script {
	@Id @GeneratedValue
	private int id;
	private String name;
	private String script;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Script() {
		// EMPTY
	}
	
	/**
	 * Erstellt ein neues Script.
	 * @param name Der Name des Scripts
	 * @param script Das Script selbst
	 */
	public Script(String name, String script) {
		this.name = name;
		this.script = script;
	}

	/**
	 * Gibt den Namen des Scripts zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Scripts.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt das Script zurueck.
	 * @return Das Script
	 */
	public String getScript() {
		return script;
	}

	/**
	 * Setzt das Script.
	 * @param script Das Script
	 */
	public void setScript(String script) {
		this.script = script;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Setzt die ID.
	 * @param id Die ID
	 */
	public void setId(int id)
	{
		this.id = id;
	}
}
