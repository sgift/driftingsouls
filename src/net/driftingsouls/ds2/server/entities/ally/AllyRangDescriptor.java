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
package net.driftingsouls.ds2.server.entities.ally;

import net.driftingsouls.ds2.server.config.Medals;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Eine Rangbeschreibung einer (NPC-)Allianz. Ueberschreibt die
 * globalen Rangnamen bei durch NPCs verliehenen Raengen.
 * @author Christopher Jung
 * @see net.driftingsouls.ds2.server.entities.UserRank
 */
@Entity
@Table(name="ally_rangdescriptors")
public class AllyRangDescriptor implements Comparable<AllyRangDescriptor>
{
	@Id	@GeneratedValue
	private int id;
	
	@Version
	private int version;
	
	@ManyToOne(cascade={}, optional = false)
	@JoinColumn
	@ForeignKey(name = "ally_rangdescriptors_fk_ally")
	private Ally ally;
	
	private int rang;
	@Column(nullable = false)
	private String name;
	private String customImg;
	
	protected AllyRangDescriptor() {
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param ally Die Allianz, zu der die Rangbeschreibung gehoert
	 * @param rang Die Nummer des Rangs
	 * @param name Der Anzeigename
	 */
	public AllyRangDescriptor(Ally ally, int rang, String name)
	{
		this.ally = ally;
		this.rang = rang;
		this.name = name;
	}
	
	/**
	 * Gibt die Allianz zurueck, zu der die Rangbeschreibung gehoert.
	 * @return Die Allianz
	 */
	public Ally getAlly()
	{
		return this.ally;
	}
	
	/**
	 * Gibt die Nummer des Rangs zurueck.
	 * @return Die Nummer
	 */
	public int getRang()
	{
		return this.rang;
	}
	
	/**
	 * Gibt den Anzeigenamen des Rangs zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return this.name;
	}

	@Override
	public int compareTo(@NotNull AllyRangDescriptor o)
	{
		if( this.ally.getId() != o.getAlly().getId() )
		{
			return this.ally.getId() - o.getAlly().getId();
		}
		
		return this.rang - o.getRang();
	}

	/**
	 * Setzt den Anzeigenamen des Rangs.
	 * @param rangname Der Anzeigename
	 */
	public void setName(String rangname)
	{
		this.name = rangname;
	}

	/**
	 * Setzt die ID des Bilds dieses Ranges (DynamicContent).
	 * @param customImg Die ID
	 */
	public void setCustomImg(String customImg)
	{
		this.customImg = customImg;
	}
	
	/**
	 * Gibt, sofern vorhanden, die ID des Bilds dieses Ranges
	 * zurueck (DynamicContent).
	 * @return Die ID oder <code>null</code>
	 */
	public String getCustomImg()
	{
		return this.customImg;
	}
	
	/**
	 * Gibt das Bild (Pfad) des Ranges zurueck.
	 * @return Der Bildpfad
	 */
	public String getImage()
	{
		if( customImg != null )
		{
			return "data/dynamicContent/"+this.customImg;
		}
		return Medals.get().rang(this.rang).getImage();
	}
}
