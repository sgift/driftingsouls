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
package net.driftingsouls.ds2.server.entities.npcorders;

import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Eine NPC-Schiffsbestellung.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="orders_ships")
public class OrderableShip {
	@Id @GeneratedValue
	private int id;
	@ManyToOne(fetch=FetchType.LAZY, optional = false)
	@JoinColumn(name="type", nullable = false)
	@ForeignKey(name="orders_ships_fk_shiptypes")
	private ShipType shipType;
	private int cost;
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="orderable_ship_fk_rasse")
	private Rasse rasse;

	/**
	 * Konstruktor.
	 *
	 */
	public OrderableShip() {
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param shipType Der Schiffstyp
	 * @param rasse Die Rasse, die die Bestellung aufgeben kann
	 * @param cost Die Kosten in NPC-Punkten
	 */
	public OrderableShip(ShipType shipType, Rasse rasse, int cost)
	{
		this.shipType = shipType;
		this.rasse = rasse;
		this.cost = cost;
	}

	/**
	 * Gibt die Rasse zurueck, die das Schiff ordern kann.
	 * @return Die Rasse
	 */
	public Rasse getRasse()
	{
		return this.rasse;
	}

	/**
	 * Die Kosten der Bestellung.
	 * @return Die Kosten
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Der Schiffstyp des bestellten Schiffes.
	 * @return Der Schiffstyp
	 */
	public ShipType getShipType() {
		return this.shipType;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Setzt den Schiffstyps des bestellten Schiffes.
	 * @param shipType Der Schiffstyp
	 */
	public void setShipType(ShipType shipType)
	{
		this.shipType = shipType;
	}

	/**
	 * Die Kosten der Bestellung.
	 * @param cost  Die Kosten
	 */
	public void setCost(int cost)
	{
		this.cost = cost;
	}

	/**
	 * Setzt die Rasse, die das Schiff ordern kann.
	 * @param rasse Die Rasse
	 */
	public void setRasse(Rasse rasse)
	{
		this.rasse = rasse;
	}
}
