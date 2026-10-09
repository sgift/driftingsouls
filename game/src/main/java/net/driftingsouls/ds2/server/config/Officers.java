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
public class Officers {
	/**
	 * Beschreibt eine konkrete (initiale) Ausbildung eines Offiziers bzw
	 * den aus der Ausbildung resultierenden Offizier.
	 */
	public static class OfficerTraining {
		private final int id;
		private final String name;
		private final Map<Offizier.Ability,Integer> abilities;
		private int[] specials;

		/**
		 * Konstruktor.
		 * @param id Die ID der Ausbildung
		 * @param name Der Name der Ausbildung
		 */
		public OfficerTraining(int id, String name)
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
		 * @param value Der Wert
		 */
		public void setAbility(Offizier.Ability ability, int value)
		{
			this.abilities.put(ability, value);
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
	public static final int MAX_RANK = 6;
	
	/**
	 * Die Liste der Offizierstypen.
	 */
	public static final Map<Integer,OfficerTraining> LIST;
	
	static {
		// TODO: In XML auslagern...
		Map<Integer,OfficerTraining> list = new LinkedHashMap<>();

		OfficerTraining officerTraining = new OfficerTraining(1, "Ingenieur");
		officerTraining.setAbility(Offizier.Ability.ING, 25);
		officerTraining.setAbility(Offizier.Ability.WAF, 20);
		officerTraining.setAbility(Offizier.Ability.NAV, 10);
		officerTraining.setAbility(Offizier.Ability.SEC, 5);
		officerTraining.setAbility(Offizier.Ability.COM, 5);
		list.put(1, officerTraining);
		
		officerTraining = new OfficerTraining(2, "Navigator");
		officerTraining.setAbility(Offizier.Ability.ING, 5);
		officerTraining.setAbility(Offizier.Ability.WAF, 10);
		officerTraining.setAbility(Offizier.Ability.NAV, 30);
		officerTraining.setAbility(Offizier.Ability.SEC, 5);
		officerTraining.setAbility(Offizier.Ability.COM, 10);
		list.put(2, officerTraining);
		
		officerTraining = new OfficerTraining(3, "Sicherheitsexperte");
		officerTraining.setAbility(Offizier.Ability.ING, 10);
		officerTraining.setAbility(Offizier.Ability.WAF, 25);
		officerTraining.setAbility(Offizier.Ability.NAV, 5);
		officerTraining.setAbility(Offizier.Ability.SEC, 35);
		officerTraining.setAbility(Offizier.Ability.COM, 5);
		list.put(3, officerTraining);

		officerTraining = new OfficerTraining(4, "Captain");
		officerTraining.setAbility(Offizier.Ability.ING, 10);
		officerTraining.setAbility(Offizier.Ability.WAF, 10);
		officerTraining.setAbility(Offizier.Ability.NAV, 15);
		officerTraining.setAbility(Offizier.Ability.SEC, 5);
		officerTraining.setAbility(Offizier.Ability.COM, 35);
		list.put(4, officerTraining);
		
		LIST = Collections.unmodifiableMap(list);
	}
}
