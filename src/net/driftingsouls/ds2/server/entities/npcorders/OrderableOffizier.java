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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

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
	public OrderableOffizier() {
		// EMPTY
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
