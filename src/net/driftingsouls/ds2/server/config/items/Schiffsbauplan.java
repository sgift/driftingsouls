package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDraftShip;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Der Bauplan zu einem Schiff.
 */
@Entity
@DiscriminatorValue("Schiffsbauplan")
public class Schiffsbauplan extends Item
{
	/**
	 * Konstruktor.
	 */
	protected Schiffsbauplan()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsbauplan(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Schiffsbauplan(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEDraftShip getEffect()
	{
		return (IEDraftShip) super.getEffect();
	}
}
