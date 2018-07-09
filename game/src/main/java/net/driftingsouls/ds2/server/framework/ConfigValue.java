package net.driftingsouls.ds2.server.framework;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;


/**
 * Globale Konfigurationsoptionen und -werte. Eine Konfigurationsoption
 * wird eindeutig ueber ihren Namen identifiziert. Jeder Konfigurationsoption
 * ist zudem ein Wert zugeordnet.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="config")
public class ConfigValue
{
	@Id
	private String name;
	@Lob
	@Column(nullable = false)
	private String value;
	@Version
	private int version;

	/**
	 * Konstruktor.
	 */
	protected ConfigValue()
	{
	}

	/**
	 * Konstruktor.
	 * @param name Der name der Konfigurationseinstellung
	 * @param value Der Wert
	 */
	public ConfigValue(String name, String value)
	{
		this.name = name;
		this.value = value;
	}

	/**
	 * Gibt den Namen der Konfigurationsoption zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gibt den momentanen Wert der Konfigurationsoption zurueck.
	 * @return Der Wert
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * Gibt die Versionsnummer zurueck (Hibernate-Interna).
	 * @return Die Versionsnummer
	 */
	public int getVersion()
	{
		return version;
	}

	/**
	 * Setzt den Namen der Konfigurationsoption.
	 * @param name Der Name
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Setzt den momentanen Wert der Konfigurationsoption.
	 * @param value Der Wert
	 */
	public void setValue(String value)
	{
		this.value = value;
	}

	/**
	 * Setzt die Versionsnummer (Hibernate-Interna).
	 * @param version Die Versionsnummer
	 */
	public void setVersion(int version)
	{
		this.version = version;
	}
}
