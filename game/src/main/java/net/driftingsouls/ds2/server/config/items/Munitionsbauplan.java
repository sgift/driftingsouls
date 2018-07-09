package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDraftAmmo;
import net.driftingsouls.ds2.server.entities.FactoryEntry;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Der Bauplan fuer eine bestimmte Munition.
 */
@Entity
@DiscriminatorValue("Munitionsbauplan")
public class Munitionsbauplan extends Item
{
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="munitionsbauplan_fk_fabrikeintrag")
	private FactoryEntry fabrikeintrag;

	private boolean allianzEffekt;

	/**
	 * Konstruktor.
	 */
	protected Munitionsbauplan()
	{
	}

	/**
	 * Konstruktor.
	 *
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Munitionsbauplan(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 *
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Munitionsbauplan(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEDraftAmmo getEffect()
	{
		return new IEDraftAmmo(allianzEffekt, fabrikeintrag);
	}

	/**
	 * Gibt den durch diesen Bauplan ermoeglichen Fabrikeintrag zurueck.
	 * @return Der Eintrag
	 */
	public FactoryEntry getFabrikeintrag()
	{
		return fabrikeintrag;
	}

	/**
	 * Setzt den durch diesen Bauplan ermoeglichten Fabrikeintrag.
	 * @param fabrikeintrag Der Fabrikeintrag
	 */
	public void setFabrikeintrag(FactoryEntry fabrikeintrag)
	{
		this.fabrikeintrag = fabrikeintrag;
	}

	/**
	 * Gibt zurueck, ob der Bauplan allianzweit zur Verfuegung gestellt werden kann.
	 * @return true, falls dem so ist
	 */
	public boolean isAllianzEffekt()
	{
		return allianzEffekt;
	}

	/**
	 * Setzt, ob der Bauplan allianzweit zur Verfuegung gestellt werden kann.
	 * @param allianzEffekt true, falls dem so ist
	 */
	public void setAllianzEffekt(boolean allianzEffekt)
	{
		this.allianzEffekt = allianzEffekt;
	}
}
