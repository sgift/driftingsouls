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
package net.driftingsouls.ds2.server.framework.db.batch;

/**
 * Eine Arbeitsaufgabe, die zudem am Ende jedes Schritts komplett aus dem
 * Speicher entfernt werden kann. Dies bedeutet, dass zwischen den einzelnen
 * Schritten jedoch <b>keine</b> Entities hin- und hergereicht werden koennen.
 * @author Christopher Jung
 * @param <T> Der Typ
 *
 */
public abstract class EvictableUnitOfWork<T> extends UnitOfWork<T>
{
	/**
	 * Konstruktor.
	 * @param name Der Name der Arbeitsaufgabe
	 */
	public EvictableUnitOfWork(String name)
	{
		super(name);
	}

	@Override
	public void onFlushed()
	{
		super.onFlushed();
		
		getDB().clear();
	}
	
}
