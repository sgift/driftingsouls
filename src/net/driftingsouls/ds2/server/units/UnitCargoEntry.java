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

package net.driftingsouls.ds2.server.units;

import net.driftingsouls.ds2.server.framework.JSONSupport;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Diese Klasse repraesentiert alle UnitCargo-Eintraege.
 */
@Entity
@Table(name="cargo_entries_units")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="type")
public abstract class UnitCargoEntry implements JSONSupport
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name="unittype")
	private UnitType unittype;

	private long amount;

	/**
	 * Konstruktor.
	 */
	public UnitCargoEntry()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param unittype Der Einheitentyps
	 * @param amount Die Menge
	 */
	public UnitCargoEntry(UnitType unittype, long amount)
	{
		this.unittype = unittype;
		this.amount = amount;
	}

	/**
	 * Gibt die ID des UnitTyps zurueck.
	 * @return Die ID
	 */
	public int getUnitTypeId()
	{
		return unittype.getId();
	}

	/**
	 * Gibt den UnitTyp zurueck.
	 * @return der UnitTyp
	 */
	public UnitType getUnitType()
	{
		return unittype;
	}

	/**
	 * Setzt den Eintrags-Typ.
	 * @param type der Typ
	 */
	public void setTyp(int type)
	{
		//this.type = type;
	}

	/**
	 * Setzt den UnitTyp.
	 * @param unittype der UnitTyp
	 */
	public void setUnitType(UnitType unittype)
	{
		this.unittype = unittype;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + unittype.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if( this == obj )
		{
			return true;
		}
		if( obj == null )
		{
			return false;
		}
		if( obj instanceof HibernateProxy )
		{
			obj = ((HibernateProxy)obj).getHibernateLazyInitializer().getImplementation();
		}
		if( getClass() != obj.getClass() )
		{
			return false;
		}
		UnitCargoEntry other = (UnitCargoEntry)obj;
		return unittype == other.unittype;
	}

	/**
	 * Gibt die Menge zurueck.
	 * @return Die Menge
	 */
	public long getAmount()
	{
		return this.amount;
	}

	/**
	 * Setzt die Menge.
	 * @param amount Die Menge
	 */
	public void setAmount(long amount)
	{
		this.amount = amount;
	}

	@Override
	public JSON toJSON()
	{
		JSONObject result = new JSONObject();
		result.accumulate("id", this.getUnitTypeId())
			.accumulate("count", amount)
			.accumulate("name", this.getUnitType().getName())
			.accumulate("picture", this.getUnitType().getPicture());

		return result;
	}

	/**
	 * Erstellt eine Kopie des UnitCargo-Eintrags.
	 * @return Die Kopie
	 */
	public abstract UnitCargoEntry createCopy();
}