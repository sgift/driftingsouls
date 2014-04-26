package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDisableShip;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein Item, dass den Bau eines bestimmten Schiffs verbietet.
 */
@Entity
@DiscriminatorValue("Schiffsverbot")
public class Schiffsverbot extends Item
{
	/**
	 * Konstruktor.
	 */
	protected Schiffsverbot()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsverbot(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Schiffsverbot(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEDisableShip getEffect()
	{
		return (IEDisableShip)super.getEffect();
	}
}
