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

import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Ein fuer NPCs bestellbarer Offizier.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="orders_offiziere")
@Immutable
public class OrderableOffizier {
	@Id @GeneratedValue
	private int id;
	@Column(nullable = false)
	private String name;
	private int rang;
	private int ing;
	private int waf;
	private int nav;
	private int sec;
	private int com;
	private int cost;

	/**
	 * Konstruktor.
	 *
	 */
	protected OrderableOffizier() {
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param name Der Name des Eintrags
	 * @param cost Die Kosten (NPC-Punkte)
	 * @param rang Der Rang
	 * @param ing Die Ingenieursfaehigkeit
	 * @param waf Die Waffenfaehigkeit
	 * @param nav Die Navigationsfaehigkeit
	 * @param sec Die Sicherheitsfaehigkeit
	 * @param com Die Kommunikationsfaehigkeit
	 */
	public OrderableOffizier(String name, int cost, int rang, int ing, int waf, int nav, int sec, int com) {
		this.name = name;
		this.rang = rang;
		this.ing = ing;
		this.waf = waf;
		this.nav = nav;
		this.sec = sec;
		this.com = com;
		this.cost = cost;
	}

	/**
	 * Gibt den Kommunikationsskill zurueck.
	 * @return Der Kommunikationsskill
	 */
	public int getCom() {
		return com;
	}

	/**
	 * Gibt die Orderkosten zurueck.
	 * @return Die Kosten
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den Technikskill zurueck.
	 * @return Der Technikskill
	 */
	public int getIng() {
		return ing;
	}

	/**
	 * Gibt den Namen zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gibt den Navigationsskill zurueck.
	 * @return Der Navigationsskill
	 */
	public int getNav() {
		return nav;
	}

	/**
	 * Gibt den Rang zurueck.
	 * @return Der Rang
	 */
	public int getRang() {
		return rang;
	}

	/**
	 * Gibt den Sicherheitsskill zurueck.
	 * @return Der Skill
	 */
	public int getSec() {
		return sec;
	}

	/**
	 * Gibt den Waffenskill zurueck.
	 * @return Der Skill
	 */
	public int getWaf() {
		return waf;
	}
	
	
}
