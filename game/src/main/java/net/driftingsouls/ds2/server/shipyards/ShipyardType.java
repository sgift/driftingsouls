package net.driftingsouls.ds2.server.shipyards;

/**
 * Der Typ einer Werft. Der Typ steuert verschiedene Verhaltensweisen
 * bzw die Verfuegbarkeit von Aktionen.
 */
public enum ShipyardType
{
	/**
	 * Eine Werft auf einem Schiff.
	 */
	SHIP,
	/**
	 * Eine auf einer Basis beheimatete Werft.
	 */
	BASE,
	/**
	 * Eine Einwegwerft. Einwegwerften werden
	 * bei der Fertigstellung des ersten Jobs
	 * in ihrer Warteschlange zerstoert.
	 */
	ONE_WAY
}
