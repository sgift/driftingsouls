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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.caches.CacheManager;
import net.driftingsouls.ds2.server.framework.caches.ControllableCache;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

/**
 * <h1>Repraesentiert eine Forschung in DS</h1>
 * Hinweis zu den Forschungs-IDs:<br>
 * Eine normale Forschung hat eine ID ab 1<br>
 * "Keine Forschung"/"Keine Vorbedingung" hat die ID 0<br>
 * "Nie erforschbar" hat die ID -1
 * @author Christopher Jung
 *
 */
@Entity
@Table(name="forschungen")
@Immutable
public class Forschung {
	/**
	 * Beim Erforschen einer Forschung mit dieser Technologie, verliert
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
				org.hibernate.Session db = ContextMap.getContext().getDB();
				
				Forschung res = (Forschung)db.get(Forschung.class, fid);
				if( res == null ) {
					return null;
				}	
				
				cache.put(fid, res);
				
				return res;
			}
		}
		
		return cache.get(fid);
	}
	
	/**
	 * Leert den Forschungscache
	 *
	 */
	public static void clearCache() {
		synchronized(cache) {
			cache.clear();
		}
	}
	
	@Id @GeneratedValue
	private int id;
	private String name;
	private int req1;
	private int req2;
	private int req3;
	private int time;
	@Type(type="cargo")
	private Cargo costs;
	@Column(name="descrip")
	private String description;
	private int race;
	private int visibility;
	private String flags;
	
	/**
	 * Konstruktor
	 *
	 */
	public Forschung() {
		// EMPTY
	}
	
	/**
	 * Gibt die ID der Forschung zurueck
	 * @return die ID
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gibt den Namen der Forschung zurueck
	 * @return Der Name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Gibt die ID einer der fuer diese Forschung benoetigten Forschungen zurueck
	 * @param number Die Nummer der benoetigten Forschung (1-3)
	 * @return Die ID oder 0
	 */
	public int getRequiredResearch( int number ) {
		switch(number) {
		case 1:
			return this.req1;
		case 2:
			return this.req2;
		case 3:
			return this.req3;
		}
		throw new RuntimeException("Ungueltiger Forschungsindex '"+number+"'");
	}
	
	/**
	 * Gibt die Forschungsdauer in Ticks zurueck
	 * @return Die Forschungsdauer
	 */
	public int getTime() {
		return this.time;
	}
	
	/**
	 * Gibt die Forschungskosten als nicht modifizierbarer Cargo zurueck
	 * @return Die Forschungskosten
	 */
	public Cargo getCosts() {
		return this.costs;
	}
	
	/**
	 * Gibt die Beschreibung der Forschung zurueck
	 * @return Die Beschreibung
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gibt die ID der Rasse zurueck, der die Forschung zugeordnet ist
	 * @return Die ID der Rasse
	 */
	public int getRace() {
		return this.race;
	}
	
	/**
	 * Prueft, ob die Forschung allgemein sichtbar ist oder erst sichtbar wird,
	 * wenn alle benoetigten Forschungen erforscht sind
	 * @return <code>true</code>, falls die Forschung allgemein sichtbar ist
	 */
	public boolean isVisibile() {
		return ( this.visibility > 0 ? true : false );
	}
	
	/**
	 * Prueft, ob die Forschung ein bestimmtes Flag hat
	 * @param flag Das Flag
	 * @return <code>true</code>, falls die Forschung das Flag besitzt
	 */
	public boolean hasFlag( String flag ) {
		return this.flags.indexOf(flag) > -1;
	}
}
