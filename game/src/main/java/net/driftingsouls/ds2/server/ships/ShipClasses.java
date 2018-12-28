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
package net.driftingsouls.ds2.server.ships;

import java.util.HashSet;
import java.util.Set;

/**
 * Die unterschiedlichen Schiffsklassen in DS.
 * @author Christopher Jung
 *
 */
public enum ShipClasses {
	/**
	 * Die Schiffsklasse "Unbekannt". ID 0.
	 */
	UNBEKANNT("Unbekannt", "Unbekannt", true, false),
	/**
	 * Die Schiffsklasse "Transporter". ID 1.
	 */
	TRANSPORTER("Transporter", "Transporter", true, false),
	/**
	 * Die Schiffsklasse "Zerstoerer". ID 2.
	 */
	ZERSTOERER("Zerstörer", "Zerstörer", true, false),
	/**
	 * Die Schiffsklasse "Tanker". ID 3.
	 */
	TANKER("Tanker", "Tanker", true, false),
	/**
	 * Die Schiffsklasse "Juggernauten". ID 4.
	 */
	JUGGERNAUT("Juggernaut","Juggernauten", true, false),
	/**
	 * Die Schiffsklasse "Korvetten". ID 5.
	 */
	KORVETTE("Korvette", "Korvetten", true, false),
	/**
	 * Die Schiffsklasse "Kreuzer". ID 6.
	 */
	KREUZER("Kreuzer", "Kreuzer", true, false),
	/**
	 * Die Schiffsklasse "Schwere Kreuzer". ID 7.
	 */
	SCHWERER_KREUZER("Schwerer Kreuzer", "Schwere Kreuzer", true, false),
	/**
	 * Die Schiffsklasse "Stationen". ID 8.
	 */
	STATION("Station", "Stationen", true, false),
	/**
	 * Die Schiffsklasse "Jaeger". ID 9.
	 */
	JAEGER("Jäger", "Jäger", true, false),
	/**
	 * Die Schiffsklasse "Geschuetze". ID 10.
	 */
	GESCHUETZ("Geschütz","Geschütze", false, false),
	/**
	 * Die Schiffsklasse "Forschungskreuzer". ID 11.
	 */
	FORSCHUNGSKREUZER("Forschungskreuzer", "Forschungskreuzer", true, true),
	/**
	 * Die Schiffsklasse "Container". ID 12.
	 */
	CONTAINER("Container", "Container", true, false),
	/**
	 * Die Schiffsklasse "AWACs". ID 13.
	 */
	AWACS("AWAC", "AWACs", true, true),
	/**
	 * Die Schiffsklasse "Schrotthaufen". ID 14.
	 */
	SCHROTT("Schrott", "Schrotthaufen", true, false),
	/**
	 * Die Schiffsklasse "Traeger". ID 15.
	 */
	TRAEGER("Träger", "Träger", true, false),
	/**
	 * Die Schiffsklasse "Kommandoschiffe". ID 16.
	 */
	KOMMANDOSCHIFF("Kommandoschiff", "Kommandoschiffe", true, false),
	/**
	 * Die Schiffsklasse "Bomber". ID 17.
	 */
	BOMBER("Bomber", "Bomber", true, false),
	/**
	 * Die Schiffsklasse "Rettungskapseln". ID 18.
	 */
	RETTUNGSKAPSEL("Rettungskapsel", "Rettungskapseln", true, false),
	/**
	 * Die Schiffsklasse "" (nichts). ID 19.
	 */
	EMTPY("", "", true, false),
	/**
	 * Die Schiffsklasse "Felsbrocken". ID 20.
	 */
	FELSBROCKEN("Felsbrocken", "Felsbrocken", true, false),
	/**
	 * Die Schiffsklasse "Fregatte". ID 21.
	 */
	FREGATTE("Fregatte", "Fregatten", true, false),
	/**
	 * Die Schiffsklasse "Schutzschild". ID 22.
	 */
	SCHUTZSCHILD("Schutzschild", "Schutzschilde", false, false),
	/**
	 * Die Schiffsklasse "Miner". ID 23.
	 */
	MINER("Miner", "Miner", true, false);

	private String singular;
	private String plural;
	private final boolean kaperbar;

	private final boolean darfSchlachtenAnsehen;

	ShipClasses(String singular, String plural, boolean kaperbar, boolean darfSchlachtenAnsehen) {
		this.singular = singular;
		this.plural = plural;
		this.kaperbar = kaperbar;
		this.darfSchlachtenAnsehen = darfSchlachtenAnsehen;
	}

	/**
	 * Gibt zurueck, ob diese Schiffsklasse laufende Schlachten ansehen darf ohne ihnen beitreten zu muessen.
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isDarfSchlachtenAnsehen()
	{
		return darfSchlachtenAnsehen;
	}

	/**
	 * Gibt zurueck, ob der Schiffstyp grundsaetzlich kaperbar oder pluenderbar ist.
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isKaperbar() {
		return this.kaperbar;
	}

	/**
	 * Gibt die Singularform des Schiffsklassen-Namens zurueck.
	 * @return die Singularform
	 */
	public String getSingular() {
		return singular;
	}

	/**
	 * Gibt die Pluralform des Schiffsklassen-Namens zurueck.
	 * @return die Pluralform
	 */
	public String getPlural() {
		return plural;
	}

	/**
	 * Gibt alle Schiffsklassen zurueck, die laufende Schlachten einsehen dürfen.
	 * @return Die Schiffsklassen
	 * @see #isDarfSchlachtenAnsehen()
	 */
	public static Set<ShipClasses> darfSchlachtenAnsehen() {
		Set<ShipClasses> result = new HashSet<>();
		for (ShipClasses shipClasses : values())
		{
			if( shipClasses.isDarfSchlachtenAnsehen() )
			{
				result.add(shipClasses);
			}
		}
		return result;
	}
}
