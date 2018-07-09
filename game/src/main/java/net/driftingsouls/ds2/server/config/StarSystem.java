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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
	public enum Access
	{
		/**
		 * Das System ist grundsaetzlich nicht im Spiel sichtbar.
		 */
		NICHT_SICHTBAR,
		/**
		 * Normaler Zugriffslevel - Alle Benutzer koennen das System sehen.
		 */
		NORMAL,
		/**
		 * NPC Zugriffslevel - Nur NPCs und Admins koennen das System sehen.
		 */
		NPC,
		/**
		 * Admin Zugriffslevel - Nur Admins koennen das System sehen.
		 */
		ADMIN
	}

	@Column(name="Name", nullable = false)
	private String name = "";
	@Id @GeneratedValue
	private int id = 0;
	private int width = 200;
	private int height = 200;
	private int maxColonies = -1;
	@Column(name="military", nullable = false)
	private boolean allowMilitary = true;
	@Column(name="access", nullable = false)
	@Enumerated(EnumType.ORDINAL)
	private Access starmap = Access.NORMAL;
	private String gtuDropZone = null;
	@Lob
	private String orderloc = "";
	@Lob
	@Column(name="descrip")
	private String description = "";
	@Column(name="starmap", nullable = false)
	private boolean isStarmapVisible = false;
	@Lob
	private String spawnableress = "";
	private int mapX;
	private int mapY;

	@OneToMany(mappedBy = "system", cascade = CascadeType.ALL)
	private Set<ConfigFelsbrockenSystem> felsbrockenKonfigurationen = new HashSet<>();

	@Transient
	private ArrayList<Location> orderlocs = new ArrayList<>();

	/**
	 * Standardkonstruktor.
	 */
	public StarSystem() {
		//Empty
	}

	/**
	 * Der Konstruktor.
	 * @param id Die ID des neuen Systems
	 */
	public StarSystem(int id) {
		this.id = id;
	}

	/**
	 * Gibt die X-Position auf der Systemuebersichtskarte zurueck.
	 * @return Die X-Position
	 */
	public int getMapX()
	{
		return mapX;
	}

	/**
	 * Setzt die X-Position auf der Systemuebersichtskarte.
	 * @param mapX Die X-Position
	 */
	public void setMapX(int mapX)
	{
		this.mapX = mapX;
	}

	/**
	 * Gibt die Y-Position auf der Systemuebersichtskarte zurueck.
	 * @return Die Y-Position
	 */
	public int getMapY()
	{
		return mapY;
	}

	/**
	 * Setzt die Y-Position auf der Systemuebersichtskarte
	 * @param mapY Die Y-Position
	 */
	public void setMapY(int mapY)
	{
		this.mapY = mapY;
	}

	/**
	 * Schreibt alle Locations aus dem Array in einen String.
	 */
	private void locationstoString() {
		if(orderlocs.isEmpty()) {
			this.orderloc = "";
		}
		else {
			for(int i=0; i < orderlocs.size(); i++) {
				if( i != 0 ) {
					this.orderloc = this.orderloc + "|" + orderlocs.get(i).getX() + "/" + orderlocs.get(i).getY();
				}
				else
				{
					this.orderloc = orderlocs.get(i).getX() + "/" + orderlocs.get(i).getY();
				}
			}
		}
	}

	/**
	 * Schreibt den String in die Locations um.
	 */
	private void StringtoLocations() {
		this.orderlocs.clear();
		if(!"".equals(this.orderloc) && this.orderloc != null) {
			String[] locations = StringUtils.split(this.orderloc, "|");
			for (String location : locations)
			{
				if (!"".equals(location) && location != null)
				{
					Location orderloc = Location.fromString(location).setSystem(this.id);
					addOrderLocation(orderloc);
				}
			}
		}
	}

	protected void addOrderLocation( Location orderloc ) {
		this.orderlocs.add(orderloc);
		locationstoString();
	}

	protected void removeOrderLocation( Location orderloc) {
		this.orderlocs.remove(orderloc);
		locationstoString();
	}

	/**
	 * Gibt die Liste aller Order-Locations im System zurueck.
	 * @return die Liste aller Order-Locations
	 */
	public Location[] getOrderLocations() {
		StringtoLocations();
		return orderlocs.toArray(new Location[orderlocs.size()]);
	}

	/**
	 * Setzt die Liste aller Order-Locations im System.
	 * @param locations Die Liste der Order Locations.
	 */
	public void setOrderLocations(String locations) {
		this.orderloc = locations;
		StringtoLocations();
	}

	/**
	 * Gibt die anhand der ID spezifizierte Order-Location zurueck.
	 * @param locid die ID der Order-Location
	 * @return die Order-Location oder <code>null</code>
	 */
	public Location getOrderLocation( int locid ) {
		StringtoLocations();
		return orderlocs.get(locid);
	}

	/**
	 * Gibt die Order-Locations als String zurueck.
	 * @return Die Locations als String
	 */
	public String getOrderLocationString() {
		if (this.orderloc == null) {
			return "";
		}
		return this.orderloc;
	}

	/**
	 * Setzt die Location der DropZone.
	 * @param dropZone Die Location der DropZone
	 */
	public void setDropZone( Location dropZone ) {
		if(dropZone.getX() == 0 && dropZone.getY() == 0)
		{
			gtuDropZone = null;
		}
		else
		{
			gtuDropZone = dropZone.getX() + "/" + dropZone.getY();
		}
	}

	/**
	 * Liefert die Position der GTU-Dropzone im System.
	 * @return die Position der GTU-Dropzone
	 */
	public Location getDropZone() {
		if(gtuDropZone == null) {
			return null;
		}
		return Location.fromString(gtuDropZone).setSystem(id);
	}

	/**
	 * Liefert die Position der Dropzone als String.
	 * @return Die Position als String
	 */
	public String getDropZoneString() {
		if (this.gtuDropZone == null) {
			return "";
		}
		return this.gtuDropZone;
	}

	/**
	 * Setzt die maximale Anzahl an Kolonien in diesem System.
	 * @param maxColonies Die Anzahl der maximalen Kolonien (-1 fuer keine Begrenzung)
	 */
	public void setMaxColonies( int maxColonies ) {
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
	 * Setzt, ob militaerische Einheiten im System zugelassen sind.
	 * @param allowed <code>true</code> wenn erlaubt, ansonsten <code>false</code>
	 */
	public void setMilitaryAllowed(boolean allowed) {
		this.allowMilitary = allowed;
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
	public Access getAccess() {
		return this.starmap;
	}

	/**
	 * Setzt den Zugriffslevel, ab dem das Sternensystem sichtbar ist.
	 * @param access Der Zugriffslevel
	 */
	public void setAccess(Access access) {
		this.starmap = access;
	}

	/**
	 * Gibt die Breite in Feldern des Sternensystems zurueck.
	 * @return die Breite in Feldern
	 */
	public int getWidth() {
		return this.width;
	}

	/**
	 * Setzt die Breite in Feldern des Sternensystems.
	 * @param width Die Breite in Feldern
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Gibt die Hoehe in Feldern des Sternensystems zurueck.
	 * @return die Hoehe
	 */
	public int getHeight() {
		return this.height;
	}

	/**
	 * Setzt die Hoehe in Feldern des Sternensystems.
	 * @param height Die Hoehe
	 */
	public void setHeight(int height) {
		this.height = height;
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
		if( this.description == null) {
			return "";
		}
		return this.description;
	}

	/**
	 * Setzt die Beschreibung des Sternensystems.
	 * @param text Der neue Berschreibungstext
	 */
	public void setDescription( String text ) {
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

	/**
	 * Gibt einen String mit Ressourcen zurueck, die in diesem System vorkommen koennen.
	 * @return Die vorkommenden Ressourcen (itemid,chance,maxmenge)
	 */
	public String getSpawnableRess()
	{
		return this.spawnableress;
	}

	/**
	 * Setzt einen String mit Ressourcen, die in diesem System vorkommen koennen.
	 * @param spawnableress Die vorkommenden Ressourcen
	 */
	public void setSpawnableRess(String spawnableress) {
		this.spawnableress = spawnableress;
	}

	/**
	 * Gibt zurueck, ob der angegebene Spieler das Sternensystem (als Karte) sehen kann.
	 * @param user Der Spieler
	 * @return <code>true</code>, falls er das System sehen darf
	 */
	public boolean isVisibleFor(User user)
	{
		if( this.starmap == Access.NICHT_SICHTBAR )
		{
			return false;
		}
		if( user.hasFlag(UserFlag.VIEW_ALL_SYSTEMS) )
		{
			return true;
		}
		if( this.starmap == Access.ADMIN )
		{
			return false;
		}
		if( this.starmap == Access.NPC )
		{
			return user.hasFlag(UserFlag.VIEW_SYSTEMS);
		}
		return this.isStarmapVisible;
	}

	/**
	 * Gibt alle Felsbrocken-Konfigurationen des Sternensystems zurueck.
	 * @return Die Felsbrocken-Konfigurationen
	 */
	public Set<ConfigFelsbrockenSystem> getFelsbrockenKonfigurationen()
	{
		return felsbrockenKonfigurationen;
	}

}
