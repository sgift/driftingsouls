package net.driftingsouls.ds2.server.werften;

/**
 * Der Typ einer Werft. Der Typ steuert verschiedene Verhaltensweisen
 * bzw die Verfuegbarkeit von Aktionen.
 */
public enum WerftTyp
{
	/**
	 * Eine Werft auf einem Schiff.
	 */
	SCHIFF,
	/**
	 * Eine auf einer Basis beheimatete Werft.
	 */
	BASIS,
	/**
	 * Eine Einwegwerft. Einwegwerften werden
	 * bei der Fertigstellung des ersten Jobs
	 * in ihrer Warteschlange zerstoert.
	 */
	EINWEG
}
