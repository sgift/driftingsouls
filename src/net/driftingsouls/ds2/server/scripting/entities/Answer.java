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
 * Repraesentiert eine Dialogantwort.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests_answers")
public class Answer {
	@Id @GeneratedValue
	private int id;
	private String text;
	
	/**
	 * Konstruktor.
	 *
	 */
	public Answer() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Antwort.
	 * @param text Der Antworttext
	 */
	public Answer(String text) {
		this.text = text;
	}

	/**
	 * Gibt den Antworttext zurueck.
	 * @return Der Antworttext
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt den Antworttext.
	 * @param text Der Antworttext
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
}
