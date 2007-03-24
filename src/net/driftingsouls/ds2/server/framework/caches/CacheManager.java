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
package net.driftingsouls.ds2.server.framework.caches;

import java.util.HashSet;
import java.util.Set;

/**
 * Verwaltungsklasse fuer verwaltbare Caches ({@link ControllableCache}).
 * Ermoeglicht das Anwenden von Aktionen auf alle Caches.
 * 
 * @author Christopher Jung
 *
 */
public class CacheManager {
	private static CacheManager instance = null;
	
	private Set<ControllableCache> caches = new HashSet<ControllableCache>();
	
	private CacheManager() {
		// EMPTY
	}
	
	/**
	 * Gibt eine Instanz der Cacheverwaltung zurueck
	 * @return Eine Instanz der Cacheverwaltung
	 */
	public static synchronized CacheManager getInstance() {
		if( instance == null ) {
			instance = new CacheManager();
		}
		return instance;
	}
	
	/**
	 * Registriert einen Cache. Wenn der Cache bereits registriert ist,
	 * wird der Cache nicht erneut registriert
	 * @param cache Der zu registrierende Cache
	 */
	public void registerCache(ControllableCache cache) {
		caches.add(cache);
	}
	
	/**
	 * Leert alle registrierten Caches
	 *
	 */
	public void clearCaches() {
		for( ControllableCache cache : caches ) {
			cache.clear();
		}
	}
}
