package net.driftingsouls.ds2.server.config;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Die Gesamtkonfiguration fuer in einem System spawnende Felsbrocken
 * unterschiedlichen Typs und Cargos. Jede Gesamtkonfiguration legt die
 * maximale Anzahl an Felsbrocken jeweiligen System fest. Ferner
 * besteht sie aus einer beliebigen Menge an konkreten Felsbrocken-Konfigurationen
 * ({@link ConfigFelsbrocken}).
 * @author christopherjung
 *
 */
@Entity
@Table(name="config_felsbrocken_systems")
public class ConfigFelsbrockenSystem
{
	@Id
	private int system;
	private int count;
	@OneToMany(mappedBy="system", cascade = CascadeType.ALL)
	@Sort(type=SortType.NATURAL)
	private SortedSet<ConfigFelsbrocken> felsbrocken;

	/**
	 * Konfiguration.
	 */
	protected ConfigFelsbrockenSystem()
	{
		this.felsbrocken = new TreeSet<>();
	}

	/**
	 * Konstruktor.
	 * @param system Das Sternensystem
	 * @param anzahl Die maximale Anzahl an Felsbrocken
	 */
	public ConfigFelsbrockenSystem(StarSystem system, int anzahl)
	{
		this();

		this.system = system.getID();
		this.count = anzahl;
	}

	/**
	 * Gibt die Maximalanzahl an Felsbrocken im System zurueck.
	 * @return Die Maximalanzahl
	 */
	public int getCount()
	{
		return count;
	}

	/**
	 * Setzt die Maximalanzahl an Felsbrocken im System.
	 * @param count Die Maximalanzahl
	 */
	public void setCount(int count)
	{
		this.count = count;
	}

	/**
	 * Gibt alle Einzelkonfigurationen fuer Felsbrocken im System
	 * zurueck.
	 * @return Die Einzelkonfigurationen
	 */
	public SortedSet<ConfigFelsbrocken> getFelsbrocken()
	{
		return felsbrocken;
	}

	/**
	 * Setzt die Einzelkonfigurationen fuer Felsbrocken im System.
	 * @param felsbrocken Die Einzelkonfigurationen
	 */
	public void setFelsbrocken(SortedSet<ConfigFelsbrocken> felsbrocken)
	{
		this.felsbrocken = felsbrocken;
	}

	/**
	 * Gibt die ID des Systems zurueck, in dem die Felsbrocken
	 * spawnen sollen.
	 * @return Die ID
	 */
	public int getSystem()
	{
		return system;
	}
}
