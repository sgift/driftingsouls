package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IENone;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein Item das ausschliesslich als Handelsware fungiert.
 */
@Entity
@DiscriminatorValue("Ware")
public class Ware extends Item
{
	/**
	 * Konstruktor.
	 */
	protected Ware()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Ware(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Ware(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IENone getEffect()
	{
		return new IENone();
	}
}
