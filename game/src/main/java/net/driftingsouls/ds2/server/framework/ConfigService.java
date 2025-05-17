package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

/**
 * Service zum Zugriff auf Konfigurationswerte. Die Konfigurationswerte werden durch {@link net.driftingsouls.ds2.server.framework.ConfigValue}s
 * repraesentiert und besitzen primaer einen Schluessel (key bzw name) und einen Wert in Form eines Strings.
 */
@Service
public class ConfigService
{
	private final EntityManager db;

	public ConfigService(@Lazy EntityManager db) {
		this.db = db;
	}

	/**
	 * Gibt das Konfigurationsobjekt mit dem angegebenen Schluessel zurueck. Die Methode sollte
	 * nur verwendet werden, wenn wirklich das gesamte Konfigurationsobjekt benoetigt wird.
	 * Fuer direkte Zugriffe auf den Wert existiert die Methode {@link #getValue(ConfigValueDescriptor)}.
	 * @param descriptor Der Schluessel des Konfigurationsobjekts
	 * @return Das Konfigurationsobjekt
	 * @see #getValue(ConfigValueDescriptor)
	 */
	public ConfigValue get(ConfigValueDescriptor<?> descriptor)
	{
		ConfigValue value = db.find(ConfigValue.class, descriptor.getName());
		if( value == null )
		{
			value = new ConfigValue(descriptor.getName(), descriptor.getDefaultValue());
			db.persist(value);
		}
		return value;
	}

	/**
	 * Gibt den momentanen Wert der angegebenen Konfigurationseinstellung
	 * zurueck.
	 * @param descriptor Der Descriptor fuer die Konfigurationseinstellung
	 * @param <T> Der Zieltyp
	 * @return Der Wert des Konfigurationsobjekts im Zieldatentyp
	 */
	public <T> T getValue(ConfigValueDescriptor<T> descriptor)
	{
		ConfigValue configValue = db.find(ConfigValue.class, descriptor.getName());
		return StringToTypeConverter.convert(descriptor.getType(), configValue != null ? configValue.getValue() : descriptor.getDefaultValue());
	}
}
