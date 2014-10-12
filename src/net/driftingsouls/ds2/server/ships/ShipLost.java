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
package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.SchlachtLog;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.ConfigService;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Ein in einer Schlacht zerstoertes Schiff.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="ships_lost")
public class ShipLost {
	// Hinweis: Foreign Keys/Assoziationen sind hier nur begrenzt sinnvoll,
	// da z.B. ein Eintrag in dieser Tabelle laenger existiert als ggf eine Allianz
	
	@Id @GeneratedValue
	private int id;
	private int type;
	private String name;
	private int tick;
	@Index(name="shiplost_owner")
	private int owner;
	@Index(name="shiplost_ally")
	private int ally;
	@Column(name="destowner", nullable = false)
	@Index(name="shiplost_destowner")
	private int destOwner;
	@Column(name="destally", nullable = false)
	@Index(name="shiplost_destally")
	private int destAlly;
	@Index(name="shiplost_battle")
	private int battle;
	@Column(name="battlelog")
	@Index(name="shiplost_battlelog")
	private String battleLog;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="shiplost_fk_schlachtlog")
	private SchlachtLog schlachtLog;
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public ShipLost() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen Eintrag fuer das angegebene Schiff. Alle Felder mit Ausnahme von 
	 * <code>battleLog</code>, <code>destOwner</code> und <code>destAlly</code> werden gesetzt
	 * @param ship Das Schiff, fuer das der Eintrag angelegt werden soll
	 */
	public ShipLost(Ship ship) {
		this.type = ship.getType();
		this.name = ship.getName();
		this.tick = new ConfigService().getValue(WellKnownConfigValue.TICKS);
		this.owner = ship.getOwner().getId();
		if( ship.getOwner().getAlly() != null ) {
			this.ally = ship.getOwner().getAlly().getId();
		}
		
		if( ship.getBattle() != null ) {
			setBattle(ship.getBattle().getId());
			this.schlachtLog = ship.getBattle().getSchlachtLog();
		}
	}

	/**
	 * Gibt das Log zur Schlacht zurueck, an der das Schiff beteiligt war.
	 * Falls das Log nicht vorliegt wird <code>null</code> zurueckgegeben.
	 * @return Das Log oder <code>null</code>
	 */
	public SchlachtLog getSchlachtLog()
	{
		return schlachtLog;
	}

	/**
	 * Gibt die Allianz zurueck, der das Schiff gehoert hat.
	 * @return Die Allianz
	 */
	public int getAlly() {
		return ally;
	}

	/**
	 * Setzt die Allianz, der das Schiff gehoert hat.
	 * @param ally Die Allianz
	 */
	public void setAlly(Ally ally) {
		this.ally = ally != null ? ally.getId() : 0;
	}

	/**
	 * Gibt die Schlacht zurueck, in der das Schiff zerstoert wurde.
	 * @return Die Schlacht
	 */
	public int getBattle() {
		return battle;
	}

	/**
	 * Setzt die Schlacht, in der das Schiff zerstoert wurde.
	 * @param battle Die Schlacht
	 */
	public void setBattle(int battle) {
		this.battle = battle;
	}

	/**
	 * Gibt den Dateinamen des Schlachtlogs zurueck.
	 * @return Das Schlachtlog
	 */
	public String getBattleLog() {
		return battleLog;
	}

	/**
	 * Gibt die Allianz zurueck, die das Schiff zerstoert hat.
	 * @return Die Allianz
	 */
	public int getDestAlly() {
		return destAlly;
	}

	/**
	 * Setzt die Allianz, die das Schiff zerstoert hat.
	 * @param destAlly Die Allianz
	 */
	public void setDestAlly(Ally destAlly) {
		this.destAlly = destAlly != null ? destAlly.getId() : 0;
	}

	/**
	 * Gibt den Spieler zurueck, der das Schiff zerstoert hat.
	 * @return Der Spieler
	 */
	public int getDestOwner() {
		return destOwner;
	}

	/**
	 * Setzt den Spieler, der das Schiff zerstoert hat.
	 * @param destOwner Der Spieler
	 */
	public void setDestOwner(User destOwner) {
		this.destOwner = destOwner.getId();
	}

	/**
	 * Gibt den Namen des Schiffes zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen des Schiffes.
	 * @param name Der Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt den Besitzer des Schiffes zurueck.
	 * @return Der Besitzer
	 */
	public int getOwner() {
		return owner;
	}

	/**
	 * Setzt den Besiter des Schiffes.
	 * @param owner der Besitzer
	 */
	public void setOwner(User owner) {
		this.owner = owner.getId();
	}

	/**
	 * Gibt den Zeitpunkt (Tick) der Zerstoerung zurueck.
	 * @return Der Zeitpunkt
	 */
	public long getTime() {
		return tick;
	}

	/**
	 * Setzt den Zeitpunkt (Tick) der Zerstoerung.
	 * @param time Der Zeitpunkt
	 */
	public void setTime(int time) {
		this.tick = time;
	}

	/**
	 * Gibt den Schiffstyp zurueck.
	 * @return Der Schiffstyp
	 */
	public int getType() {
		return type;
	}

	/**
	 * Setzt den Schiffstyp.
	 * @param type Der Typ
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Gibt die ID des Eintrags zurueck. Diese ist nicht identisch mit der
	 * ID des zerstoerten Schiffes!
	 * @return Die ID
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
