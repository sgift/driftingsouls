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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Repraesentiert einen Orden in DS.
 * @author Chirstopher Jung
 *
 */
@Entity
public class Medal {
	@Id
	@GeneratedValue
	private int id;

	private String name;
	private String image;
	private	String imageSmall;
	private boolean adminOnly;

	/**
	 * Konstruktor.
	 */
	protected Medal()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param name Der Name
	 * @param image Das Standardbild
	 * @param imageSmall Das Icon
	 */
	public Medal(String name, String image, String imageSmall)
	{
		this.name = name;
		this.image = image;
		this.imageSmall = imageSmall;
	}

	/**
	 * Soll der Orden nur von Admins verliehen werden koennen?
	 * @param value <code>true</code>, falls nur Admins den Orden verleihen koennen duerfen
	 */
	public void setAdminOnly( boolean value ) {
		this.adminOnly = value;	
	}
	
	/**
	 * Gibt zurueck, ob der Orden nur von Admins verliehen werden kann.
	 * @return <code>true</code>, falls der Orden nur von Admins verliehen werden kann
	 */
	public boolean isAdminOnly() {
		return adminOnly;	
	}
	
	/**
	 * Gibt die ID des Ordens zurueck.
	 * @return die ID des Ordens
	 */
	public int getId() {
		return id;	
	}
	
	/**
	 * Gibt den Namen des Ordens zurueck.
	 * @return der Name des Ordens
	 */
	public String getName() {
		return name;	
	}

	/**
	 * Gibt Bild des Ordens in Miniaturform zurueck.
	 * @return der Pfad zum Bild oder <code>null</code>
	 */
	public String getImageSmall()
	{
		return imageSmall;
	}

	/**
	 * Gibt Bild des Ordens in normaler Groesse zurueck.
	 * @return der Pfad zum Bild oder <code>null</code>
	 */
	public String getImage() {
		return image;
	}

	/**
	 * Setzt den Namen des Ordens.
	 * @param name der Name des Ordens
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Setzt Bild des Ordens in normaler Groesse.
	 * @param image der Pfad zum Bild oder <code>null</code>
	 */
	public void setImage(String image)
	{
		this.image = image;
	}

	/**
	 * Setzt Bild des Ordens in Miniaturform.
	 * @param imageSmall der Pfad zum Bild oder <code>null</code>
	 */
	public void setImageSmall(String imageSmall)
	{
		this.imageSmall = imageSmall;
	}

	/**
	 * Konvertiert die angegebene ID in einen Orden.
	 * @param medalId Die ID
	 * @return Der Orden oder <code>null</code>
	 */
	public static Medal valueOf(String medalId)
	{
		return Medals.get().medal(Integer.parseInt(medalId));
	}
}
