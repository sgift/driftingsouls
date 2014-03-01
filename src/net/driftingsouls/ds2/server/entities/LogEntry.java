/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import net.driftingsouls.ds2.server.framework.Common;

/**
 * Ein Logeintrag.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="logging")
public class LogEntry {
	@Id @GeneratedValue
	private long id;
	@Column(nullable = false)
	private String type;
	@Column(name="user_id", nullable = false)
	private int user; 
	private long time;
	@Column(nullable = false)
	private String source;
	@Column(nullable = false)
	private String target;
	@Lob
	private String data;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public LogEntry() {
		// EMPTY
	}
	
	/**
	 * <p>Konstruktor.</p>
	 * Erstellt einen neuen Logeintrag fuer den aktuellen Zeitpunkt.
	 * @param type Der Typ des Eintrgas
	 * @param user Der User, der den Eintrag ausgeloest hat
	 * @param source Die Quelle der Handlung
	 * @param target Das Ziel der Handlung
	 */
	public LogEntry(String type, int user, String source, String target) {
		this.type = type;
		this.user = user;
		this.source = source;
		this.target = target;
		this.time = Common.time();
	}
	
	/**
	 * Gibt weitere Daten zurueck.
	 * @return weitere Daten
	 */
	public String getData() {
		return data;
	}
	
	/**
	 * Setzt weitere Daten.
	 * @param data weitere Daten
	 */
	public void setData(String data) {
		this.data = data;
	}
	
	/**
	 * Gibt die Quelle der Aktion zurueck.
	 * @return Die Quelle
	 */
	public String getSource() {
		return source;
	}
	
	/**
	 * Setzt die Quelle der Aktion.
	 * @param source Die Quelle
	 */
	public void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * Gibt das Ziel der Aktion zurueck.
	 * @return Das Ziel
	 */
	public String getTarget() {
		return target;
	}
	
	/**
	 * Setzt das Ziel der Aktion.
	 * @param target Das Ziel
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/**
	 * Gibt den Zeitpunkt der Aktion zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Setzt den Zeitpunkt der Aktion.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(long time) {
		this.time = time;
	}
	
	/**
	 * Gibt den Typ des Logeintrags zurueck.
	 * @return Der Logeintrag
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Setzt den Typ des Logeintrags.
	 * @param type Der Typ
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Gibt den ausloesenden User zurueck.
	 * @return Der User
	 */
	public int getUser() {
		return user;
	}
	
	/**
	 * Setzt den ausloesenden user.
	 * @param user Der User
	 */
	public void setUser(int user) {
		this.user = user;
	}
	
	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public long getId() {
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
