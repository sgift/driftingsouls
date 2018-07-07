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

import net.driftingsouls.ds2.server.framework.Common;
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
 * Ein Besuchseintrag eines Users fuer einen ComNet-Kanal.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="skn_visits")
@org.hibernate.annotations.Table(
		appliesTo = "skn_visits",
		indexes = {@Index(name="skn_visits_user", columnNames = {"user_id", "channel_id"})}
)
public class ComNetVisit {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(nullable=false)
	@ForeignKey(name="skn_visits_fk_users")
	private User user;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(nullable=false)
	@ForeignKey(name="skn_visits_fk_skn_channels")
	private ComNetChannel channel;
	private long time;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ComNetVisit() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Besuchseintrag fuer den aktuellen Zeitpunkt.
	 * @param user Der Besitzer
	 * @param channel Der Kanal
	 */
	public ComNetVisit(User user, ComNetChannel channel) {
		this.user = user;
		this.channel = channel;
		this.time = Common.time();
	}

	/**
	 * Gibt den ComNet-Kanal zurueck.
	 * @return Der ComNet-Kanal
	 */
	public ComNetChannel getChannel() {
		return channel;
	}

	/**
	 * Setzt den ComNet-Kanal.
	 * @param channel Der Kanal
	 */
	public void setChannel(ComNetChannel channel) {
		this.channel = channel;
	}

	/**
	 * Gibt den Zeitpunkt des Besuchs zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt des Besuchs.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den Besitzer des Besuchseintrags zurueck.
	 * @return Der Besitzer
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den Besitzer des Besuchseintrags.
	 * @param user Der Besitzer
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Der Eintrag
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
