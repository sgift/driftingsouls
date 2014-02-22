package net.driftingsouls.ds2.server.framework;

import org.hibernate.Session;

/**
 * Service zum Zugriff auf Konfigurationswerte. Die Konfigurationswerte werden durch {@link net.driftingsouls.ds2.server.framework.ConfigValue}s
 * repraesentiert und besitzen primaer einen Schluessel (key bzw name) und einen Wert in Form eines Strings.
 */
public class ConfigService
{
	/**
	 * Gibt das Konfigurationsobjekt mit dem angegebenen Schluessel zurueck. Die Methode sollte
	 * nur verwendet werden, wenn wirklich das gesamte Konfigurationsobjekt benoetigt wird.
	 * Fuer direkte Zugriffe auf den Wert existiert die Methode {@link #getValue(Class, String)}.
	 * @param key Der Schluessel des Konfigurationsobjekts
	 * @return Das Konfigurationsobjekt
	 * @see #getValue(Class, String)
	 */
	public ConfigValue get(String key)
	{
		Session db = ContextMap.getContext().getDB();
		return (ConfigValue)db.get(ConfigValue.class, key);
	}

	/**
	 * Gibt den Wert des Konfigurationsobjekts mit dem angegebenen Schluessel im gewuenschten Typ
	 * zurueck. Die Methode wandelt den String-Wert soweit moeglich in den Zieltyp um. Wird der
	 * Zieltyp nicht unterstuetzt wird ein Fehler geworfen.
	 * @param type Der Zieltyp (z.B. {@link java.lang.Integer}).
	 * @param key Der Schluessel des Konfigurationsobjekts
	 * @param <T> Der Zieltyp
	 * @return Der Wert des Konfigurationsobjekts im gewueschten Zieltyp
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> type, String key)
	{
		ConfigValue configValue = get(key);
		if( type == String.class )
		{
			return (T)configValue.getValue();
		}
		if( type == Integer.class )
		{
			return (T)Integer.valueOf(configValue.getValue());
		}
		if( type == Long.class )
		{
			return (T)Long.valueOf(configValue.getValue());
		}
		if( type == Double.class )
		{
			return (T)Double.valueOf(configValue.getValue());
		}
		if( type == Boolean.class )
		{
			return (T)Boolean.valueOf(configValue.getValue());
		}

		throw new IllegalArgumentException("Der Zieltyp "+type.getName()+" wird nicht unterstuetzt");
	}
}
