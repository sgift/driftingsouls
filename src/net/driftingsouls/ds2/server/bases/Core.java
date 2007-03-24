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
package net.driftingsouls.ds2.server.bases;

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.caches.CacheManager;
import net.driftingsouls.ds2.server.framework.caches.ControllableCache;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

//TODO: Warum Verbrauch/Produktion unterscheiden?
/**
 * Basisklasse fuer alle Coretypen
 * @author Christopher Jung
 *
 */
public abstract class Core {
	private static Map<Integer,Core> coreCache = new HashMap<Integer,Core>();
	
	static {
		CacheManager.getInstance().registerCache(
			new ControllableCache() {
				public void clear() {
					Core.clearCache();
				}
			}
		);
	}
	
	static void clearCache() {
		synchronized(coreCache) {
			coreCache.clear();
		}
	}
	
	/**
	 * Gibt eine Instanz der Coreklasse des angegebenen Coretyps zurueck.
	 * Sollte kein passender Coretyp existieren, wird <code>null</code> zurueckgegeben.
	 * 
	 * @param db Eine Datenbankverbindung
	 * @param id Die ID des Coretyps
	 * @return Eine Instanz der zugehoerigen Coreklasse
	 */
	public static synchronized Core getCore(Database db, int id) {
		if( !coreCache.containsKey(id) ) {
			SQLResultRow row = db.first("SELECT * FROM cores WHERE id='",id,"'");
			if( row.isEmpty() ) {
				coreCache.put(id, null);
			}
			else {
				coreCache.put(id, new DefaultCore(row));
			}
		}
		return coreCache.get(id);
	}
	
	/**
	 * Die ID des Coretyps
	 * @return die ID
	 */
	public abstract int getID();
	
	/**
	 * Der Name der Core
	 * @return der Name
	 */
	public abstract String getName();
	
	/**
	 * Gibt den Basis-Typ, in den die Core passt, zurueck
	 * @return der Basistyp
	 * @see Base#getKlasse()
	 */
	public abstract int getAstiType();
	
	/**
	 * Gibt die Baukosten, welche zum errichten der Core notwendig sind, zurueck
	 * @return die Baukosten
	 */
	public abstract Cargo getBuildCosts();
	
	/**
	 * Gibt die Produktion pro Tick der Core zurueck
	 * @return die Produktion pro Tick
	 */
	public abstract Cargo getProduces();
	
	/**
	 * Gibt den Verbrauch pro Tick der Core zurueck
	 * @return der Verbrauch pro Tick
	 */
	public abstract Cargo getConsumes();
	
	/**
	 * Gibt die Anzahl der zum Betrieb der Core notwendigen Arbeiter zurueck
	 * @return die benoetigten Arbeiter
	 */
	public abstract int getArbeiter();
	
	/**
	 * Gibt den Energieverbrauch der Core pro Tick zurueck
	 * @return der Energieverbrauch pro Tick
	 */
	public abstract int getEVerbrauch();
	
	/**
	 * Gibt die Energieproduktion der Core pro Tick zurueck
	 * @return Die Energieproduktion pro Tick
	 */
	public abstract int getEProduktion();
	
	/**
	 * Gibt den durch die Core bereitgestellten Wohnraum zurueck
	 * @return Der Wohnraum
	 */
	public abstract int getBewohner();
	
	/**
	 * Gibt die ID der Forschung zurueck, welche zum errichten der Core benoetigt wird
	 * @return Die ID der benoetigten Forschung
	 */
	public abstract int getTechRequired();
	
	/**
	 * Unbekannt (?????) - Wird aber auch nicht verwendet
	 * @return ????
	 */
	public abstract int getEPS();
}
