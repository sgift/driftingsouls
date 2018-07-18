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
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Repraesentiert die SectorTemplates von DS.
 */
@Entity
@Table(name="global_sectortemplates")
public class GlobalSectorTemplate
{
	@Id
	private String id;
	private int x;
	private int y;
	@Column(name="h", nullable = false)
	private int heigth;
	@Column(name="w", nullable = false)
	private int width;
	private int scriptid;
	
	/**
	 * Konstruktor.
	 */
	public GlobalSectorTemplate()
	{
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param id Der Name des Templates
	 * @param x X-Koordinate des Templates
	 * @param y Y-Koordinate des Templates
	 * @param width Breite die das Template nutzt
	 * @param heigth Hoehe die das Template nutzt
	 * @param scriptid Id des Scriptes das ausgefuehrt wird
	 */
	public GlobalSectorTemplate(String id,int x,int y,int width,int heigth,int scriptid)
	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.width = width;
		this.heigth = heigth;
		this.scriptid = scriptid;
	}
	
	/**
	 * Gibt den Namen des Templates zurueck.
	 * @return Der Name
	 */
	public String getId()
	{
		return id;
	}
	
	/**
	 * Gibt die X-Koordinate des Templates zurueck.
	 * @return Die X-Koordinate
	 */
	public int getX()
	{
		return x;
	}
	
	/**
	 * Gibt die Y-Koordinate des Templates zurueck.
	 * @return Die Y-Koordinate
	 */
	public int getY()
	{
		return y;
	}
	
	/**
	 * Gibt die Breite des Templates zurueck.
	 * @return Die Breite
	 */
	public int getWidth()
	{
		return width;
	}
	
	/**
	 * Gibt die Hoehe des Templates zurueck.
	 * @return Die Hoehe
	 */
	public int getHeigth()
	{
		return heigth;
	}
	
	/**
	 * Gibt die ID des Sciptes zurueck.
	 * @return Die ID
	 */
	public int getScriptId()
	{
		return scriptid;
	}
	
	/**
	 * Setzt den Namen des Templates.
	 * @param id Der Name
	 */
	public void setId(String id)
	{
		this.id = id;
	}
	
	/**
	 * Setzt die X-Koordinate des Templates.
	 * @param x Die X-Koordinate
	 */
	public void setX(int x)
	{
		this.x = x;
	}
	
	/**
	 * Setzt die Y-Koordinate des Templates.
	 * @param y Die Y-Koordinate
	 */
	public void setY(int y)
	{
		this.y = y;
	}
	
	/**
	 * Setzt die Breite des Templates.
	 * @param width Die Breite
	 */
	public void setWidth(int width)
	{
		this.width = width;
	}
	
	/**
	 * Setzt die Hoehe des Templates.
	 * @param heigth Die Hoehe
	 */
	public void setHeigth(int heigth)
	{
		this.heigth = heigth;
	}
	
	/**
	 * Setzt die ID des Scriptes.
	 * @param scriptid Die ID
	 */
	public void setScriptId(int scriptid)
	{
		this.scriptid = scriptid;
	}
}