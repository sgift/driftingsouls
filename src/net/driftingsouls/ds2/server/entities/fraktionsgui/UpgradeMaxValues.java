/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.entities.fraktionsgui;

import net.driftingsouls.ds2.server.bases.BaseType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Maximalwerte der einzelnen Asteroidentypen beim Basisausbau. Wenn
 * ein Asteroidentyp keine Werte hat, kann er nicht ausgebaut werden.
 * Zu jedem Asteroidentyp kann es nur einen Datensatz geben.
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="upgrade_maxvalues")
public class UpgradeMaxValues
{
	@Id @GeneratedValue
	private int id;

	@OneToOne(optional = false)
	@JoinColumn(name="type", nullable = false)
	@ForeignKey(name="upgrade_max_values_fk_basetype")
	private BaseType type;
	private int maxcargo;
	private int maxtiles;
	
	/**
	 * Konstruktor.
	 */
	public UpgradeMaxValues() {
		// Empty
	}

	/**
	 * Gibt den Asteroidentyp zurueck.
	 * @return Der Typ
	 */
	public BaseType getType()
	{
		return type;
	}

	/**
	 * Setzt den Asteroidentyp.
	 * @param type Der Typ
	 */
	public void setType(BaseType type)
	{
		this.type = type;
	}

	/**
	 * Gibt den maximalen Cargo zurueck.
	 * @return the maxcargo Der maximale Cargo
	 */
	public int getMaxCargo()
	{
		return maxcargo;
	}

	/**
	 * Setzt den maximalen Cargo.
	 * @param maxcargo der maximale Cargo
	 */
	public void setMaxCargo(int maxcargo)
	{
		this.maxcargo = maxcargo;
	}

	/**
	 * Gibt die maximale Anzahl an Feldern zurueck.
	 * @return Die maximale Anzahl an Feldern
	 */
	public int getMaxTiles()
	{
		return maxtiles;
	}

	/**
	 * Setzt die maximale Anzahl an Feldern.
	 * @param maxtiles Die maximale Anzahl
	 */
	public void setMaxTiles(int maxtiles)
	{
		this.maxtiles = maxtiles;
	}
}
