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
package net.driftingsouls.ds2.server.cargo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Comparator;

/**
 * Liste von Resourcen aus einem Cargo
 * @author Christopher Jung
 * @see ResourceEntry
 *
 */
public class ResourceList implements Iterable<ResourceEntry>, Iterator<ResourceEntry> {
	/**
	 * Vergleichsklasse fuer Resourcen-IDs
	 * @author Christopher Jung
	 *
	 */
	private class IDComparator implements Comparator<ResourceEntry> {
		private ResourceIDComparator comp;
		IDComparator(boolean descending) {
			this.comp = new ResourceIDComparator(descending);
		}
		
		public int compare(ResourceEntry o1, ResourceEntry o2) {
			return comp.compare(o1.getId(), o2.getId());
		}
	}
	
	/**
	 * Vergleichsklasse fuer Resourcen-IDs. Verglichen wird auf Basis
	 * der vorhandenen Resourcenmenge
	 * @author Christopher Jung
	 *
	 */
	private class CargoComparator implements Comparator<ResourceEntry> {
		private boolean descending;
		
		CargoComparator(boolean descending) {
			this.descending = descending;
		}
		
		public int compare(ResourceEntry o1, ResourceEntry o2) {
			if( o1.getCount1() > o2.getCount1() ) {
				return (descending ? -1 : 1);
			}
			if( o1.getCount1() < o2.getCount1() ) {
				return (descending ? 1 : -11);
			}
			return 0;
		}
	}
	
	private List<ResourceEntry> list = new ArrayList<ResourceEntry>();
	private Iterator<ResourceEntry> iter = null;
	
	protected ResourceList() {
		// EMPTY
	}
	
	/**
	 * Fuegt einen neuen Resourceneintrag zur Resourcenliste hinzu
	 * @param entry Der Resourceneintrag
	 */
	public void addEntry(ResourceEntry entry) {
		list.add(entry);
	}
	
	/**
	 * Sortiert die Liste auf Basis der Resourcen-ID
	 * @param descending Soll die Liste absteigend sortiert werden (<code>true</code>)?
	 */
	public void sortByID( boolean descending ) {
		Collections.sort(list, new IDComparator(descending));
	}

	/**
	 * Sortiert die Liste auf Basis der Cargomenge einer Resource
	 * @param descending Soll die Liste absteigend sortiert werden (<code>true</code>)?
	 */
	public void sortByCargo( boolean descending ) {
		Collections.sort(list, new CargoComparator(descending));
	}
	
	/**
	 * Gibt die Anzahl der Resourceneintraege in der Resourcenliste an
	 * @return Die Anzahl der Resourceneintraege
	 */
	public int size() {
		return list.size();
	}

	public Iterator<ResourceEntry> iterator() {
		iter = list.iterator();
		return this;
	}

	public boolean hasNext() {
		if( iter == null ) {
			iterator();
		}
		return iter.hasNext();
	}

	public ResourceEntry next() {
		return iter.next();
	}

	public void remove() {
		throw new UnsupportedOperationException("Das entfernen von Resourcen-Eintraegen ist nicht moeglich");
	}
}
