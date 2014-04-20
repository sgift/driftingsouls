package net.driftingsouls.ds2.server.config.items;

/**
 * Enthaelt die moeglichen Qualitaetsstufen eines Items.
 * @author Christopher Jung
 *
 */
public enum Quality
{
	/**
	 * Gewoehnliches Item.
	 */
	COMMON(0,""),
	/**
	 * Seltenes Item.
	 */
	RARE(1,"#3CB371"),
	/**
	 * Sehr seltenes Item.
	 */
	ULTRA_RARE(2,"#BA55D3"),
	/**
	 * Episches (noch selteners) Item.
	 */
	EPIC(3,"#FF8C00"),
	/**
	 * Artefakt (am seltensten) oder Admin-Item.
	 */
	ARTIFACT(4,"#DC143C");

	private int id = 0;
	private String color = null;

	Quality(int id, String color) {
		this.id = id;
		this.color = color;
	}

	/**
	 * Gibt die ID der Qualitaetsstufe zurueck.
	 * Niederige Qualitaetsstufen haben eine niederige ID.
	 * Hohe eine hohe ID.
	 * @return die ID
	 */
	public int id() {
		return this.id;
	}

	/**
	 * <p>Gibt den Farbcode in Hex-Zahlen zurueck, welcher mit dieser Qualitaetsstufe
	 * assoziiert ist.</p>
	 * <p>Items sollten grundsaetzlich im Farbcode ihrer Farbstufe angezeigt werden.</p>
	 *
	 * @return Der Farbcode als Hexzahl
	 */
	public String color() {
		return this.color;
	}

	/**
	 * Konvertiert ein String in einen Qualitaetswert. Sollte kein passender
	 * Qualitaetswert existieren wird <code>COMMON</code> zurueckgegeben
	 * @param quality Der String, welcher den Qualitaetswert als Text enthaelt.
	 * @return Der Qualitaetswert
	 */
	public static Quality fromString(String quality) {
		if( quality == null ) {
			return COMMON;
		}
		if( quality.equalsIgnoreCase("common") ) {
			return COMMON;
		}
		if( quality.equalsIgnoreCase("rare") ) {
			return RARE;
		}
		if( quality.equalsIgnoreCase("ultra-rare") || quality.equalsIgnoreCase("urare") ) {
			return ULTRA_RARE;
		}
		if( quality.equalsIgnoreCase("epic") ) {
			return EPIC;
		}
		if( quality.equalsIgnoreCase("artifact") ) {
			return ARTIFACT;
		}
		return COMMON;
	}

	/**
	 * Gibt die Qualitaet als String zurueck.
	 * @return Die Qualitaet als String
	 */
	@Override
	public String toString() {
		if( id() == 0) {
			return "common";
		}
		else if ( id() == 1) {
			return "rare";
		}
		else if ( id() == 2) {
			return "ultra-rare";
		}
		else if ( id() == 3) {
			return "epic";
		}
		else if ( id() == 4) {
			return "artifact";
		}
		return "common";
	}
}
