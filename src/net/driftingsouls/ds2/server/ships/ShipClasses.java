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
 * Die unterschiedlichen Schiffsklassen in DS
 * @author Christopher Jung
 *
 */
public enum ShipClasses {
	/**
	 * Die Schiffsklasse "Unbekannt"
	 */
	UNBEKANNT("Unbekannt"),						// 0
	/**
	 * Die Schiffsklasse "Transporter"
	 */
	TRANSPORTER("Transporter"),					// 1
	/**
	 * Die Schiffsklasse "Zerstoerer"
	 */
	ZERSTOERER("Zerst&ouml;rer"),				// 2
	/**
	 * Die Schiffsklasse "Tanker"
	 */
	TANKER("Tanker"),							// 3
	/**
	 * Die Schiffsklasse "Juggernauten"
	 */
	JUGGERNAUT("Juggernaut","Juggernauten"),	// 4
	/**
	 * Die Schiffsklasse "Korvetten"
	 */
	KORVETTE("Korvette", "Korvetten"),			// 5
	/**
	 * Die Schiffsklasse "Kreuzer"
	 */
	KREUZER("Kreuzer", "Kreuzer"),				// 6
	/**
	 * Die Schiffsklasse "Schwere Kreuzer"
	 */
	SCHWERER_KREUZER("Schwerer Kreuzer", "Schwere Kreuzer"),	// 7
	/**
	 * Die Schiffsklasse "Stationen"
	 */
	STATION("Station", "Stationen"),			// 8
	/**
	 * Die Schiffsklasse "Jaeger"
	 */
	JAEGER("J&auml;ger"),						// 9
	/**
	 * Die Schiffsklasse "Geschuetze"
	 */
	GESCHUETZ("Gesch&uuml;tz","Gesch&uuml;tze"),				// 10
	/**
	 * Die Schiffsklasse "Forschungskreuzer"
	 */
	FORSCHUNGSKREUZER("Forschungskreuzer"),		// 11
	/**
	 * Die Schiffsklasse "Container"
	 */
	CONTAINER("Container"),						// 12
	/**
	 * Die Schiffsklasse "AWACs"
	 */
	AWACS("AWAC", "AWACs"),						// 13
	/**
	 * Die Schiffsklasse "Schrotthaufen"
	 */
	SCHROTT("Schrott", "Schrotthaufen"),		// 14
	/**
	 * Die Schiffsklasse "Traeger"
	 */
	TRAEGER("Tr&auml;ger"),						// 15
	/**
	 * Die Schiffsklasse "Kommandoschiffe"
	 */
	KOMMANDOSCHIFF("Kommandoschiff", "Kommandoschiffe"),		// 16
	/**
	 * Die Schiffsklasse "Bomber"
	 */
	BOMBER("Bomber"),							// 17
	/**
	 * Die Schiffsklasse "Rettungskapseln"
	 */
	RETTUNGSKAPSEL("Rettungskapsel", "Rettungskapseln"),		// 18
	/**
	 * Die Schiffsklasse "" (nichts)
	 */
	EMTPY(""),									// 19
	/**
	 * Die Schiffsklasse "Felsbrocken"
	 */
	FELSBROCKEN("Felsbrocken");					// 20
	
	private String singular;
	private String plural;
	
	private ShipClasses(String singular, String plural) {
		this.singular = singular;
		this.plural = plural;
	}
	
	private ShipClasses(String name) {
		this(name,name);
	}
	
	/**
	 * Gibt die Singularform des Schiffsklassen-Namens zurueck
	 * @return die Singularform
	 */
	public String getSingular() {
		return singular;
	}
	
	/**
	 * Gibt die Pluralform des Schiffsklassen-Namens zurueck
	 * @return die Pluralform
	 */
	public String getPlural() {
		return plural;
	}
}
