package net.driftingsouls.ds2.server.entities;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Repraesentation einer Menge von vergebenen Loyalitaetspunkten durch einen NPC
 * an einen Nutzer. Der Vergabe ist jeweils ein Datum sowie ein Grund zugeordnet.
 * Optional koennen weitere Anmerkungen erfolgen.
 * @author christopherjung
 *
 */
@Entity
@Table(name="loyalitaetspunkte")
public class Loyalitaetspunkte implements Comparable<Loyalitaetspunkte>
{
	@Id
	@GeneratedValue
	private int id;

	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="loyalitaetspunkte_fk_users_1")
	private User user;
	@Column(nullable = false)
	private String grund;
	@Lob
	private String anmerkungen;
	private int anzahlPunkte;
	@Column(nullable = false)
	private Date zeitpunkt;
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="loyalitaetspunkte_fk_users_2")
	private User verliehenDurch;

	/**
	 * Konstruktor.
	 */
	protected Loyalitaetspunkte()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param user Der User dem die Punkte verliehen werden
	 * @param verliehenDurch Der NPC, der die Punkte vergeben hat
	 * @param grund Der Grund
	 * @param anzahlPunkte Die Anzahl der Punkte
	 */
	public Loyalitaetspunkte(User user, User verliehenDurch, String grund, int anzahlPunkte)
	{
		this.user = user;
		this.verliehenDurch = verliehenDurch;
		this.grund = grund;
		this.anzahlPunkte = anzahlPunkte;
		this.zeitpunkt = new Date();
	}

	/**
	 * Gibt die ID des DB-Eintrags zurueck.
	 * @return Die ID
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Gibt die weiteren Anmerkungen zur Punktevergabe zurueck.
	 * @return Die Anmerkungen
	 */
	public String getAnmerkungen()
	{
		return this.anmerkungen;
	}

	/**
	 * Setzt die weiteren Anmerkungen zur Punktevergabe.
	 * @param anmerkungen Die Anmerkungen
	 */
	public void setAnmerkungen(String anmerkungen)
	{
		this.anmerkungen = anmerkungen;
	}

	/**
	 * Gibt den Nutzer zurueck, dem die Punkte verliehen wurden.
	 * @return Der Nutzer
	 */
	public User getUser()
	{
		return this.user;
	}

	/**
	 * Gibt den Grund fuer die Punktevergabe zurueck.
	 * @return Der Grund
	 */
	public String getGrund()
	{
		return this.grund;
	}

	/**
	 * Gibt die Anzahl der verliehenen Punkte zurueck.
	 * @return Die Anzahl
	 */
	public int getAnzahlPunkte()
	{
		return this.anzahlPunkte;
	}

	/**
	 * Gibt den Zeitpunkt der Punktevergabe zurueck.
	 * @return Der Zeitpunkt
	 */
	public Date getZeitpunkt()
	{
		return new Date(this.zeitpunkt.getTime());
	}

	/**
	 * Gibt den Nutzer (NPC) zurueck, der die Punkte verliehen hat.
	 * @return Der Nutzer
	 */
	public User getVerliehenDurch()
	{
		return verliehenDurch;
	}

	@Override
	public int compareTo(@NotNull Loyalitaetspunkte arg0)
	{
		int diff = this.zeitpunkt.compareTo(arg0.zeitpunkt);
		if( diff != 0 )
		{
			return -diff;
		}
		return this.grund.compareTo(arg0.grund);
	}
}
