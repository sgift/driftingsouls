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
import net.driftingsouls.ds2.server.framework.JSONSupport;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.hibernate.annotations.Immutable;

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
@Immutable
public class JumpNode implements Locatable, JSONSupport {
	@Id @GeneratedValue
	private int id;
	private int x;
	private int y;
	private int system;
	@Column(name="xout")
	private int xOut;
	@Column(name="yout")
	private int yOut;
	@Column(name="systemout")
	private int systemOut;
	private String name;
	@Column(name="wpnblock")
	private boolean weaponBlock;
	@Column(name="gcpcolonistblock")
	private boolean gcpColonistBlock;
	private int hidden;
	
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
		this.hidden = 0;
	}

	/**
	 * Gibt zurueck, ob die Jumpnode fuer Kolonisten blockiert ist.
	 * @return <code>true</code>, falls sie blockiert ist
	 */
	public boolean isGcpColonistBlock() {
		return gcpColonistBlock;
	}

	/**
	 * Gibt zurueck, ob die Jumpnode versteckt ist.
	 * @return <code>true</code>, falls sie versteckt ist
	 */
	public boolean isHidden() {
		return hidden != 0;
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
	 * Gibt das Eingangssystem zurueck.
	 * @return Das eingangssystem
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * Gibt das Ausgangssystem zurueck.
	 * @return Das Ausgangssystem
	 */
	public int getSystemOut() {
		return systemOut;
	}

	/**
	 * Gibt zurueck, ob Schiffe mit Waffen geblockt sind.
	 * @return <code>true</code>, falls sie geblockt sind
	 */
	public boolean isWeaponBlock() {
		return weaponBlock;
	}

	/**
	 * Gibt die Eingangs-X-Koordinate zurueck.
	 * @return Die Eingangs-X-Koordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gibt die Ausgangs-X-Koordinate zurueck.
	 * @return Die Ausgangs-X-Koordinate
	 */
	public int getXOut() {
		return xOut;
	}

	/**
	 * Gibt die Eingangs-Y-Koordinate zurueck.
	 * @return Die Eingangs-Y-Koordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Gibt die Ausgangs-Y-Koordinate zurueck.
	 * @return Die Ausgangs-Y-Koordinate
	 */
	public int getYOut() {
		return yOut;
	}

	@Override
	public Location getLocation() {
		return new Location(this.system, this.x, this.y);
	}

	@Override
	public JSON toJSON()
	{
		JSONObject nodeObj = new JSONObject();
		nodeObj.accumulate("system", this.system);
		nodeObj.accumulate("x", this.x);
		nodeObj.accumulate("y", this.y);
		nodeObj.accumulate("name", this.name);
		nodeObj.accumulate("systemout", this.systemOut);
		nodeObj.accumulate("blocked", this.gcpColonistBlock);
		nodeObj.accumulate("hidden", this.hidden != 0);

		return nodeObj;
	}
}
