package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

/**
 * Service zum Zugriff auf Konfigurationswerte. Die Konfigurationswerte werden durch {@link net.driftingsouls.ds2.server.framework.ConfigValue}s
 * repraesentiert und besitzen primaer einen Schluessel (key bzw name) und einen Wert in Form eines Strings.
 */
@Service
public class ConfigService
{
	/**
	 * Gibt das Konfigurationsobjekt mit dem angegebenen Schluessel zurueck. Die Methode sollte
	 * nur verwendet werden, wenn wirklich das gesamte Konfigurationsobjekt benoetigt wird.
	 * Fuer direkte Zugriffe auf den Wert existiert die Methode {@link #getValue(ConfigValueDescriptor)}.
	 * @param db Die Session, die verwendet werden soll
	 * @param descriptor Der Schluessel des Konfigurationsobjekts
	 * @return Das Konfigurationsobjekt
	 * @see #getValue(ConfigValueDescriptor)
	 * @see #get(ConfigValueDescriptor)
	 */
	public ConfigValue get(Session db, ConfigValueDescriptor<?> descriptor) {
		ConfigValue value = (ConfigValue)db.get(ConfigValue.class, descriptor.getName());
		if( value == null )
		{
			value = new ConfigValue(descriptor.getName(), descriptor.getDefaultValue());
			db.persist(value);
		}
		return value;
	}


	public ConfigValue get(ConfigValueDescriptor<?> descriptor)
	{
		Session db = ContextMap.getContext().getDB();
		return get(db, descriptor);
	}

	/**
	 * Gibt den momentanen Wert der angegebenen Konfigurationseinstellung
	 * zurueck.
	 * @param db Die Session, die verwendet werden soll
	 * @param descriptor Der Descriptor fuer die Konfigurationseinstellung
	 * @param <T> Der Zieltyp
	 * @return Der Wert des Konfigurationsobjekts im Zieldatentyp
	 * @see #getValue(ConfigValueDescriptor)
	 */
	public <T> T getValue(Session db, ConfigValueDescriptor<T> descriptor) {
		ConfigValue configValue = (ConfigValue)db.get(ConfigValue.class, descriptor.getName());
		return StringToTypeConverter.convert(descriptor.getType(), configValue != null ? configValue.getValue() : descriptor.getDefaultValue());
	}


	public <T> T getValue(ConfigValueDescriptor<T> descriptor)
	{
		Session db = ContextMap.getContext().getDB();
		return getValue(db, descriptor);
	}
}
