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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.units.UnitType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Repraesentiert eine Kaserne auf einer Basis in DS.
 *
 */
@Entity
@Table(name="kaserne")
public class Kaserne {
	@Id @GeneratedValue
	private int id;
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="col", nullable=false)
	@ForeignKey(name="kaserne_fk_bases")
	private Base base;

	@OneToMany(mappedBy="kaserne")
	private List<KaserneEntry> entries;

	/**
	 * Konstruktor.
	 *
	 */
	protected Kaserne()
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
		this.entries = new ArrayList<>();
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
	public List<KaserneEntry> getQueueEntries()
	{
		return new ArrayList<>(this.entries);
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

		for(KaserneEntry entry : this.entries)
		{
			db.delete(entry);
		}

		this.entries.clear();

		db.delete(this);
	}

	/**
	 * Gibt zurueck, ob in dieser Kaserne aktuell etwas gebaut wird.
	 * @return <code>true</code>, falls etwas gebaut wird
	 */
	public boolean isBuilding()
	{
		return !this.entries.isEmpty();
	}

	/**
	 * Fuegt den angegebenen Einheitentyp in der Angegebenen Menge der Produktion hinzu.
	 * @param unittype Der Einheitentyp
	 * @param newcount Die Menge
	 */
	public void addEntry(UnitType unittype, int newcount)
	{
		boolean found = false;
		for( KaserneEntry entry : this.entries)
		{
			if(entry.getUnit().getId() == unittype.getId() && entry.getRemaining() == unittype.getDauer())
			{
				entry.setCount(entry.getCount() + newcount);
				found = true;
				break;
			}
		}

		if(!found)
		{
			KaserneEntry newEntry = new KaserneEntry(this, unittype);
			newEntry.setRemaining(unittype.getDauer());
			newEntry.setCount(newcount);
			ContextMap.getContext().getDB().save(newEntry);

			this.entries.add(newEntry);
		}
	}
}
