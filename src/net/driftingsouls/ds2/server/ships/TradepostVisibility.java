package net.driftingsouls.ds2.server.ships;

/**
 * Zugriffsbeschraenkungen bei Handelsposten.
 * @author christopherjung
 *
 */
public enum TradepostVisibility
{
	/**
	 * Alle Spieler.
	 */
	ALL("Allen zugänglich"),
	/**
	 * Nur neutrale und befreundete Spieler.
	 */
	NEUTRAL_AND_FRIENDS("Feinde ausnehmen"),
	/**
	 * Nur befreundete Spieler.
	 */
	FRIENDS("Auf Freunde begrenzen"),
	/**
	 * Nur eigene Allianz.
	 */
	ALLY("Auf die Allianz begrenzen"),
	/**
	 * Niemand anderes.
	 */
	NONE("Niemandem zugänglich");
	
	private final String label;
	
	TradepostVisibility(String label)
	{
		this.label = label;
	}
	
	/**
	 * Gibt das Anzeigelabel an der Oberflaeche zurueck.
	 * @return Das Label
	 */
	public String getLabel()
	{
		return this.label;
	}
}
