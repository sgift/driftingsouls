package net.driftingsouls.ds2.server.battles;

import net.driftingsouls.ds2.server.entities.User;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein spezieller Eintrag im Schlachtlog der den Wechsel eines Kommandanten signalisiert.
 */
@Entity
@DiscriminatorValue("KommandantWechselt")
public class SchlachtLogKommandantWechselt extends SchlachtLogEintrag
{
	private int seite;
	private int userId;
	private String name;
	private Integer allianzId;

	/**
	 * Konstruktor.
	 */
	protected SchlachtLogKommandantWechselt()
	{
		// JPA-Konstruktor
	}

	/**
	 * Konstruktor.
	 * @param seite Die Nummer der handelnden Seite
	 * @param user Der neue Kommandant
	 */
	public SchlachtLogKommandantWechselt(int seite, User user)
	{
		this.seite = seite;
		this.userId = user.getId();
		this.allianzId = user.getAlly() != null ? user.getAlly().getId() : null;
		this.name = user.getName();
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
	 * Gibt die ID des neuen Kommandanten zurueck.
	 * @return Die ID
	 */
	public int getUserId()
	{
		return userId;
	}

	/**
	 * Gibt den Namen des neuen Kommandanten zurueck.
	 * @return Der Name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt, sofern vorhanden, die ID der Allianz des neuen Kommandanten zurueck.
	 * @return Die ID oder <code>null</code>
	 */
	public Integer getAllianzId()
	{
		return allianzId;
	}
}
