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
package net.driftingsouls.ds2.server.config;

import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.util.Iterator;

/**
 * Repraesentiert die Rassen-Liste in DS.
 *
 * @author Christopher Jung
 */
public class Rassen implements Iterable<Rasse>
{
	private static Rassen rassenList = new Rassen();

	private Rassen()
	{
		// EMPTY
	}

	/**
	 * Liefert eine Instanz der Rassenliste.
	 *
	 * @return Eine Instanz der Rassenliste
	 */
	public static Rassen get()
	{
		return rassenList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<Rasse> iterator()
	{
		return ContextMap.getContext().getDB().createCriteria(Rasse.class).list().iterator();
	}

	/**
	 * Liefert die zu einer ID gehoerende Rasse.
	 *
	 * @param id Die ID der Rasse
	 * @return Die zur ID gehoerende Rasse
	 */
	public Rasse rasse(int id)
	{
		return (Rasse) ContextMap.getContext().getDB().get(Rasse.class, id);
	}
}
