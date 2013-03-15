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
 * Repraesentiert ein Quest.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests")
public class Quest {
	@Id @GeneratedValue
	private int id;
	private String name;
	private String qid;

	/**
	 * Konstruktor.
	 *
	 */
	public Quest() {
		// EMPTY
	}

	/**
	 * Erstellt ein neues Quest.
	 * @param name Der Name des Quests
	 */
	public Quest(String name) {
		this.name = name;
		this.qid = "";
	}

	/**
	 * Gibt den Namen des Quests zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Quests.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt die ID des QuickQuests zurueck, sofern das Quest mit einem
	 * QuickQuest verbunden ist. Andernfalls wird ein leerer String
	 * zurueckgegeben .
	 * @return Die QuickQuest-ID
	 */
	public String getQid() {
		return qid;
	}

	/**
	 * Setzt die ID des QuickQuests, mit dem das Quest verbunden ist.
	 * @param qid Die QuickQuest-ID oder ein leerer String
	 */
	public void setQid(String qid) {
		this.qid = qid;
	}

	/**
	 * Gibt die ID des Quests zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	@Override
	public String toString()
	{
		return "Quest [id: "+this.id+" name: "+this.name+"]";
	}

	/**
	 * Setzt die ID des Quests.
	 * @param id Die ID
	 */
	public void setId(Integer id)
	{
		this.id = id;
	}
}
