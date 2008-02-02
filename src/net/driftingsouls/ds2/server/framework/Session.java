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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Eine Session in DS
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="sessions")
public class Session {
	@Id
	private String session;
	
	@ManyToOne(fetch=FetchType.EAGER)
	@JoinColumn(name="id", nullable=false)
	private BasicUser user;
	private String ip;
	private long lastaction;
	private int actioncounter;
	private int usegfxpak;
	private int tick;
	private String attach;
	
	/**
	 * Konstruktor
	 *
	 */
	public Session() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Session fuer einen Benutzer
	 * @param user Der Benutzer
	 */
	public Session(BasicUser user) {
		this.session = Common.md5(Integer.toString(RandomUtils.nextInt(Integer.MAX_VALUE)));
		this.user = user;
		this.lastaction = Common.time();
	}

	/**
	 * Gibt den ActionCounter der Session zurueck
	 * @return Der ActionCounter
	 */
	public int getActionCounter() {
		return actioncounter;
	}

	/**
	 * Setzt den ActionCounter der Session
	 * @param actioncounter Der neue Wert
	 */
	public void setActionCounter(int actioncounter) {
		this.actioncounter = actioncounter;
	}

	/**
	 * Gibt die angefuegte Session-ID oder <code>null</code> zurueck
	 * @return Die Session-ID oder <code>null</code>
	 */
	public String getAttach() {
		return attach;
	}

	/**
	 * Setzt die angefuegte Session-ID
	 * @param attach Die neue Session-ID
	 */
	public void setAttach(String attach) {
		this.attach = attach;
	}

	/**
	 * Gibt die Liste der gueltigen IPs zurueck
	 * @return Die Liste der gueltigen IPs
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * Setzt die Liste der gueltigen IPs
	 * @param ip Die neue Liste
	 */
	public void setIP(String ip) {
		this.ip = ip;
	}
	
	/**
	 * Prueft, ob die angegebene IP-Adresse in der Liste der IP-Adressen vorkommt
	 * @param ip Die IP-Adresse
	 * @return <code>true</code>, falls die IP-Adresse in der Liste vorkommt
	 */
	public boolean isValidIP(String ip) {
		if( !getIP().contains("<"+ip+">") ) {
			return false;
		}
		return true;
	}

	/**
	 * Gibt den Zeitpunkt der letzten Aktion zurueck
	 * @return Der Zeitpunkt der letzten Aktion
	 */
	public long getLastAction() {
		return lastaction;
	}

	/**
	 * Setzt den Zeitpunkt der letzten Aktion
	 * @param lastaction Der Zeitpunkt
	 */
	public void setLastAction(long lastaction) {
		this.lastaction = lastaction;
	}

	/**
	 * Gibt die Session-ID zurueck
	 * @return Die Session-ID
	 */
	public String getSession() {
		return session;
	}

	/**
	 * Gibt zurueck, ob fuer den Spieler gerade Daten berechnet werden
	 * @return <code>true</code>, falls gerade fuer den Spieler Daten berechnet werden
	 */
	public boolean getTick() {
		return tick != 0;
	}

	/**
	 * Setzt, ob fuer den Spieler gerade Daten berechnet werden
	 * @param tick <code>true</code>, falls gerade Daten berechnet werden
 	 */
	public void setTick(boolean tick) {
		this.tick = tick ? 1 : 0;
	}

	/**
	 * Gibt <code>true</code>, falls das Grafikpak verwendet werden soll
	 * @return <code>true</code>, falls das Grafikpak verwendet werden soll
	 */
	public boolean getUseGfxPak() {
		return usegfxpak != 0;
	}

	/**
	 * Setzt, ob das Grafikpak verwendet werden soll
	 * @param usegfxpak <code>true</code>, falls das Grafikpak verwendet werden soll
	 */
	public void setUseGfxPak(boolean usegfxpak) {
		this.usegfxpak = usegfxpak ? 1 : 0;
	}

	/**
	 * Gibt den mit der Session verknuepften User zurueck
	 * @return Der mit der Session verknuepfte User
	 */
	public BasicUser getUser() {
		return user;
	}

	/**
	 * Setzt den mit der Session verknuepften User
	 * @param user Der mit der Session verknuepfte User
	 */
	public void setUser(BasicUser user) {
		this.user = user;
	}
}
