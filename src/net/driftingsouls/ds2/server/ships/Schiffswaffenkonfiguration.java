package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.entities.Weapon;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

/**
 * Die Konfiguration einer konkreten Waffe auf einem konkreten Schiff. Die Konfiguration
 * wird durch die Waffe selbst sowie ihre Anzahl auf dem Schiff, die generierte Hitze pro Schuss
 * und die maximale Ueberhitzung bestimmt.
 */
@Entity
public class Schiffswaffenkonfiguration
{
	@Id
	@GeneratedValue
	private Long id;

	@Version
	private int version;

	@ManyToOne
	@JoinColumn
	@ForeignKey(name="weapon_changeset_fk_weapon")
	private Weapon waffe;
	private int maxUeberhitzung;
	private int hitze;
	private int anzahl;

	/**
	 * Konstruktor.
	 */
	protected Schiffswaffenkonfiguration()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param waffe Die Waffe
	 * @param anzahl Die Anzahl der montierten Waffen
	 * @param hitze Die pro Schuss generierte Hitze
	 * @param maxUeberhitzung Die maximale Ueberhitzung
	 */
	public Schiffswaffenkonfiguration(Weapon waffe, int anzahl, int hitze, int maxUeberhitzung)
	{
		this.waffe = waffe;
		this.maxUeberhitzung = maxUeberhitzung;
		this.hitze = hitze;
		this.anzahl = anzahl;
	}

	/**
	 * Gibt die Waffe zurueck.
	 * @return Die Waffe
	 */
	public Weapon getWaffe()
	{
		return waffe;
	}

	/**
	 * Setzt die Waffe.
	 * @param waffe Die Waffe
	 */
	public void setWaffe(Weapon waffe)
	{
		this.waffe = waffe;
	}

	/**
	 * Gibt die maximale Ueberhitzung zurueck.
	 * @return Die maximale Ueberhitzung
	 */
	public int getMaxUeberhitzung()
	{
		return maxUeberhitzung;
	}

	/**
	 * Setzt die maximale Ueberhitzung.
	 * @param maxUeberhitzung Die maximale Ueberhitzung
	 */
	public void setMaxUeberhitzung(int maxUeberhitzung)
	{
		this.maxUeberhitzung = maxUeberhitzung;
	}

	/**
	 * Gibt die pro Schuss generierte Hitze zurueck.
	 * @return Die Hitze
	 */
	public int getHitze()
	{
		return hitze;
	}

	/**
	 * Setzt die pro Schuss generierte Hitze.
	 * @param hitze Die Hitze
	 */
	public void setHitze(int hitze)
	{
		this.hitze = hitze;
	}

	/**
	 * Gibt die Anzahl der montierten Waffen dieses Typs zurueck.
	 * @return Die Anzahl der montierten Waffen
	 */
	public int getAnzahl()
	{
		return anzahl;
	}

	/**
	 * Setzt die Anzahl der montierten Waffen dieses Typs.
	 * @param anzahl Die Anzahl der montierten Waffen
	 */
	public void setAnzahl(int anzahl)
	{
		this.anzahl = anzahl;
	}
}
