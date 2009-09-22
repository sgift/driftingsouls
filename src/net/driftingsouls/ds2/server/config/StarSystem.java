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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.Location;

/**
 * Repraesentiert ein Sternensystem in DS.
 * 
 * <p>Order-Locations: Geben Positionen an, in deren Umgebung neue Spieler nach der Registrierung "spawnen" koennen.</p>
 * 
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="systems")
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
	
	@Column(name="Name")
	private String name = "";
	@Id
	private int id = 0;
	private int width = 200;
	private int height = 200;
	private int maxColonies = -1;
	@Column(name="military")
	private boolean allowMilitary = true;
	@Column(name="access")
	private int starmap = StarSystem.AC_NORMAL;
	private Location gtuDropZone = null;
	private ArrayList<Location> orderloc = new ArrayList<Location>();
	@Column(name="descrip")
	private String description = "";
	@Column(name="starmap")
	private boolean isStarmapVisible = false;

	protected StarSystem() {
		// Empty
	}
	
	protected StarSystem( int myid, String myname, int width, int height, boolean allowMilitary, int myStarMap, String myDescription, Location mygtuDropZone) {
		name 		= myname;
		id 			= myid;
		this.width		= width;
		this.height		= height;
		this.allowMilitary = allowMilitary;
		starmap 		= myStarMap;
		this.description = myDescription;
		this.gtuDropZone = mygtuDropZone;
	}
	
	protected void addOrderLocation( Location orderloc ) {
		this.orderloc.add(orderloc);	
	}
	
	protected void removeOrderLocation( Location orderloc) {
		this.orderloc.remove(orderloc);
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
	
	/**
	 * Setzt die Location der DropZone.
	 * @param dropZone Die Location der DropZone
	 */
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
	
	/**
	 * Setzt die maximale Anzahl an Kolonien in diesem System.
	 * @param maxColonies Die Anzahl der maximalen Kolonien (-1 fuer keine Begrenzung)
	 */
	protected void setMaxColonies( int maxColonies ) {
		this.maxColonies = maxColonies;	
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
		return this.maxColonies;	
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
	 * Setzt den Namen des Sternensystems.
	 * @param name Der neue Name des Sternensystems
	 */
	public void setName(String name) {
		this.name = name;
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
	
	/**
	 * Setzt die Beschreibung des Sternensystems.
	 * @param text Der neue Berschreibungstext
	 */
	protected void setDescription( String text ) {
		this.description = text;
	}
	
	/**
	 * Gibt zurueclk ob dieses System per Sternenkarte angesehen werden kann.
	 * @return <code>true</code> falls dieses System auf der Sternenkarte angesehen werden kann, ansonsten <code>false</code>
	 */
	public boolean isStarmapVisible() {
		return this.isStarmapVisible;
	}
	
	/**
	 * Setzt, ob dieses System auf der Sternenkarte angeschaut werden kann.
	 * @param visible <code>true</code> falls es angeschaut werden können soll, ansonsten <code>false</code>
	 */
	public void setStarmapVisible(boolean visible) {
		this.isStarmapVisible = visible;
	}
}
