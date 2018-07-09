package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDisableIFF;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Item zum Deaktivieren der IFF auf einem Schiff.
 */
@Entity
@DiscriminatorValue("IffDeaktivieren")
public class IffDeaktivierenItem extends Item
{
	/**
	 * Konstruktor.
	 */
	protected IffDeaktivierenItem()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public IffDeaktivierenItem(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public IffDeaktivierenItem(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEDisableIFF getEffect()
	{
		return new IEDisableIFF();
	}
}
