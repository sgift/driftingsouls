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

import java.util.ArrayList;

import net.driftingsouls.ds2.server.Location;

/**
 * Repraesentiert ein Sternensystem in DS.
 * 
 * <p>Order-Locations: Geben Positionen an, in deren Umgebung neue Spieler nach der Registrierung "spawnen" koennen.</p>
 * 
 * @author Christopher Jung
 *
 */
public class StarSystem {
	/**
	 * Normaler Zugriffslevel - Alle Benutzer koennen das System sehen.
	 */
	public static final int AC_NORMAL = 1;
	/**
	 * NPC Zugriffslevel - Nur NPCs und Admins koennen das System sehen.
	 */
	public static final int AC_NPC = 2;
	/**
	 * Admin Zugriffslevel - Nur Admins koennen das System sehen.
	 */
	public static final int AC_ADMIN = 3;
	
	private String name = "";
	private int id = 0;
	private int width = 200;
	private int height = 200;
	private int maxColonys = -1;
	private boolean allowMilitary = true;
	private int starmap = StarSystem.AC_NORMAL;
	private Location gtuDropZone = null;
	private ArrayList<Location> orderloc = new ArrayList<Location>();
	private String description = "";

	protected StarSystem( int myid, String myname, int width, int height, boolean allowMilitary, int myStarMap) {
		name 		= myname;
		id 			= myid;
		this.width		= width;
		this.height		= height;
		this.allowMilitary = allowMilitary;
		starmap 		= myStarMap;
	}
	
	protected void addOrderLocation( Location orderloc ) {
		this.orderloc.add(orderloc);	
	}
	
	/**
	 * Gibt die Liste aller Order-Locations im System zurueck.
	 * @return die Liste aller Order-Locations
	 */
	public Location[] getOrderLocations() {
		return orderloc.toArray(new Location[orderloc.size()]);	
	}
	
	/**
	 * Gibt die anhand der ID spezifizierte Order-Location zurueck.
	 * @param locid die ID der Order-Location
	 * @return die Order-Location oder <code>null</code>
	 */
	public Location getOrderLocation( int locid ) {
		return orderloc.get(locid);	
	}
	
	protected void setDropZone( Location dropZone ) {
		gtuDropZone = dropZone;	
	}
	
	/**
	 * Liefert die Position der GTU-Dropzone im System.
	 * @return die Position der GTU-Dropzone
	 */
	public Location getDropZone() {
		return gtuDropZone;	
	}
	
	protected void setMaxColonys( int maxColonys ) {
		this.maxColonys = maxColonys;	
	}
	
	/**
	 * Gibt an, ob militaerische Einheiten im System zugelassen sind.
	 * @return <code>true</code>, falls militaerische Einheiten zugelassen sind
	 */
	public boolean isMilitaryAllowed() {
		return this.allowMilitary;	
	}
	
	/**
	 * Gibt die maximale zulaessige Anzahl an Kolonien
	 * innerhalb dieses Sternensystems zurueck. Sollte keine 
	 * Begrenzung existieren, wird <code>-1</code> zurueckgegeben.
	 * 
	 * @return Die max. Anzahl an Kolonien oder <code>-1</code>
	 */
	public int getMaxColonies() {
		return this.maxColonys;	
	}
	
	/**
	 * Gibt den Zugriffslevel zurueck, ab dem das Sternensystem sichtbar ist.
	 * Der Zugriffslevel wird ueber entsprechende User-Flags festgelegt.
	 * @return Der Zugriffslevel
	 */
	public int getAccess() {
		return this.starmap;	
	}
	
	/**
	 * Gibt die Breite in Feldern des Sternensystems zurueck.
	 * @return die Breite in Feldern
	 */
	public int getWidth() {
		return this.width;	
	}
	
	/**
	 * Gibt die Hoehe in Feldern des Sternensystems zurueck.
	 * @return die Hoehe
	 */
	public int getHeight() {
		return this.height;	
	}
	
	/**
	 * Gibt den Namen des Sternensystems zurueck.
	 * @return der Name
	 */
	public String getName() {
		return this.name;	
	}
	
	/**
	 * Gibt die ID des Sternensystems zurueck.
	 * @return die ID des Sternensystems
	 */
	public int getID() {
		return this.id;	
	}
	
	/**
	 * Gibt die Beschreibung des Sternensystems zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return this.description;
	}
	
	protected void setDescription( String text ) {
		this.description = text;
	}
}
