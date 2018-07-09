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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.hibernate.annotations.ForeignKey;

/**
 * <p>Ein Post im ComNet.</p>
 * <p>Ein Post existiert auch nach entfernen des Posters aus dem System
 * weiterhin und wird dann an den Spieler 0 gebunden.</p>
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="skn")
public class ComNetEntry {
	@Id @GeneratedValue
	private int post;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="userid", nullable=false)
	@ForeignKey(name="skn_fk_users")
	private User user;
	private long time;
	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private String head;
	@Lob
	@Column(nullable = false)
	private String text;
	private int pic;
	@Column(name="allypic", nullable = false)
	private int allyPic;
	private int tick;

	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="channel", nullable=false)
	@ForeignKey(name="skn_fk_skn_channels")
	private ComNetChannel channel;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	protected ComNetEntry() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Post fuer den aktuellen Zeitpunkt.
	 * @param user Der User, der den Post erstellt hat
	 * @param channel Der Channel, in dem gepostet werden soll
	 */
	public ComNetEntry(User user, ComNetChannel channel) {
		this.head = "";
		this.text = "";
		this.user = user;
		this.channel = channel;
		this.pic = user.getId();
		this.allyPic = user.getAlly() != null ? user.getAlly().getId() : 0;
		this.tick = ContextMap.getContext().get(ContextCommon.class).getTick();
		this.time = Common.time();
		this.name = user.getName();
	}

	/**
	 * Gibt die Nummer des Ally-Pics zurueck.
	 * @return Die Nummer
	 */
	public int getAllyPic() {
		return allyPic;
	}
	
	/**
	 * Setzt die Nummer des Ally-Pics.
	 * @param allyPic Die Nummer
	 */
	public final void setAllyPic(int allyPic) {
		this.allyPic = allyPic;
	}

	/**
	 * Gibt den ComNet-Kanal zurueck, in dem sich der Post befindet.
	 * @return Der ComNet-Kanal
	 */
	public ComNetChannel getChannel() {
		return channel;
	}

	/**
	 * Setzt den ComNet-Kanal, in dem sich der Post befindet.
	 * @param channel Der ComNet-Kanal
	 */
	public final void setChannel(ComNetChannel channel) {
		this.channel = channel;
	}

	/**
	 * Gibt den Titel des Posts zurueck.
	 * @return Der Titel
	 */
	public String getHead() {
		return head;
	}

	/**
	 * Setzt den Titel des Posts.
	 * @param head Der Titel
	 */
	public void setHead(String head) {
		this.head = head;
	}

	/**
	 * Gibt den Namen des Spielers zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Spielers.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt die Nummer des Spieler-Pics zurueck.
	 * @return Die Nummer
	 */
	public int getPic() {
		return pic;
	}

	/**
	 * Setzt die Nummer des Spieler-Pics.
	 * @param pic Die Nummer
	 */
	public final void setPic(int pic) {
		this.pic = pic;
	}

	/**
	 * Gibt den Text des Posts zurueck.
	 * @return Der Text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Setzt den Text des Posts.
	 * @param text Der Text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gibt den Tick zurueck, an dem der Post erstellt wurde.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, an dem der Post erstellt wurde.
	 * @param tick Der Tick
	 */
	public final void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt den Zeitpunkt des Posts zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Setzt den Zeitpunkt des Posts.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * Gibt den User zurueck, der den Post erstellt hat.
	 * @return Der User
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Setzt den User, der den Post erstellt hat.
	 * @param user Der User
	 */
	public final void setUser(final User user) {
		this.user = user;
	}

	/**
	 * Gibt die ID es Posts zurueck.
	 * @return Die ID
	 */
	public int getPost() {
		return post;
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
