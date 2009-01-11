/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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

/**
 * Repraesentiert eine durch einen Spieler erforschte Technologie.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="userresearch")
public class UserResearch {
	@Id @GeneratedValue
	private int id;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="owner")
	private User owner;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="research")
	private Forschung research;
	
	/**
	 * Konstruktor.
	 */
	public UserResearch() {
		//EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param owner Der Spieler, welcher die Technologie erforscht hat
	 * @param research Die erforschte Technologie
	 */
	public UserResearch(User owner, Forschung research) {
		this.owner = owner;
		this.research = research;
	}

	/**
	 * Gibt die ID des Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt die erforschte Technologie zurueck.
	 * @return Die Technologie
	 */
	public Forschung getResearch() {
		return research;
	}

	/**
	 * Gibt den Spieler zurueck, der die Technologie erforscht hat.
	 * @return Der Spieler
	 */
	public User getOwner() {
		return owner;
	}
}
