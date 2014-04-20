package net.driftingsouls.ds2.server.framework;

/**
 * Interface fuer Klassen zum Pruefen von Berechtigungen.
 * @author Christopher Jung
 *
 */
public interface PermissionResolver
{
	/**
	 * Gibt zurueck, ob der momentan aktive Benutzer die entsprechende Berechtigung besitzt.
	 * @param permission Die Beschreibung der benoetigten Berechtigung
	 * @return <code>true</code> falls der Benutzer die Berechtigung hat.
	 */
	public boolean hasPermission(PermissionDescriptor permission);
}
