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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Liste aller ausbildbaren Offizierstypen
 * @author Christopher Jung
 *
 */
public class Offiziere {
	/**
	 * Der maximale Rang, den ein Offizier durch Erfahrung erlangen kann
	 */
	public static final int MAX_RANG = 6;
	
	/**
	 * Die Liste der Offizierstypen
	 */
	public static final Map<Integer,SQLResultRow> LIST;
	
	static {
		// TODO: In XML auslagern...
		// TODO: ...eine richtige Klasse statt SQLResultRow waere auch nicht schlecht...
		Map<Integer,SQLResultRow> liste = new LinkedHashMap<Integer,SQLResultRow>();
		
		SQLResultRow offi = new SQLResultRow();
		offi.put("id", 1);
		offi.put("name", "Ingenieur");
		offi.put("ing", 25);
		offi.put("waf", 20);
		offi.put("nav", 10);
		offi.put("sec", 5);
		offi.put("com", 5);
		offi.put("specials", new int[] {1,2,3,4,5,6});
		liste.put(1, offi);
		
		offi = new SQLResultRow();
		offi.put("id", 2);
		offi.put("name", "Navigator");
		offi.put("ing", 5);
		offi.put("waf", 10);
		offi.put("nav", 30);
		offi.put("sec", 5);
		offi.put("com", 10);
		offi.put("specials", new int[] {1,2,3,4,5,6});
		liste.put(2, offi);
		
		offi = new SQLResultRow();
		offi.put("id", 3);
		offi.put("name", "Sicherheitsexperte");
		offi.put("ing", 10);
		offi.put("waf", 25);
		offi.put("nav", 5);
		offi.put("sec", 35);
		offi.put("com", 5);
		offi.put("specials", new int[] {1,2,3,4,5,6});
		liste.put(3, offi);

		offi = new SQLResultRow();
		offi.put("id", 4);
		offi.put("name", "Captain");
		offi.put("ing", 10);
		offi.put("waf", 10);
		offi.put("nav", 15);
		offi.put("sec", 5);
		offi.put("com", 35);
		offi.put("specials", new int[] {1,2,3,4,5,6});
		liste.put(4, offi);
		
		LIST = Collections.unmodifiableMap(liste);
	}
}
