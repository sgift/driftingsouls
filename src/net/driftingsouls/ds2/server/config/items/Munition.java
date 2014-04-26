package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Eine Munitionseinheit, die von einer geeigneten Waffe verschossen werden kann.
 */
@Entity
@DiscriminatorValue("Munition")
public class Munition extends Item
{
	/**
	 * Konstruktor.
	 */
	protected Munition()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Munition(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Munition(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEAmmo getEffect()
	{
		return (IEAmmo) super.getEffect();
	}
}
