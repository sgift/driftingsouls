package net.driftingsouls.ds2.server.framework;

/**
 * Interface zur Definition von Konfigurationseinstellungen.
 */
public interface ConfigValueDescriptor<T>
{
	/**
	 * Gibt den Namen der Konfigurationseinstellung zurueck (Schluessel).
	 * @return Der name
	 */
	public String getName();

	/**
	 * Gibt die Beschreibung der Konfigurationseinstellung zurueck.
	 * @return Der Beschreibungstext
	 */
	public String getDescription();

	/**
	 * Gibt den Standardwert fuer die Konfigurationseinstellung zurueck.
	 * @return Der Standardwert
	 */
	public String getDefaultValue();

	/**
	 * Gibt den Datentyp der Konfigurationseinstellung zurueck.
	 * @return Der Datentyp
	 */
	public Class<T> getType();
}
