package net.driftingsouls.ds2.server.entities;

import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
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

	public Rasse(String name, boolean playable)
	{
		this.name = name;
		this.playable = playable;
	}

	public Rasse(String name, boolean playable, boolean playableext, Rasse memberIn)
	{
		this(name, playable);
		this.memberIn = memberIn;

		this.playableext = playable || playableext;
	}

	/**
	 * Prueft, ob die Rasse direkt oder indirekt Mitglied in einer anderen Rasse ist.
	 *
	 * @param rasse die Rasse, in der die Mitgliedschaft geprueft werden soll
	 * @return <code>true</code>, falls die Rasse Mitglied ist
	 */
	public boolean isMemberIn(@Nonnull Rasse rasse)
	{
		return isMemberIn(rasse.getId());
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

	/**
	 * Setzt den Kopf der Rasse.
	 * @param user Der Spieler (NPC)
	 */
	public void setHead(User user)
	{
		head = user;
	}

	/**
	 * Prueft, ob ein Spieler ein Kopf der Rasse ist.
	 *
	 * @param user Der Spielers
	 * @return <code>true</code>, falls der Spieler ein Kopf der Rasse ist
	 */
	public boolean isHead(@Nonnull User user)
	{
		return head != null && head == user;
	}

	/**
	 * Gibt den Kopf der Rasse zurueck.
	 * @return Der Spieler (NPC)
	 */
	public User getHead()
	{
		return head;
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
	 * Setzt den Namen der Rasse.
	 *
	 * @param name der Name
	 */
	public void setName(String name)
	{
		this.name = name;
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
	 * Setzt, ob die Rasse spielbar ist, d.h. ob Spieler sich unter Angabe dieser Rasse
	 * registrieren koennen.
	 *
	 * @param playable <code>true</code>, falls die Rasse spielbar ist
	 */
	public void setPlayable(boolean playable)
	{
		this.playable = playable;
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
	 * Setzt, ob die Rasse "erweitert" spielbar ist. Diese Rassen werden im Forschungsbaum
	 * usw aufgelistet und koennen zu einem spaeteren Zeitpunkt im Spiel ausgewaehlt werden. Sie
	 * stehen jedoch nicht bei der Registrierung zur Auswahl.
	 *
	 * @param playableext <code>true</code>, falls die Rasse erweitert spielbar ist
	 */
	public void setExtPlayable(boolean playableext)
	{
		this.playableext = playableext;
	}

	/**
	 * Gibt den Namensgenerator fuer Personennamen zurueck.
	 *
	 * @return Der Namensgenerator
	 */
	public @Nonnull PersonenNamenGenerator getPersonenNamenGenerator()
	{
		if (personenNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getPersonenNamenGenerator();
		}
		else if( personenNamenGenerator == null ) {
			return PersonenNamenGenerator.ENGLISCH;
		}
		return personenNamenGenerator;
	}

	/**
	 * Setzt Namensgenerator fuer Personennamen zurueck.
	 *
	 * @param personenNamenGenerator Der Namensgenerator oder <code>null</code>
	 */
	public void setPersonenNamenGenerator(@Nonnull PersonenNamenGenerator personenNamenGenerator)
	{
		this.personenNamenGenerator = personenNamenGenerator;
	}

	/**
	 * Gibt den Namensgenerator fuer Schiffsklassen zurueck.
	 *
	 * @return Der Namensgenerator
	 */
	public @Nonnull SchiffsKlassenNamenGenerator getSchiffsKlassenNamenGenerator()
	{
		if (schiffsKlassenNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getSchiffsKlassenNamenGenerator();
		}
		else if( schiffsKlassenNamenGenerator == null ) {
			return SchiffsKlassenNamenGenerator.KEIN_KUERZEL;
		}
		return schiffsKlassenNamenGenerator;
	}

	/**
	 * Setzt den Namensgenerator fuer Schiffsklassen.
	 *
	 * @param schiffsKlassenNamenGenerator Der Namensgenerator
	 */
	public void setSchiffsKlassenNamenGenerator(@Nonnull SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator)
	{
		this.schiffsKlassenNamenGenerator = schiffsKlassenNamenGenerator;
	}

	/**
	 * Gibt den Namensgenerator fuer Schiffsnamen zurueck.
	 *
	 * @return Der Namensgenerator
	 */
	public @Nonnull SchiffsNamenGenerator getSchiffsNamenGenerator()
	{
		if (schiffsNamenGenerator == null && (memberIn != null))
		{
			return memberIn.getSchiffsNamenGenerator();
		}
		else if( schiffsNamenGenerator == null ) {
			return SchiffsNamenGenerator.SCHIFFSTYP;
		}
		return schiffsNamenGenerator;
	}

	/**
	 * Setzt den Namensgenerator fuer Schiffsnamen.
	 *
	 * @param schiffsNamenGenerator Der Namensgenerator
	 */
	public void setSchiffsNamenGenerator(@Nonnull SchiffsNamenGenerator schiffsNamenGenerator)
	{
		this.schiffsNamenGenerator = schiffsNamenGenerator;
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

	/**
	 * Setzt die Beschreibung der Rasse.
	 *
	 * @param description Die Beschreibung der Rasse
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}

	/**
	 * Gibt zurueck, zu welcher Rasse diese Rasse gehoert.
	 * @return Die Rasse oder <code>null</code>
	 */
	public Rasse getMemberIn()
	{
		return memberIn;
	}

	/**
	 * Setzt, zu welcher Rasse diese Rasse gehoert.
	 * @param memberIn Die Rasse oder <code>null</code>
	 */
	public void setMemberIn(Rasse memberIn)
	{
		this.memberIn = memberIn;
	}
}
