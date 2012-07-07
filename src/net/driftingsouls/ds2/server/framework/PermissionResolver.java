package net.driftingsouls.ds2.server.framework;

/**
 * Interface fuer Klassen zum Pruefen von Berechtigungen.
 * @author Christopher Jung
 *
 */
public interface PermissionResolver
{
	/**
	 * Gibt zurueck, ob der momentan aktive Benutzer berechtigt ist,
	 * die angegebene Aktion durchzufuehren.
	 * @param category Die Kategorie in der die Aktion durchgefuehrt wird
	 * @param action Die Aktion selbst
	 * @return <code>true</code> falls der Benutzer die Berechtigung hat.
	 */
	public boolean hasPermission(String category, String action);
}
