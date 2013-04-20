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

/**
 * Die unterschiedlichen Schiffsklassen in DS.
 * @author Christopher Jung
 *
 */
public enum ShipClasses {
	/**
	 * Die Schiffsklasse "Unbekannt". ID 0.
	 */
	UNBEKANNT("Unbekannt", "Unbekannt", true),
	/**
	 * Die Schiffsklasse "Transporter". ID 1.
	 */
	TRANSPORTER("Transporter", "Transporter", true),
	/**
	 * Die Schiffsklasse "Zerstoerer". ID 2.
	 */
	ZERSTOERER("Zerstörer", "Zerstörer", true),
	/**
	 * Die Schiffsklasse "Tanker". ID 3.
	 */
	TANKER("Tanker", "Tanker", true),
	/**
	 * Die Schiffsklasse "Juggernauten". ID 4.
	 */
	JUGGERNAUT("Juggernaut","Juggernauten", true),
	/**
	 * Die Schiffsklasse "Korvetten". ID 5.
	 */
	KORVETTE("Korvette", "Korvetten", true),
	/**
	 * Die Schiffsklasse "Kreuzer". ID 6.
	 */
	KREUZER("Kreuzer", "Kreuzer", true),
	/**
	 * Die Schiffsklasse "Schwere Kreuzer". ID 7.
	 */
	SCHWERER_KREUZER("Schwerer Kreuzer", "Schwere Kreuzer", true),
	/**
	 * Die Schiffsklasse "Stationen". ID 8.
	 */
	STATION("Station", "Stationen", true),
	/**
	 * Die Schiffsklasse "Jaeger". ID 9.
	 */
	JAEGER("Jäger", "Jäger", true),
	/**
	 * Die Schiffsklasse "Geschuetze". ID 10.
	 */
	GESCHUETZ("Geschütz","Geschütze", false),
	/**
	 * Die Schiffsklasse "Forschungskreuzer". ID 11.
	 */
	FORSCHUNGSKREUZER("Forschungskreuzer", "Forschungskreuzer", true),
	/**
	 * Die Schiffsklasse "Container". ID 12.
	 */
	CONTAINER("Container", "Container", true),
	/**
	 * Die Schiffsklasse "AWACs". ID 13.
	 */
	AWACS("AWAC", "AWACs", true),
	/**
	 * Die Schiffsklasse "Schrotthaufen". ID 14.
	 */
	SCHROTT("Schrott", "Schrotthaufen", true),
	/**
	 * Die Schiffsklasse "Traeger". ID 15.
	 */
	TRAEGER("Träger", "Träger", true),
	/**
	 * Die Schiffsklasse "Kommandoschiffe". ID 16.
	 */
	KOMMANDOSCHIFF("Kommandoschiff", "Kommandoschiffe", true),
	/**
	 * Die Schiffsklasse "Bomber". ID 17.
	 */
	BOMBER("Bomber", "Bomber", true),
	/**
	 * Die Schiffsklasse "Rettungskapseln". ID 18.
	 */
	RETTUNGSKAPSEL("Rettungskapsel", "Rettungskapseln", true),
	/**
	 * Die Schiffsklasse "" (nichts). ID 19.
	 */
	EMTPY("", "", true),
	/**
	 * Die Schiffsklasse "Felsbrocken". ID 20.
	 */
	FELSBROCKEN("Felsbrocken", "Felsbrocken", true);

	private String singular;
	private String plural;
	private final boolean kaperbar;
	
	private ShipClasses(String singular, String plural, boolean kaperbar) {
		this.singular = singular;
		this.plural = plural;
		this.kaperbar = kaperbar;
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
}
