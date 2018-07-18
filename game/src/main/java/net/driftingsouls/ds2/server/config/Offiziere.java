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

import net.driftingsouls.ds2.server.entities.Offizier;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liste aller ausbildbaren Offizierstypen.
 * @author Christopher Jung
 *
 */
public class Offiziere {
	/**
	 * Beschreibt eine konkrete (initiale) Ausbildung eines Offiziers bzw
	 * den aus der Ausbildung resultierenden Offizier.
	 */
	public static class Offiziersausbildung {
		private int id;
		private String name;
		private Map<Offizier.Ability,Integer> abilities;
		private int[] specials;

		/**
		 * Konstruktor.
		 * @param id Die ID der Ausbildung
		 * @param name Der Name der Ausbildung
		 */
		public Offiziersausbildung(int id, String name)
		{
			this.id = id;
			this.name = name;
			this.specials = new int[] {1,2,3,4,5,6};
			this.abilities = new HashMap<>();
			for (Offizier.Ability ability : Offizier.Ability.values())
			{
				this.abilities.put(ability, 0);
			}

		}

		/**
		 * Gibt die ID der Ausbildung zurueck.
		 * @return Die ID
		 */
		public int getId()
		{
			return id;
		}

		/**
		 * Gibt den Namen der Ausbildung zurueck.
		 * @return Der Name
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * Gibt den Wert der angegebenen Faehigkeit des Offiziers nach der
		 * Ausbildung zurueck.
		 * @param ability Die Faehigkeit
		 * @return Der Wert
		 */
		public int getAbility(Offizier.Ability ability)
		{
			return this.abilities.get(ability);
		}

		/**
		 * Setzt den Wert der angegebenen Faehigkeit des Offiziers nach der
		 * Ausbildung.
		 * @param ability Die Faehigkeit
		 * @param wert Der Wert
		 */
		public void setAbility(Offizier.Ability ability, int wert)
		{
			this.abilities.put(ability, wert);
		}

		/**
		 * Gibt alle durch die Ausbildung moeglichen Spezialfaehigkeiten des
		 * Offiziers zurueck.
		 * @return Die Faehigkeiten
		 */
		public int[] getSpecials()
		{
			return specials.clone();
		}

		/**
		 * Setzt alle durch die Ausbildung moeglichen Spezialfaehigkeiten des
		 * Offiziers.
		 * @param specials Die Faehigkeiten
		 */
		public void setSpecials(int[] specials)
		{
			if( specials == null )
			{
				throw new NullPointerException();
			}
			this.specials = specials.clone();
		}
	}

	/**
	 * Der maximale Rang, den ein Offizier durch Erfahrung erlangen kann.
	 */
	public static final int MAX_RANG = 6;
	
	/**
	 * Die Liste der Offizierstypen.
	 */
	public static final Map<Integer,Offiziersausbildung> LIST;
	
	static {
		// TODO: In XML auslagern...
		Map<Integer,Offiziersausbildung> liste = new LinkedHashMap<>();

		Offiziersausbildung offi = new Offiziersausbildung(1, "Ingenieur");
		offi.setAbility(Offizier.Ability.ING, 25);
		offi.setAbility(Offizier.Ability.WAF, 20);
		offi.setAbility(Offizier.Ability.NAV, 10);
		offi.setAbility(Offizier.Ability.SEC, 5);
		offi.setAbility(Offizier.Ability.COM, 5);
		liste.put(1, offi);
		
		offi = new Offiziersausbildung(2, "Navigator");
		offi.setAbility(Offizier.Ability.ING, 5);
		offi.setAbility(Offizier.Ability.WAF, 10);
		offi.setAbility(Offizier.Ability.NAV, 30);
		offi.setAbility(Offizier.Ability.SEC, 5);
		offi.setAbility(Offizier.Ability.COM, 10);
		liste.put(2, offi);
		
		offi = new Offiziersausbildung(3, "Sicherheitsexperte");
		offi.setAbility(Offizier.Ability.ING, 10);
		offi.setAbility(Offizier.Ability.WAF, 25);
		offi.setAbility(Offizier.Ability.NAV, 5);
		offi.setAbility(Offizier.Ability.SEC, 35);
		offi.setAbility(Offizier.Ability.COM, 5);
		liste.put(3, offi);

		offi = new Offiziersausbildung(4, "Captain");
		offi.setAbility(Offizier.Ability.ING, 10);
		offi.setAbility(Offizier.Ability.WAF, 10);
		offi.setAbility(Offizier.Ability.NAV, 15);
		offi.setAbility(Offizier.Ability.SEC, 5);
		offi.setAbility(Offizier.Ability.COM, 35);
		liste.put(4, offi);
		
		LIST = Collections.unmodifiableMap(liste);
	}
}
