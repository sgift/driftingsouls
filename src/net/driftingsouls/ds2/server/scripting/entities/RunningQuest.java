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

import net.driftingsouls.ds2.server.entities.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <h1>Repraesentiert ein laufendes Quest.</h1>
 * <p>Ein laufendes Quest in in gewisser Weise eine Instanz eines
 * Quests. Jedes laufende Quest ist an einen Spieler gebunden. Es beinhaltet
 * alle notwendigen Daten, damit der Spieler das Quest durchfuehren kann wie z.B.
 * Statusinformationen. Zudem enthaelt es die Informationen um das laufende Quest
 * nach Abschluss wieder aus dem System zu entfernen.</p>
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="quests_running")
public class RunningQuest {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="questid", nullable=false)
	private Quest quest;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="userid", nullable=false)
	private User user;
	@Column(name="execdata")
	@Lob
	private byte[] execData;
	private String uninstall;
	@Column(name="statustext")
	private String statusText;
	private int publish;
	@Column(name="ontick")
	private Integer onTick;

	/**
	 * Konstruktor.
	 *
	 */
	public RunningQuest() {
		// EMPTY
	}

	/**
	 * Erstellt ein neues laufendes Quest.
	 * @param quest Das Quest
	 * @param user Der Spieler, der das Quest ausfuehrt
	 */
	public RunningQuest(Quest quest, User user) {
		this.quest = quest;
		this.user = user;
		this.uninstall = null;
		this.statusText = "";
		this.publish = 0;
		this.onTick = null;
		this.execData = new byte[0];
	}

	/**
	 * Gibt die Ausfuehrungsdaten des Quests zurueck.
	 * Die Ausfuehrungsdaten enthalten den Zustand des
	 * Scriptparsers.
	 * @return the execData Die Ausfuehrungsdaten
	 */
	public byte[] getExecData() {
		return execData;
	}

	/**
	 * Setzt die Ausfuehrungsdaten des Quests.
	 * Die Ausfuehrungsdaten enthalten den Zustand des
	 * Scriptparsers.
	 * @param execData Die Ausfuehrungsdaten
	 */
	public void setExecData(byte[] execData) {
		this.execData = execData;
	}

	/**
	 * Gibt die ID des Scripts zurueck, welches
	 * bei jedem Tick aufgerufen werden soll.
	 * @return Die ID oder <code>null</code>.
	 */
	public Integer getOnTick() {
		return onTick;
	}

	/**
	 * Setzt die ID des Scripts, welches
	 * bei jedem Tick aufgerufen werden soll.
	 * @param onTick Die ID oder <code>null</code>
	 */
	public void setOnTick(Integer onTick) {
		this.onTick = onTick;
	}

	/**
	 * Gibt zurueck, ob das Quest vor dem Spieler
	 * versteckt ablaufen oder in der Questliste aufgefuehrt
	 * werden soll.
	 * @return <code>true</code>, falls es in der Liste aufgefuehrt werden soll
	 */
	public boolean getPublish() {
		return publish != 0;
	}

	/**
	 * Setzt, ob das Quest vor dem Spieler
	 * versteckt ablaufen oder in der Questliste aufgefuehrt
	 * werden soll.
	 * @param publish <code>true</code>, falls es in der Liste aufgefuehrt werden soll
	 */
	public void setPublish(int publish) {
		this.publish = publish;
	}

	/**
	 * Gibt das Quest zurueck, zu dem das laufende Quest gehoert.
	 * @return Das Quest
	 */
	public Quest getQuest() {
		return quest;
	}

	/**
	 * Setzt das Quest, zu dem das laufende Quest gehoert.
	 * @param quest Das Quest
	 */
	public void setQuest(Quest quest) {
		this.quest = quest;
	}

	/**
	 * Gibt den Statustext zurueck, der dem User zum Quest
	 * angezeigt werden soll.
	 * @return Der Statustext
	 */
	public String getStatusText() {
		return statusText;
	}

	/**
	 * Setzt den Statustext, der dem User zum Quest angezeigt werden soll.
	 * @param statusText Der Statustext
	 */
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	/**
	 * Gibt das Uninstall-Script zurueck, was nach Beendigung
	 * des Quests ablaufen soll. Falls noch keines vorhanden ist,
	 * wird <code>null</code> zurueckgegeben.
	 * @return Das Uninstallscript oder <code>null</code>.
	 */
	public String getUninstall() {
		return uninstall;
	}

	/**
	 * Setzt das Uninstall-Script, was nach Beendigung des Quests
	 * ablaufen soll. <code>null</code> bedeutet, dass noch keines
	 * vorhanden ist.
	 * @param uninstall Das Uninstall-Script oder <code>null</code>
	 */
	public void setUninstall(String uninstall) {
		this.uninstall = uninstall;
	}

	/**
	 * Gibt den Spieler zurueck, der das Quest durchfuehrt.
	 * @return Der Spieler
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Spieler, der das Quest durchfuehrt.
	 * @param user Der Spieler
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Gibt die ID des laufenden Quests zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
}
