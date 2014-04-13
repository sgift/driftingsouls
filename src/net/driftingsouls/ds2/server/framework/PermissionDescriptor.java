package net.driftingsouls.ds2.server.framework;

/**
 * Beschreibung einer Permission, die geprueft werden kann.
 */
public interface PermissionDescriptor
{
	/**
	 * Die Kategorie in der die Aktion ausgefuehrt werden soll.
	 * @return Der Name der Kategorie
	 */
	public String getCategory();

	/**
	 * Die Aktion, die ausgefuehrt werden soll
	 * @return Der Name der Aktion
	 */
	public String getAction();
}
