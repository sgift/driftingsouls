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

import net.driftingsouls.ds2.server.framework.JSONSupport;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Repraesentiert einen Orden in DS.
 * @author Chirstopher Jung
 *
 */
public class Medal implements JSONSupport {
	/**
	 * Das normale Bild.
	 */
	public static final int IMAGE_NORMAL = 1;
	/**
	 * Das Highlight-Bild.
	 */
	public static final int IMAGE_HIGHLIGHT = 2;
	/**
	 * Das kleine Bild.
	 */
	public static final int IMAGE_SMALL = 3;
	
	private String name;
	private String image;
	private String imageHighlight;
	private	String imageSmall;
	private int id;
	private boolean adminOnly;
	
	/**
	 * Erstellt einen neuen Orden.
	 * @param id die ID des Ordens
	 * @param name der Name des Ordens
	 */
	public Medal(int id, String name ) {
		this.id = id;
		this.name = name;
		this.adminOnly = false;
	}
	
	/**
	 * Setzt die Bilder des Ordens.
	 * @param image Das normale Bild
	 * @param imageh Das highlight-Bild
	 * @param imagesmall das kleine Bild
	 */
	public void setImages( String image, String imageh, String imagesmall ) {
		this.image = image;
		this.imageHighlight = imageh;
		this.imageSmall = imagesmall;	
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
	public int getID() {
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
	 * Gibt das angegebene Bild des Ordens zurueck.
	 * @param imageid der Typ des Bildes
	 * @return der Pfad zum Bild oder <code>null</code>
	 */
	public String getImage( int imageid ) {
		if( imageid == IMAGE_NORMAL ) {
			return image;
		}
		else if( imageid == IMAGE_HIGHLIGHT ) {
			return imageHighlight;	
		}
		else if( imageid == IMAGE_SMALL ) {
			return imageSmall;	
		}
		return null;
	}

	@Override
	public JSON toJSON()
	{
		JSONObject medalObj = new JSONObject()
			.accumulate("name", this.name)
			.accumulate("id", this.id)
			.accumulate("image", this.image)
			.accumulate("imageSmall", this.imageSmall);
		return medalObj;
	}
}
