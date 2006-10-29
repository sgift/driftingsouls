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
package net.driftingsouls.ds2.server.framework;

import java.util.Iterator;
import java.util.NoSuchElementException;

import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Ein Iterator, welcher ueber eine SQL-Query iteriert. Die SQL-Query muss gueltige User-Eintraege
 * liefern. Als Feld muss "id" enthalten sein.
 * Jeder Iterationsschritt liefert ein User-Objekt, welches alle von der Query gelieferten Werte (ausgenommen
 * jene, die in der User-Tabelle nicht vorkommen) enthaelt
 * 
 * @author Christopher Jung
 *
 */
public class UserIterator implements Iterable<User>, Iterator<User> {
	private SQLQuery query;
	private SQLResultRow next = null;
	private Context context;
	
	protected UserIterator(Context context, SQLQuery query) {
		this.query = query;
		this.context = context;
	}

	public Iterator<User> iterator() {
		return this;
	}

	public boolean hasNext() {
		if( next == null ) {
			if( !query.next() ) {
				return false;
			}
			next = query.getRow();
		}
		
		return true;
	}

	public User next() {
		if( next == null ) { 
			if( !query.next() ) {
				throw new NoSuchElementException("Keine weiteren User-Objekte vorhanden");
			}
			next = query.getRow();
		}
		SQLResultRow row = next;
		next = null;
		return new User(context, row);
	}

	public void remove() {
		throw new UnsupportedOperationException("Das Entfernen von Benutzern wird nicht unterstuetzt");
	}

	/**
	 * Gibt die vom Iterator belegten Resourcen wieder frei.
	 * Diese Methode sollte auf jeden Fall aufgerufen werden,
	 * wenn der Iterator nicht mehr verwendet wird! 
	 *
	 */
	public void free() {
		query.free();
	}
}
