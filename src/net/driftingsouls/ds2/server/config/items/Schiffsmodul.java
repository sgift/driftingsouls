package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEModule;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffectFactory;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein Item, dass als Modul in ein Schiff eingebaut werden kann.
 */
@Entity
@DiscriminatorValue("Schiffsmodul")
public class Schiffsmodul extends Item
{
	/**
	 * Konstruktor.
	 */
	protected Schiffsmodul()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsmodul(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public Schiffsmodul(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEModule getEffect()
	{
		try {
			return (IEModule)ItemEffectFactory.fromString(this.effect);
		}
		catch (Exception e) {
			throw new IllegalStateException("Konnte keinen Effekt fuer Item "+getID()+" ermitteln",e);
		}
	}
}
