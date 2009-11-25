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
package net.driftingsouls.ds2.server.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * Repraesentiert eine Kaserne auf einer Basis in DS.
 *
 */
@Entity
@Table(name="kaserne")
public class Kaserne {
	@Id
	private int id;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="col", nullable=false)
	private Base base;

	/**
	 * Konstruktor.
	 *
	 */
	public Kaserne() 
	{
		// EMPTY
	}
	
	/**
	 * Konstruktor.
	 * @param base Die Basis, auf der diese Kaserne steht
	 */
	public Kaserne(Base base)
	{
		this.base = base;
	}
	
	/**
	 * Gibt die ID der Kaserne zurueck.
	 * @return Die ID
	 */
	public int getId()
	{
		return id;
	}
	
	/**
	 * Gibt die Eintraege dieser Kaserne als Liste aus.
	 * @return Die Eintrage der Kaserne
	 */
	public KaserneEntry[] getQueueEntries()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<KaserneEntry> entrylist = Common.cast(db.createQuery("from KaserneEntry WHERE kaserne=:kaserne").setInteger("kaserne", id).list());
		
		KaserneEntry[] entries = new KaserneEntry[entrylist.size()];
		
		int zaehler = 0;
		for(KaserneEntry entry : entrylist)
		{
			entries[zaehler++] = entry;
		}
		
		return entries;
	}
	
	/**
	 * Gibt die Basis zurueck, auf der diese Kaserne steht.
	 * @return Die Basis
	 */
	public Base getBase()
	{
		return base;
	}
	
	/**
	 * Loescht alle Bauschlangeneintraege und danach sich selbst.
	 */
	public void destroy()
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		KaserneEntry[] entrylist = getQueueEntries();
		
		for(KaserneEntry entry : entrylist)
		{
			db.delete(entry);
		}
		
		db.delete(this);
	}
	
	/**
	 * Gibt zurueck, ob in dieser Kaserne aktuell etwas gebaut wird.
	 * @return <code>true</code>, falls etwas gebaut wird
	 */
	public boolean isBuilding()
	{
		if(getQueueEntries().length > 0)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Fuegt den angegebenen Einheitentyp in der Angegebenen Menge der Produktion hinzu.
	 * @param unittype Der Einheitentyp
	 * @param newcount Die Menge
	 */
	public void addEntry(UnitType unittype, int newcount)
	{
		boolean found = false;
		for( KaserneEntry entry : getQueueEntries())
		{
			if(entry.getUnitId() == unittype.getId() && entry.getRemaining() == unittype.getDauer())
			{
				entry.setCount(entry.getCount() + newcount);
				found = true;
				break;
			}
		}
		
		if(!found)
		{
			KaserneEntry newEntry = new KaserneEntry(id, unittype.getId());
			newEntry.setRemaining(unittype.getDauer());
			newEntry.setCount(newcount);
			ContextMap.getContext().getDB().save(newEntry);
		}
	}
}
