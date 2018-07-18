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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ForeignKey;

/**
 * Ein Eintrag der Kaserne.
 *
 */
@Entity
@Table(name="kaserne_queues")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class KaserneEntry {

	@Id @GeneratedValue
	private int id;
	@ManyToOne(optional = false)
	@JoinColumn(name="kaserne", nullable = false)
	@ForeignKey(name="kaserne_queues_fk_kaserne")
	private Kaserne kaserne;
	@ManyToOne(optional = false)
	@JoinColumn(name="unitid", nullable = false)
	@ForeignKey(name="kaserne_queues_fk_unittype")
	private UnitType unit;
	private int count;
	private int remaining;

	/**
	 * Konstruktor.
	 *
	 */
	public KaserneEntry()
	{
		// EMPTY
	}

	/**
	 * Der Konstruktor.
	 * @param kaserne Die Kaserne
	 * @param unitid Die zu bauenden Einheit
	 */
	public KaserneEntry(Kaserne kaserne, UnitType unitid)
	{
		this.kaserne = kaserne;
		this.unit = unitid;
	}

	/**
	 * Gibt die ID zurueck.
	 * @return Die ID
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Gibt die Kaserne zurueck, in der dieser Eintrag laeuft.
	 * @return Die Kaserne
	 */
	public Kaserne getKaserne()
	{
		return kaserne;
	}

	/**
	 * Gibt die zu gebauten Einheit zurueck.
	 * @return Die Einheit
	 */
	public UnitType getUnit()
	{
		return unit;
	}

	/**
	 * Gibt die Menge der auszubildenden Einheiten zurueck.
	 * @return Die Menge
	 */
	public int getCount()
	{
		return count;
	}

	/**
	 * Gibt die Anzahl der verbleibenen Ticks bis zur Fertigstellung zurueck.
	 * @return Die verbleibenden Ticks
	 */
	public int getRemaining()
	{
		return remaining;
	}

	/**
	 * Setzt die Anzahl der zu bauenden Einheiten.
	 * @param count Die neue Anzahl
	 */
	public void setCount(int count)
	{
		this.count = count;
	}

	/**
	 * Setzt die Anzahl der verbleibenden Ticks.
	 * @param remaining Die neue Anzahl
	 */
	public void setRemaining(int remaining)
	{
		this.remaining = remaining;
	}

	/**
	 * Beendet diesen Ausbildungsauftrag.
	 * @param base Die Basis auf der die zugehoerige Kaserne steht
	 */
	public void finishBuildProcess(Base base)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		UnitCargo unitcargo = base.getUnits();

		unitcargo.addUnit(getUnit(), getCount());

		base.setUnits(unitcargo);

		db.delete(this);
	}
}
