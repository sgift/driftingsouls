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
package net.driftingsouls.ds2.server.config;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Repraesentiert einen Rang in DS.
 * @author Christopher Jung
 *
 */
@Entity
public class Rang implements Comparable<Rang>
{
	@Id
	private int id;
	private String name;
	private String image;

	protected Rang()
	{
		// EMPTY
	}

	/**
	 * Erstellt einen neuen Rang.
	 *
	 * @param id die ID des Ranges
	 * @param name der Name des Ranges
	 * @param image Das zum Rang gehoerende Bild
	 */
	public Rang(int id, String name, String image)
	{
		super();
		this.id = id;
		this.name = name;
		this.image = image;
	}

	/**
	 * Gibt die ID des Ranges zurueck.
	 *
	 * @return die ID des Ranges
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Setzt die ID des Ranges.
	 *
	 * @param id die ID des Ranges
	 */
	public void setId(int id)
	{
		this.id = id;
	}

	/**
	 * Gibt den Namen des Ranges zurueck.
	 *
	 * @return der Name des Ranges
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt den Pfad zur Ranggrafik zurueck.
	 *
	 * @return Der Pfad
	 */
	public String getImage()
	{
		return this.image;
	}

	/**
	 * Setzt den Namen des Ranges.
	 *
	 * @param name der Name des Ranges
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Setzt den Pfad zur Ranggrafik.
	 *
	 * @param image Der Pfad
	 */
	public void setImage(String image)
	{
		this.image = image;
	}

	@Override
	public int compareTo(@Nonnull Rang o)
	{
		return this.id - o.id;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rang other = (Rang) obj;
		return id == other.id;
	}
}
