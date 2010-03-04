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

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Diese Klasse repraesentiert alle UnitCargo-Eintraege.
 */
@Entity
@Table(name="cargo_entries_units")
public class UnitCargoEntry
{
	/**
	 * Diese Klasse repraesentiert einen Primarschluessel fuer einen Cargo-Eintrag.
	 */
	@Embeddable
	public static class UnitCargoEntryKey implements Serializable
	{
		private static final long serialVersionUID = 1510394179895753873L;
		
		private int type;
		private int destid;
		private int unittype;
		
		/**
		 * Konstruktor.
		 */
		public UnitCargoEntryKey()
		{
			// EMPTY
		}
		
		/**
		 * Konstruktor.
		 * @param type Der Typ des Eintrages
		 * @param destid Die ID des Ziels
		 * @param unittype Die ID des Einheitentyps
		 */
		public UnitCargoEntryKey(int type, int destid, int unittype)
		{
			this.type = type;
			this.destid = destid;
			this.unittype = unittype;
		}
		
		/**
		 * Gibt den Typ des Eintrages zurueck.
		 * @return der Typ
		 */
		public int getTyp()
		{
			return type;
		}
		
		/**
		 * Gibt die ZielId des Eintrages zurueck.
		 * @return die ZielID
		 */
		public int getDestId()
		{
			return destid;
		}
		
		/**
		 * Gibt die ID des UnitTyps zurueck.
		 * @return Die ID
		 */
		public int getUnitTypeId()
		{
			return unittype;
		}
		
		/**
		 * Gibt den UnitTyp zurueck.
		 * @return der UnitTyp
		 */
		public UnitType getUnitType()
		{
			org.hibernate.Session db = ContextMap.getContext().getDB();
			return (UnitType)db.get(UnitType.class, unittype);
		}
		
		/**
		 * Setzt den Eintrags-Typ.
		 * @param type der Typ
		 */
		public void setTyp(int type)
		{
			this.type = type;
		}
		
		/**
		 * Setzt die ID des Zielobjekts.
		 * @param destid Die ID
		 */
		public void setDestId(int destid)
		{
			this.destid = destid;
		}
		
		/**
		 * Setzt die ID des UnitTyps.
		 * @param unittype Die ID
		 */
		public void setUnitType(int unittype)
		{
			this.unittype = unittype;
		}
		
		/**
		 * Setzt den UnitTyp.
		 * @param unittype der UnitTyp
		 */
		public void setUnitType(UnitType unittype)
		{
			setUnitType(unittype.getId());
		}
	}
	
	@Id
	private UnitCargoEntryKey key;
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
	 * @param type Der Typ des Eintrages
	 * @param destid Die ID des Zielobjekts
	 * @param unittype Die ID des Einheitentyps
	 * @param amount Die Menge
	 */
	public UnitCargoEntry(int type, int destid, int unittype, long amount)
	{
		this.key = new UnitCargoEntryKey(type,destid,unittype);
		this.amount = amount;
	}
	
	/**
	 * Konstruktor.
	 * @param type Der Typ des Eintrages
	 * @param destid Die ID des Zielobjekts
	 * @param unittype Der Einheitentyp
	 * @param amount Die Menge
	 */
	public UnitCargoEntry(int type, int destid, UnitType unittype, long amount)
	{
		this(type,destid,unittype.getId(),amount);
	}
	
	/**
	 * Gibt den Typ des Eintrages zurueck.
	 * @return Der Typ
	 */
	public int getTyp()
	{
		return this.key.getTyp();
	}
	
	/**
	 * Gibt die ID des Zielobjekts zurueck.
	 * @return Die ID
	 */
	public int getDestId()
	{
		return this.key.getDestId();
	}
	
	/**
	 * Gibt die ID des Einheitentyps zurueck.
	 * @return Die ID
	 */
	public int getUnitTypeId()
	{
		return this.key.getUnitTypeId();
	}
	
	/**
	 * Gibt den Einheitentyp zurueck.
	 * @return Der Einheitentyp
	 */
	public UnitType getUnitType()
	{
		return this.key.getUnitType();
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
	 * Setzt den Typ des Eintrages.
	 * @param typ Der Typ
	 */
	public void setTyp(int typ)
	{
		this.key.setTyp(typ);
	}
	
	/**
	 * Setzt die ID des Zielobjekts.
	 * @param destid Die ID
	 */
	public void setDestId(int destid)
	{
		this.key.setDestId(destid);
	}
	
	/**
	 * Setzt die ID des Einheitentyps.
	 * @param unittype Die ID
	 */
	public void setUnitType(int unittype)
	{
		this.key.setUnitType(unittype);
	}
	
	/**
	 * Setzt den Einheitentyp.
	 * @param unittype Der Einheitentyp
	 */
	public void setUnitType(UnitType unittype)
	{
		this.key.setUnitType(unittype);
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
	public UnitCargoEntry clone()
	{
		return new UnitCargoEntry(getTyp(),getDestId(),getUnitTypeId(),getAmount());
	}
}