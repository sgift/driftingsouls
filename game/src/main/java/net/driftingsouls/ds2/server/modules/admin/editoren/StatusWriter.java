package net.driftingsouls.ds2.server.modules.admin.editoren;

/**
 * Interface fuer Status-Logger.
 */
public interface StatusWriter
{
	/**
	 * Loggt den angebenenen Statustext.
	 * @param text Der Text
	 * @return Die Instanz
	 */
    StatusWriter append(String text);
}
