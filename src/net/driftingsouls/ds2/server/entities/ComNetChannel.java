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

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import org.hibernate.Session;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Ein ComNet-Kanal.</p>
 * Die Berechtigungen fuer Lese- und Schreibrechte sind unabhaengig voneinander. Dies bedeutet,
 * dass die Zugriffsberechtigten Spieler nicht die Liste der Spieler in einer zugriffsberechtigten
 * Allianz enthaelt.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="skn_channels")
public class ComNetChannel {
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "comnet_channel_fk_ally")
	private Ally allyOwner;
	@Column(name="writeall", nullable = false)
	private boolean writeAll;
	@Column(name="readall", nullable = false)
	private boolean readAll;
	@Column(name="readnpc", nullable = false)
	private boolean readNpc;
	@Column(name="writenpc", nullable = false)
	private boolean writeNpc;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "comnet_channel_fk_ally2")
	private Ally writeAlly;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "comnet_channel_fk_ally3")
	private Ally readAlly;
	@Lob
	@Column(name="readplayer", nullable = false)
	private String readPlayer;
	@Lob
	@Column(name="writeplayer", nullable = false)
	private String writePlayer;

	@Version
	private int version;

	/**
	 * Konstruktor.
	 *
	 */
	protected ComNetChannel() {
		// EMPTY
	}

	/**
	 * Erstellt einen neuen ComNet-Kanal.
	 * @param name Der Name des Kanals
	 */
	public ComNetChannel(String name) {
		this.name = name;
		this.readPlayer = "";
		this.writePlayer = "";
	}

	/**
	 * Gibt die Ally zurueck, die den Channel besitzt.
	 * @return Die Allianz
	 */
	public Ally getAllyOwner() {
		return allyOwner;
	}

	/**
	 * Setzt den Allianzbesitzer des Channels.
	 * @param allyOwner Die Allianz
	 */
	public void setAllyOwner(Ally allyOwner) {
		this.allyOwner = allyOwner;
	}

	/**
	 * Gibt den Namen des Channels zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Channels.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt zurueck, ob der Kanal fuer alle lesbar ist.
	 * @return <code>true</code>, falls er fuer alle lesbar ist
	 */
	public boolean isReadAll() {
		return readAll;
	}

	/**
	 * Setzt, ob der Kanal fuer alle lesbar ist.
	 * @param readAll <code>true</code>, falls er fuer alle lesbar ist
	 */
	public void setReadAll(boolean readAll) {
		this.readAll = readAll;
	}

	/**
	 * Gibt die Allianz zurueck, die den Kanal lesen kann.
	 * @return Die Allianz
	 */
	public Ally getReadAlly() {
		return readAlly;
	}

	/**
	 * Setzt die Allianz, die den Kanal lesen kann.
	 * @param readAlly Die Allianz
	 */
	public void setReadAlly(Ally readAlly) {
		this.readAlly = readAlly;
	}

	/**
	 * Gibt zurueck, ob NPCs den Kanal lesen koennen.
	 * @return <code>true</code>, falls NPCs den Kanal lesen koennen
	 */
	public boolean isReadNpc() {
		return readNpc;
	}

	/**
	 * Setzt, ob NPCs den Kanal lesen koennen.
	 * @param readNpc <code>true</code>, falls NPCs den Kanal lesen koennen
	 */
	public void setReadNpc(boolean readNpc) {
		this.readNpc = readNpc;
	}

	/**
	 * Gibt die Spieler zurueck, die den Kanal lesen koennen.
	 * @return die User
	 */
	public Set<User> getReadPlayer() {
		Session db = ContextMap.getContext().getDB();
		Set<User> readplayers =  Arrays.stream(Common.explodeToInt(",", readPlayer)).mapToObj(id -> (User) db.get(User.class, id)).collect(Collectors.toSet());
        // Falls nicht mehr vorhandene oder falsch eingetragene Spielerids hier auftreten führt es an anderer Stelle zu einer NPE
        // Also sorgen wir hier dafür, dass diese einfach ignoriert werden
        readplayers.remove(null);
        return readplayers;
	}

	/**
	 * Setzt die Spieler, die den Kanal lesen koennen.
	 * @param readPlayer Die IDs
	 */
	public void setReadPlayer(int[] readPlayer) {
		this.readPlayer = Common.implode(",",readPlayer);
	}

	/**
	 * Setzt die Spieler, die den Kanal lesen koennen.
	 * @param readPlayer Die User
	 */
	public void setReadPlayer(Set<User> readPlayer) {
		this.readPlayer = readPlayer.stream().map(User::getId).map(Object::toString).collect(Collectors.joining(","));
	}

	/**
	 * Gibt zurueck, ob alle Spieler im Kanal posten koennen.
	 * @return <code>true</code>, falls alle Posten koennen
	 */
	public boolean isWriteAll() {
		return writeAll;
	}

	/**
	 * Setzt, ob alle Spieler im Kanal posten koennen.
	 * @param writeAll <code>true</code>, falls alle Spieler posten koennen
	 */
	public void setWriteAll(boolean writeAll) {
		this.writeAll = writeAll;
	}

	/**
	 * Gibt die Allianz zurueck, deren Mitglieder im Kanal posten koennen.
	 * @return Die Allianz
	 */
	public Ally getWriteAlly() {
		return writeAlly;
	}

	/**
	 * Setzt die Allianz, deren Mitglieder im Kanal posten koennen.
	 * @param writeAlly Die Allianz
	 */
	public void setWriteAlly(Ally writeAlly) {
		this.writeAlly = writeAlly;
	}

	/**
	 * Gibt zurueck, ob NPCs im Kanal posten koennen.
	 * @return <code>true</code>, falls NPCs posten koennen
	 */
	public boolean isWriteNpc() {
		return writeNpc;
	}

	/**
	 * Setzt, ob NPCs im Kanal posten koennen.
	 * @param writeNpc <code>true</code>, falls NPCs posten koennen
	 */
	public void setWriteNpc(boolean writeNpc) {
		this.writeNpc = writeNpc;
	}

	/**
	 * Gibt die Spieler zurueck, die im Kanal posten koennen.
	 * @return Die User
	 */
	public Set<User> getWritePlayer() {
		Session db = ContextMap.getContext().getDB();
		Set<User> writeplayers = Arrays.stream(Common.explodeToInt(",", writePlayer)).mapToObj(id -> (User) db.get(User.class, id)).collect(Collectors.toSet());
        // Falls nicht mehr vorhandene oder falsch eingetragene Spielerids hier auftreten führt es an anderer Stelle zu einer NPE
        // Also sorgen wir hier dafür, dass diese einfach ignoriert werden
        writeplayers.remove(null);
        return writeplayers;
	}

	/**
	 * Setzt die Spieler, die im Kanal posten koennen.
	 * @param writePlayer Die IDs
	 */
	public void setWritePlayer(int[] writePlayer) {
		this.writePlayer = Common.implode(",", writePlayer);
	}

	/**
	 * Setzt die Spieler, die im Kanal posten koennen.
	 * @param writePlayer Die Spieler
	 */
	public void setWritePlayer(Set<User> writePlayer) {
		this.writePlayer = writePlayer.stream().map(User::getId).map(Object::toString).collect(Collectors.joining(","));
	}

	/**
	 * Gibt die ID des Kanals zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Prueft, ob der ComNet-Kanal fuer den angegebenen Benutzer lesbar ist.
	 * @param user Der Benutzer
	 * @param presolver Der zu verwendende PermissionResolver
	 * @return <code>true</code>, falls er lesbar ist
	 */
	public boolean isReadable( User user, PermissionResolver presolver ) {
		if( this.readAll || ((user.getId() < 0) && this.readNpc) ||
			((user.getAlly() != null) && (this.readAlly == user.getAlly())) ||
			presolver.hasPermission(WellKnownPermission.COMNET_ALLES_LESBAR) ) {

			return true;
		}

		if( this.writeAll || ((user.getId() < 0) && this.writeNpc) ||
			((user.getAlly() != null) && (this.writeAlly == user.getAlly())) ||
			presolver.hasPermission(WellKnownPermission.COMNET_ALLES_LESBAR) ) {

			return true;
		}

		if(!this.readPlayer.isEmpty()) {
			Integer[] playerlist = Common.explodeToInteger(",",this.readPlayer);
			if( Common.inArray(user.getId(), playerlist) ) {
				return true;
			}
		}

		if(!this.writePlayer.isEmpty()) {
			Integer[] playerlist = Common.explodeToInteger(",",this.writePlayer);
			return Common.inArray(user.getId(), playerlist);
		}

		return false;
	}

	/**
	 * Prueft, ob der ComNet-Kanal fuer den angegebenen Benutzer schreibbar ist.
	 * @param user Der Benutzer
	 * @param presolver Der zu verwendende PermissionResolver
	 * @return <code>true</code>, falls er schreibbar ist
	 */
	public boolean isWriteable( User user, PermissionResolver presolver ) {
		if( this.writeAll || ((user.getId() < 0) && this.writeNpc) ||
				((user.getAlly() != null) && (this.writeAlly == user.getAlly())) ||
				presolver.hasPermission(WellKnownPermission.COMNET_ALLES_SCHREIBBAR) ) {

				return true;
			}

		if(!this.writePlayer.isEmpty()) {
			Integer[] playerlist = Common.explodeToInteger(",",this.writePlayer);
			return Common.inArray(user.getId(), playerlist);
		}

		return false;
	}

	/**
	 * Gibt die Anzahl an Posts im ComNet-Kanal zurueck.
	 * @return Die Anzahl der Posts
	 */
	public int getPostCount() {
		org.hibernate.Session db = ContextMap.getContext().getDB();

		return ((Number)db.createQuery("select count(*) from ComNetEntry where channel= :channel")
			.setEntity("channel", this)
			.iterate().next()).intValue();
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
