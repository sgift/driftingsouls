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
	UNBEKANNT("Unbekannt"),						// 0
	TRANSPORTER("Transporter"),					// 1
	ZERSTOERER("Zerst&ouml;rer"),				// 2
	TANKER("Tanker"),							// 3
	JUGGERNAUT("Juggernaut","Juggernauten"),	// 4
	KORVETTE("Korvette", "Korvetten"),			// 5
	KREUZER("Kreuzer", "Kreuzer"),				// 6
	SCHWERER_KREUZER("Schwerer Kreuzer", "Schwere Kreuzer"),	// 7
	STATION("Station", "Stationen"),			// 8
	JAEGER("J&auml;ger"),						// 9
	GESCHUETZ("Gesch&uuml;tz","Gesch&uuml;tze"),				// 10
	FORSCHUNGSKREUZER("Forschungskreuzer"),		// 11
	CONTAINER("Container"),						// 12
	AWACS("AWAC", "AWACs"),						// 13
	SCHROTT("Schrott", "Schrotthaufen"),		// 14
	TRAEGER("Tr&auml;ger"),						// 15
	KOMMANDOSCHIFF("Kommandoschiff", "Kommandoschiffe"),		// 16
	BOMBER("Bomber"),							// 17
	RETTUNGSKAPSEL("Rettungskapsel", "Rettungskapseln"),		// 18
	EMTPY(""),									// 19
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
