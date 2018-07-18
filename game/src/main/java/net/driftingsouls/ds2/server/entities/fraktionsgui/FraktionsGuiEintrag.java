package net.driftingsouls.ds2.server.entities.fraktionsgui;

import net.driftingsouls.ds2.server.entities.User;
import org.hibernate.annotations.ForeignKey;

import javax.annotation.Nonnull;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Version;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Repraesentation eines Eintrags auf der Fraktionsoberflaeche.
 * Ein Eintrag ist immer fest einem Benutzer (NPC) zugeordnet
 * und besteht aus einer Anzahl fuer diesen Eintrag freigeschalteter
 * Seiten (Reiter).
 */
@Entity
public class FraktionsGuiEintrag
{
	/**
	 * Die Seiten, die fuer einen Eintrag freigeschaltet werden koennen.
	 */
	public enum Seite
	{
		ALLGEMEIN("general"),
		VERSTEIGERUNG("versteigerung"),
		ANGEBOTE("angebote"),
		SHOP("shop"),
		AUSBAU("ausbau"),
		BANK("bank"),
		AKTION_MELDEN("aktionmelden"),
		SONSTIGES("other");

		private final String id;

		Seite(String id)
		{
			this.id = id;
		}

		public String getId()
		{
			return id;
		}
	}

	@Id @GeneratedValue
	private Long id;
	@Version
	private int version;

	@OneToOne(optional = false)
	@JoinColumn(nullable = false)
	@ForeignKey(name="fraktionsguieintrag_fk_user")
	private User user;

	@Lob
	private String text;

	@ElementCollection
	@CollectionTable
	@ForeignKey(name="fraktionsguieintrag_fk_fraktionsguieintrag_seiten")
	@Enumerated(EnumType.STRING)
	private Set<Seite> seiten;

	/**
	 * Konstruktor.
	 */
	protected FraktionsGuiEintrag()
	{
		// EMPTY
	}

	/**
	 * Konstruktor.
	 * @param user Der Benutzer fuer den der Eintrag erzeugt werden soll
	 * @param seiten Die Seiten (Tabs) des Eintrags
	 */
	public FraktionsGuiEintrag(User user, Seite ... seiten)
	{
		this.user = user;
		this.seiten = new HashSet<>(Arrays.asList(seiten));
	}

	/**
	 * Gibt den mit dem Eintrag verbundenen Benutzer zurueck.
	 * @return Der Benutzer
	 */
	public User getUser()
	{
		return user;
	}

	/**
	 * Setzt den mit dem Eintrag verbundenen Benutzer.
	 * @param user Der Benutzer
	 */
	public void setUser(User user)
	{
		this.user = user;
	}

	/**
	 * Gibt den anzuzeigenden Beschreibungstext zurueck.
	 * @return Der Text
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * Setzt den anzuzeigenden Beschreibungstext.
	 * @param text Der Text
	 */
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Gibt die anzuzeigenden Seiten (Reiter) zurueck.
	 * @return Die Seiten
	 */
	public Set<Seite> getSeiten()
	{
		return seiten;
	}

	/**
	 * Setzt die anzuzeigenden Seiten (Reiter).
	 * @param seiten Die Seiten
	 */
	public void setSeiten(Set<Seite> seiten)
	{
		this.seiten = seiten;
	}

	/**
	 * Gibt die erste Seite zurueck, die in der Gui angezeigt werden soll.
	 * @return Die erste Seite
	 */
	public @Nonnull Seite getErsteSeite()
	{
		for (Seite seite : Seite.values())
		{
			if (this.seiten.contains(seite))
			{
				return seite;
			}
		}

		throw new AssertionError("Keine Seiten fuer Fraktions-GUI konfiguriert");
	}
}
