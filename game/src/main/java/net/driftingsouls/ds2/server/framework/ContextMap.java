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

/**
 * Enthaelt alle im Moment aktiven Kontext-Objekte. Ueber diese Klasse kann
 * zudem das jeweils zum Thread gehoerende Kontext-Objekt ermittelt oder gesetzt werden.
 * 
 * @author Christopher Jung
 *
 */
public class ContextMap {
	private static ThreadLocal<Context> context = new ThreadLocal<>() {
		// EMPTY
	};
	
	/**
	 * Liefert das zum aktuellen Thread gehoerende Kontextobjekt oder null,
	 * falls keines vorhanden ist.
	 * @return Das Contextobjekt oder null
	 */
	public static Context getContext() {
		return context.get();
	}

	/**
	 * Setzt das Kontextobjekt fuer den aktuellen Thread.
	 * @param context Das Kontextobjekt
	 */
	public static void addContext(Context context) {
		ContextMap.context.set(context);
	}

	/**
	 * Entfernt das Kontextobjekt aus dem aktuellen Thread.
	 *
	 */
	protected static void removeContext() {
		context.remove();
	}
}
