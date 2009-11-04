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
package net.driftingsouls.ds2.server.bases;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.framework.Common;

/**
 * <p>Repraesentiert eine Basis-Klasse in DS.</p>
 * 
 */
@Entity
@Table(name="base_types")
public class BaseType
{
	@Id
	private int id;
	private String Name;
	private int width;
	private int height;
	private int maxtiles;
	private int cargo;
	private int energy;
	private String terrain;
	private String spawnableress;
	
	/**
	 * Konstruktor.
	 *
	 */
	public BaseType()
	{
		// EMPTY
	}
	
	/**
	 * Gibt die ID der Basis-Klasse zurueck.
	 * @return die ID der Basis-Klasse
	 */
	public int getId()
	{
		return this.id;
	}
	
	/**
	 * Gibt die Breite der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Breite
	 */
	public int getWidth()
	{
		return this.width;
	}
	
	/**
	 * Setzt die Breite der Bauflaeche auf der Basis.
	 * @param width Die Breite
	 */
	public void setWidth(int width)
	{
		this.width = width;
	}
	
	/**
	 * Gibt die Hoehe der Bauflaeche auf der Basis in Feldern zurueck.
	 * @return Die Hoehe
	 */
	public int getHeight()
	{
		return this.height;
	}
	
	/**
	 * Setzt die Hoehe der Bauflaeche auf der Basis.
	 * @param height Die Hoehe
	 */
	public void setHeight(int height)
	{
		this.height = height;
	}
	
	/**
	 * Gibt die benutzte Felderanzahl zurueck.
	 * @return die Felderanzahl
	 */
	public int getMaxTiles()
	{
		return this.maxtiles;
	}
	
	/**
	 * Setzt die benutzte Felderanzahl.
	 * @param maxtiles die neue Felderanzahl
	 */
	public void setMaxTiles(int maxtiles)
	{
		this.maxtiles = maxtiles;
	}
	
	/**
	 * Gibt den Namen der Basis zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return this.Name;
	}
	
	/**
	 * Gibt der Basis einen neuen Namen.
	 * @param name Der neue Name
	 */
	public void setName(String name)
	{
		this.Name = name;
	}
	
	/**
	 * Gibt die erlaubten Terrain-Typen der Basis-Klasse zurueck.
	 * @return Die erlaubten Terrain-Typen der Felder
	 */
	public Integer[] getTerrain()
	{
		if(this.terrain == null || this.terrain.equals(""))
		{
			return null;
		}
		return Common.explodeToInteger(";", this.terrain);
	}
	
	/**
	 * Setzt das neue Terrain der Basis.
	 * @param terrain Das neue Terrain
	 */
	public void setTerrain(final Integer[] terrain)
	{
		this.terrain = Common.implode(";", terrain);
	}
	
	/**
	 * Gibt den Max-Cargo der Basis-Klasse zurueck.
	 * @return Der Cargo
	 */
	public int getCargo()
	{
		return cargo;
	}
	
	/**
	 * Setzt den Max-Cargo der Basis-Klasse.
	 * @param cargo Der neue Cargo
	 */
	public void setCargo(int cargo) 
	{
		this.cargo = cargo;
	}

	/**
	 * Gibt die maximale Energiemenge der Basis-Klasse zurueck.
	 * @return Die Energiemenge
	 */
	public int getEnergy()
	{
		return this.energy;
	}
	
	/**
	 * Setzt die maximale Menge der auf der Basis-Klasse vorhandenen Energie.
	 * @param e Die auf der Basis vorhandene Energie
	 */
	public void setEnergy(int e)
	{
		this.energy = e;
	}
	
	/**
	 * Gibt die zum spawn freigegebenen Ressourcen zurueck.
	 * @return Die Spawn-Ressourcen als String (itemid,chance,maxmenge)
	 */
	public String getSpawnableRess() 
	{
		if (this.spawnableress == null )
		{
			return "";
		}
		return this.spawnableress;
	}
	
	/**
	 * Setzt die zum spawn freigegebenen Ressourcen zurueck.
	 * @param spawnableRess Die Spawn-Ressourcen als String
	 */
	public void setSpawnableRess(String spawnableRess)
	{
		this.spawnableress = spawnableRess;
	}
}
