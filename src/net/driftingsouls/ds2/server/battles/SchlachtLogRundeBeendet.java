package net.driftingsouls.ds2.server.battles;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Eine Runde beendet-Aktion in einem Schlachtlog.
 */
@Entity
@DiscriminatorValue("RundeBeendet")
public class SchlachtLogRundeBeendet extends SchlachtLogEintrag
{
	/**
	 * Die Art des Rundenwechsels.
	 */
	public enum Modus
	{
		/**
		 * Rundenwechsel fuer alle
		 */
		ALLE,
		/**
		 * Rundenwechsel nur fuer die handelnde Seite
		 */
		EIGENE
	}

	private int seite;
	private Modus typ;

	/**
	 * Konstruktor.
	 */
	protected SchlachtLogRundeBeendet()
	{
		// JPA-Konstruktor
	}

	/**
	 * Konstruktor.
	 * @param seite Die Nummer der Seite
	 * @param typ Der Typ des Rundenwechsels
	 */
	public SchlachtLogRundeBeendet(int seite, Modus typ)
	{
		this.seite = seite;
		this.typ = typ;
	}

	/**
	 * Gibt die Nummer der handelnden Seite zurueck.
	 * @return Die Nummer
	 */
	public int getSeite()
	{
		return seite;
	}

	/**
	 * Gibt die Art des Rundenwechsels zurueck.
	 * @return Die Art des Rundenwechsels
	 */
	public Modus getTyp()
	{
		return typ;
	}
}
