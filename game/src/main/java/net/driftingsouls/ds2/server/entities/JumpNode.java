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
package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Ein Sprungpunkt.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="jumpnodes")
@org.hibernate.annotations.Table(
		appliesTo = "jumpnodes",
		indexes = {@Index(name="jumpnode_coords", columnNames = {"x", "y", "system"})})
public class JumpNode implements Locatable {
	@Id @GeneratedValue
	private int id;
	private int x;
	private int y;
	private int system;
	@Column(name="xout", nullable = false)
	private int xOut;
	@Column(name="yout", nullable = false)
	private int yOut;
	@Column(name="systemout", nullable = false)
	private int systemOut;
	@Column(nullable = false)
	private String name;
	@Column(name="wpnblock", nullable = false)
	private boolean weaponBlock;
	@Column(name="gcpcolonistblock", nullable = false)
	private boolean gcpColonistBlock;
	private boolean hidden;
	
	/**
	 * Konstruktor.
	 *
	 */
	public JumpNode() {
		// EMPTY
	}
	
	/**
	 * Erstellt einen neuen Sprungpunkt.
	 * @param source Der Ausgangspunkt
	 * @param target Der Zielpunkt
	 * @param name Der Name des Sprungpunkts
	 */
	public JumpNode(Location source, Location target, String name) {
		this.x = source.getX();
		this.y = source.getY();
		this.system = source.getSystem();
		this.xOut = target.getX();
		this.yOut = target.getY();
		this.systemOut = target.getSystem();
		this.name = name;
		this.weaponBlock = false;
		this.gcpColonistBlock = false;
		this.hidden = false;
	}

	/**
	 * Gibt zurueck, ob die Jumpnode fuer Kolonisten blockiert ist.
	 * @return <code>true</code>, falls sie blockiert ist
	 */
	public boolean isGcpColonistBlock() {
		return gcpColonistBlock;
	}

	/**
	 * Setzt, ob die Jumpnode fuer Kolonisten blockiert ist.
	 * @param gcpColonistBlock <code>true</code>, falls sie blockiert ist
	 */
	public void setGcpColonistBlock(boolean gcpColonistBlock)
	{
		this.gcpColonistBlock = gcpColonistBlock;
	}

	/**
	 * Gibt zurueck, ob die Jumpnode versteckt ist.
	 * @return <code>true</code>, falls sie versteckt ist
	 */
	public boolean isHidden() {
		return hidden;
	}

	/**
	 * Setzt, ob die Jumpnode versteckt ist.
	 * @param hidden <code>true</code>, falls sie versteckt ist
	 */
	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}

	/**
	 * Gibt die ID der Jumpnode zurueck.
	 * @return Die ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Gibt den Namen der Jumpnode zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen der Jumpnode.
	 * @param name  Der Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Gibt das Eingangssystem zurueck.
	 * @return Das eingangssystem
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Setzt das Eingangssystem.
	 * @param system Das eingangssystem
	 */
	public void setSystem(int system)
	{
		this.system = system;
	}

	/**
	 * Gibt das Ausgangssystem zurueck.
	 * @return Das Ausgangssystem
	 */
	public int getSystemOut() {
		return systemOut;
	}

	/**
	 * Setzt das Ausgangssystem.
	 * @param systemOut Das Ausgangssystem
	 */
	public void setSystemOut(int systemOut)
	{
		this.systemOut = systemOut;
	}

	/**
	 * Gibt zurueck, ob Schiffe mit Waffen geblockt sind.
	 * @return <code>true</code>, falls sie geblockt sind
	 */
	public boolean isWeaponBlock() {
		return weaponBlock;
	}

	/**
	 * Setzt, ob Schiffe mit Waffen geblockt sind.
	 * @param weaponBlock <code>true</code>, falls sie geblockt sind
	 */
	public void setWeaponBlock(boolean weaponBlock)
	{
		this.weaponBlock = weaponBlock;
	}

	/**
	 * Gibt die Eingangs-X-Koordinate zurueck.
	 * @return Die Eingangs-X-Koordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Setzt die Eingangs-X-Koordinate.
	 * @param x Die Eingangs-X-Koordinate
	 */
	public void setX(int x)
	{
		this.x = x;
	}

	/**
	 * Gibt die Ausgangs-X-Koordinate zurueck.
	 * @return Die Ausgangs-X-Koordinate
	 */
	public int getXOut() {
		return xOut;
	}

	/**
	 * Setzt die Ausgangs-X-Koordinate.
	 * @param xOut Die Ausgangs-X-Koordinate
	 */
	public void setXOut(int xOut)
	{
		this.xOut = xOut;
	}

	/**
	 * Gibt die Eingangs-Y-Koordinate zurueck.
	 * @return Die Eingangs-Y-Koordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Setzt die Eingangs-Y-Koordinate.
	 * @param y Die Eingangs-Y-Koordinate
	 */
	public void setY(int y)
	{
		this.y = y;
	}

	/**
	 * Gibt die Ausgangs-Y-Koordinate zurueck.
	 * @return Die Ausgangs-Y-Koordinate
	 */
	public int getYOut() {
		return yOut;
	}

	/**
	 * Setzt die Ausgangs-Y-Koordinate.
	 * @param yOut Die Ausgangs-Y-Koordinate
	 */
	public void setYOut(int yOut)
	{
		this.yOut = yOut;
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}
}
