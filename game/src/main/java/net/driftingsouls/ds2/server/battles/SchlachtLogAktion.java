package net.driftingsouls.ds2.server.battles;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * Logeintrag zu einer Aktion in einer Schlacht.
 */
@Entity
@DiscriminatorValue("aktion")
public class SchlachtLogAktion extends SchlachtLogEintrag
{
	private int seite;
	@Lob
	private String text;

	/**
	 * Konstruktor.
	 */
	protected SchlachtLogAktion()
	{
		// JPA-Konstruktor
	}

	/**
	 * Konstruktor.
	 * @param seite Die Nummer der handelnden Seite
	 * @param text Der Text der Aktion
	 */
	public SchlachtLogAktion(int seite, String text)
	{
		this.seite = seite;
		this.text = text;
	}

	/**
	 * Gibt den Text der Aktion zurueck.
	 * @return Der Text
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * Setzt den Text der Aktion.
	 * @param text Der Text
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Gibt die Nummer der Seite zurueck, die die Aktion ausgefuehrt hat.
	 * @return Die Nummer
	 */
	public int getSeite()
	{
		return seite;
	}
}
