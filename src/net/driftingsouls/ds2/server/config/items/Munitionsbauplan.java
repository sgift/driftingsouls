package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDraftAmmo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Der Bauplan fuer eine bestimmte Munition.
 */
@Entity
@DiscriminatorValue("Munitionsbauplan")
public class Munitionsbauplan extends Item
{
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
		return (IEDraftAmmo) super.getEffect();
	}
}
