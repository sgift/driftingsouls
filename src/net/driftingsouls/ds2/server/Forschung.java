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
package net.driftingsouls.ds2.server;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.caches.CacheManager;
import net.driftingsouls.ds2.server.framework.caches.ControllableCache;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * <h1>Repraesentiert eine Forschung in DS.</h1>
 * Hinweis zu den Forschungs-IDs:<br>
 * Eine normale Forschung hat eine ID ab 1<br>
 * "Keine Forschung"/"Keine Vorbedingung" hat die ID 0<br>
 * "Nie erforschbar" hat die ID -1
 * @author Christopher Jung
 *
 */
public class Forschung {
	/**
	 * Beim Erforschen einer Forschung mit dieser Technologie, verliert.
	 * der Spieler den Noob-Status
	 */
	public static final String FLAG_DROP_NOOB_PROTECTION = "drop_noob";
	
	private static final Map<Integer,Forschung> cache = new HashMap<Integer,Forschung>();
	
	static {
		CacheManager.getInstance().registerCache(
			new ControllableCache() {
				public void clear() {
					Forschung.clearCache();
				}
			}
		);
	}
	
	/**
	 * Gibt ein Forschungsobjekt fuer die angegebene Forschungs-ID zurueck.
	 * Sollte keine solche Forschung existieren, so wird <code>null</code> zurueckgegeben
	 * @param fid Die Forschungs-ID
	 * @return Das zur ID gehoerende Forschungsobjekt oder <code>null</code>
	 */
	public static Forschung getInstance( int fid ) {
		synchronized(cache) {
			if( !cache.containsKey(fid) ) {
				Database db = ContextMap.getContext().getDatabase();
				
				SQLResultRow data = db.first("SELECT * FROM forschungen WHERE id='",fid,"'");
				if( data.isEmpty() ) {
					return null;
				}	
				
				cache.put(fid, new Forschung(data));
			}
		}
		
		return cache.get(fid);
	}
	
	/**
	 * Leert den Forschungscache.
	 *
	 */
	public static void clearCache() {
		synchronized(cache) {
			cache.clear();
		}
	}
	
	/**
	 * Gibt eine geordnete Map mit Forschungsobjekten zurueck. Die Forschungsobjekte
	 * werden mittels der angegebenen Parameter fuer die <code>WHERE</code> und <code>ORDER BY</code>
	 * Abschnitte einer SQL-Query ueber die Forschungs-Tabelle generiert.
	 * @param where Die Parameter fuer den <code>WHERE</code>-Abschnitt
	 * @param order Die Parameter fuer den <code>ORDER BY</code>-Abschnitt
	 * @return Die Map mit den Forschungsobjekten
	 */
	public static Map<Integer,Forschung> getSpecial(String where, String order) {
		Database db = ContextMap.getContext().getDatabase();
		
		if( where.length() != 0 ) {
			where = "WHERE "+where;
		}
		
		if( order.length() != 0 ) {
			order = "ORDER BY "+order;
		}
		
		Map<Integer,Forschung> list = new LinkedHashMap<Integer,Forschung>();
		
		SQLQuery data = db.query("SELECT * FROM forschungen "+where+" "+order);
		while( data.next() ) {
			synchronized(cache) {
				if( !cache.containsKey(data.getInt("id")) ) {
					cache.put(data.getInt("id"), new Forschung(data.getRow()));
				}
			}
			list.put(data.getInt("id"), cache.get(data.getInt("id")));
		}
		data.free();
		
		return list;
	}
	
	private SQLResultRow data;
	
	private Forschung(SQLResultRow data) {
		this.data = data;
	}
	
	/**
	 * Gibt die ID der Forschung zurueck.
	 * @return die ID
	 */
	public int getID() {
		return data.getInt("id");
	}
	
	/**
	 * Gibt den Namen der Forschung zurueck.
	 * @return Der Name
	 */
	public String getName() {
		return data.getString("name");
	}
	
	/**
	 * Gibt die ID einer der fuer diese Forschung benoetigten Forschungen zurueck.
	 * @param number Die Nummer der benoetigten Forschung (1-3)
	 * @return Die ID oder 0
	 */
	public int getRequiredResearch( int number ) {
		return data.getInt("req"+number);
	}
	
	/**
	 * Gibt die Forschungsdauer in Ticks zurueck.
	 * @return Die Forschungsdauer
	 */
	public int getTime() {
		return data.getInt("time");
	}
	
	/**
	 * Gibt die Forschungskosten als Cargo-String zurueck.
	 * @return Die Forschungskosten
	 */
	public String getCosts() {
		return data.getString("costs");
	}
	
	/**
	 * Gibt die Beschreibung der Forschung zurueck.
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return data.getString("descrip");
	}
	
	/**
	 * Gibt die ID der Rasse zurueck, der die Forschung zugeordnet ist.
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		return data.getInt("race");
	}
	
	/**
	 * Prueft, ob die Forschung allgemein sichtbar ist oder erst sichtbar wird,
	 * wenn alle benoetigten Forschungen erforscht sind.
	 * @return <code>true</code>, falls die Forschung allgemein sichtbar ist
	 */
	public boolean isVisibile() {
		return ( data.getInt("visibility") > 0 ? true : false );
	}
	
	/**
	 * Prueft, ob die Forschung ein bestimmtes Flag hat.
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Forschung das Flag besitzt
	 */
	public boolean hasFlag( String flag ) {
		return data.getString("flags").indexOf(flag) > -1;
	}
}
