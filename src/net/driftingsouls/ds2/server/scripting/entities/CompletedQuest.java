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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

/**
 * Repraesentiert ein durch einen Benutzer abgeschlossenes Quest.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests_completed")
@org.hibernate.annotations.Table(
		appliesTo = "quests_completed",
		indexes = {@Index(name="questid", columnNames = {"questid", "userid"})}
)
public class CompletedQuest {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="questid", nullable=false)
	@ForeignKey(name="quests_completed_fk_quests")
	private Quest quest;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="userid", nullable=false)
	@ForeignKey(name="quests_completed_fk_users")
	private User user;
	
	/**
	 * Konstruktor.
	 *
	 */
	public CompletedQuest() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Eintrag fuer ein abgeschlossenes Quest.
	 * @param quest Das Quest
	 * @param user Der Benutzer, der das Quest abgeschlossen hat
	 */
	public CompletedQuest(Quest quest, User user) {
		this.quest = quest;
		this.user = user;
	}

	/**
	 * Gibt das Quest zurueck.
	 * @return Das Quest
	 */
	public Quest getQuest() {
		return quest;
	}

	/**
	 * Setzt das Quest.
	 * @param quest Das Quest
	 */
	public void setQuest(Quest quest) {
		this.quest = quest;
	}

	/**
	 * Gibt den Benutzer zurueck.
	 * @return Der Benutzer
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Benutzer.
	 * @param user Der Benutzer
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
}
