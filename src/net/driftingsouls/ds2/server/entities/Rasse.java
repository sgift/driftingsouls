package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * Repraesentiert eine Rasse in Drifting Souls.
 *
 * @author Christopher Jung
 */
@Entity(name = "rasse")
public class Rasse
{
	private String name;
	@Id
	@GeneratedValue
	private int id;
	@ManyToOne(optional = true)
	@JoinColumn(nullable = true)
	@ForeignKey(name = "rasse_fk_rasse")
	private Rasse memberIn;
	private boolean playable;
	private boolean playableext;
	@OneToOne(cascade = {})
	@JoinColumn(nullable = true)
	@ForeignKey(name = "rasse_fk_user")
	private User head;
	@SuppressWarnings("UnusedDeclaration")
	@Enumerated(EnumType.STRING)
	private PersonenNamenGenerator personenNamenGenerator;
	@SuppressWarnings("UnusedDeclaration")
	@Enumerated(EnumType.STRING)
	private SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator;
	@SuppressWarnings("UnusedDeclaration")
	@Enumerated(EnumType.STRING)
	private SchiffsNamenGenerator schiffsNamenGenerator;
	@Lob
	private String description;

	protected Rasse()
	{
		// Hibernate Constructor
	}

	protected Rasse(int id, String name, boolean playable)
	{
		this.name = name;
		this.id = id;
		this.playable = playable;
	}

	protected Rasse(int id, String name, boolean playable, boolean playableext, Rasse memberIn)
	{
		this(id, name, playable);
		this.memberIn = memberIn;

		this.playableext = playable || playableext;
	}

	/**
	 * Prueft, ob die Rasse direkt oder indirekt Mitglied in einer anderen Rasse ist.
	 *
	 * @param rasse die ID der Rasse, in der die Mitgliedschaft geprueft werden soll
	 * @return <code>true</code>, falls die Rasse Mitglied ist
	 */
	public boolean isMemberIn(int rasse)
	{
		if (rasse == -1)
		{
			return true;
		}

		if (id == rasse)
		{
			return true;
		}

		if ((memberIn != null) && (memberIn.getId() == rasse))
		{
			return true;
		}

		if (memberIn != null)
		{
			return memberIn.isMemberIn(rasse);
		}
		return false;
	}

	protected void setHead(User user)
	{
		head = user;
	}

	/**
	 * Prueft, ob ein Spieler ein Kopf der Rasse ist.
	 *
	 * @param id die ID des Spielers
	 * @return <code>true</code>, falls der Spieler ein Kopf der Rasse ist
	 */
	public boolean isHead(int id)
	{
		return head != null && head.getId() == id;
	}

	/**
	 * Gibt die ID der Rasse zurueck.
	 *
	 * @return Die ID der Rasse
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Gibt den Namen der Rasse zurueck.
	 *
	 * @return der Name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gibt zurueck, ob die Rasse spielbar ist, d.h. ob Spieler sich unter Angabe dieser Rasse
	 * registrieren koennen.
	 *
	 * @return <code>true</code>, falls die Rasse spielbar ist
	 */
	public boolean isPlayable()
	{
		return playable;
	}

	/**
	 * Gibt zurueck, ob die Rasse "erweitert" spielbar ist. Diese Rassen werden im Forschungsbaum
	 * usw aufgelistet und koennen zu einem spaeteren Zeitpunkt im Spiel ausgewaehlt werden. Sie
	 * stehen jedoch nicht bei der Registrierung zur Auswahl.
	 *
	 * @return <code>true</code>, falls die Rasse erweitert spielbar ist
	 */
	public boolean isExtPlayable()
	{
		return playableext;
	}

	/**
	 * Gibt den Namensgenerator fuer Personennamen zurueck. Es wird <code>null</code> zurueckgegeben,
	 * falls kein Namensgenerator fuer den Typ vorhanden ist.
	 *
	 * @return Der Namensgenerator
	 */
	public PersonenNamenGenerator getPersonenNamenGenerator()
	{
		if (personenNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getPersonenNamenGenerator();
		}
		return personenNamenGenerator;
	}

	/**
	 * Gibt den Namensgenerator fuer Schiffsklassen zurueck. Es wird <code>null</code> zurueckgegeben,
	 * falls kein Namensgenerator fuer den Typ vorhanden ist.
	 *
	 * @return Der Namensgenerator
	 */
	public SchiffsKlassenNamenGenerator getSchiffsKlassenNamenGenerator()
	{
		if (schiffsKlassenNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getSchiffsKlassenNamenGenerator();
		}
		return schiffsKlassenNamenGenerator;
	}

	/**
	 * Gibt den Namensgenerator fuer Schiffsnamen zurueck. Es wird <code>null</code> zurueckgegeben,
	 * falls kein Namensgenerator fuer den Typ vorhanden ist.
	 *
	 * @return Der Namensgenerator
	 */
	public SchiffsNamenGenerator getSchiffsNamenGenerator()
	{
		if (schiffsNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getSchiffsNamenGenerator();
		}
		return schiffsNamenGenerator;
	}

	/**
	 * Gibt die Beschreibung der Rasse zurueck.
	 *
	 * @return Die Beschreibung der Rasse
	 */
	public String getDescription()
	{
		return description;
	}

	protected void setDescription(String description)
	{
		this.description = description;
	}
}
