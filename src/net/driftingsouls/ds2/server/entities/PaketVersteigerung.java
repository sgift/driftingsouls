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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Type;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipType;

/**
 * <h1>Ein zu Versteigerndes Paket.</h1>
 * <p>Ein Paket besteht aus einer Anzahl an Schiffen sowie einem Cargo
 * mit Resourcen</p>
 * 
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="versteigerungen_pakete")
public class PaketVersteigerung {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="bieter")
	private User bieter;
	private long preis;
	private int tick;
	@Type(type="cargo")
	private Cargo cargo;
	private String ships = "";
	
	@Version
	private int version;
	
	/**
	 * Konstruktor.
	 *
	 */
	public PaketVersteigerung() {
		// EMPTY
	}
	
	/**
	 * Erstellt eine neue Versteigerung.
	 * @param owner Der default-Bieter
	 * @param price Der Startpreis
	 */
	public PaketVersteigerung(User owner, long price) {
		this.bieter = owner;
		this.preis = price;
		this.cargo = new Cargo();
	}

	/**
	 * Gibt den Bieter zurueck.
	 * @return Der Bieter
	 */
	public User getBieter() {
		return bieter;
	}

	/**
	 * Setzt den Bieter.
	 * @param bieter Der Bieter
	 */
	public void setBieter(User bieter) {
		this.bieter = bieter;
	}

	/**
	 * Gibt den aktuellen Preis zurueck.
	 * @return Der Preis
	 */
	public long getPreis() {
		return preis;
	}

	/**
	 * Setzt den aktuellen Preis.
	 * @param preis Der Preis
	 */
	public void setPreis(long preis) {
		this.preis = preis;
	}

	/**
	 * Gibt den Tick zurueck, an dem die Versteigerung endet.
	 * @return Der Tick
	 */
	public int getTick() {
		return tick;
	}

	/**
	 * Setzt den Tick, an dem die Versteigerung endet.
	 * @param tick Der Tick
	 */
	public void setTick(int tick) {
		this.tick = tick;
	}

	/**
	 * Gibt die ID der Versteigerung zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gibt den Cargo zurueck.
	 * @return Der Cargo
	 */
	public Cargo getCargo() {
		return cargo;
	}
	
	/**
	 * Setzt den Cargo.
	 * @param cargo Der Cargo
	 */
	public void setCargo(Cargo cargo) {
		this.cargo = cargo;
	}
	
	/**
	 * Gibt die Schiffstypen aller zur Versteigerung stehender Schiffe zurueck.
	 * Die Angabe ist pro Schiff - ein Schiffstyp kann also Mehrfach in der Liste
	 * vorkommen.
	 * @return Die Schiffstypen
	 */
	public ShipType[] getShipTypes() {
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		String[] shipids = StringUtils.split(this.ships, "|");
		ShipType[] shiptypes = new ShipType[shipids.length];
		
		for( int i=0; i < shipids.length; i++ ) {
			shiptypes[i] = (ShipType)db.get(ShipType.class, Integer.parseInt(shipids[i]));
		}
		
		return shiptypes;
	}
	
	/**
	 * Fuegt einen Schiffstyp zur Liste der zu versteigernden Schiffe hinzu.
	 * @param type Der Typ, der hinzugefuegt werden soll
	 */
	public void addShipType(ShipType type) {
		if( ships.length() != 0 ) {
			ships += "|"+type.getTypeId();
		}
		else {
			ships = Integer.toString(type.getTypeId());
		}
	}

	/**
	 * Gibt die Versionsnummer zurueck.
	 * @return Die Nummer
	 */
	public int getVersion() {
		return this.version;
	}
}
