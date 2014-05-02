package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEModuleSetMeta;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffectFactory;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ein Item, dass Set-Effekte bei mehreren eingebauten {@link net.driftingsouls.ds2.server.config.items.Schiffsmodul}en
 * beschreibt.
 */
@Entity
@DiscriminatorValue("SchiffsmodulSet")
public class SchiffsmodulSet extends Item
{
	/**
	 * Konstruktor.
	 */
	protected SchiffsmodulSet()
	{
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 */
	public SchiffsmodulSet(int id, String name)
	{
		super(id, name);
	}

	/**
	 * Konstruktor.
	 * @param id Die ID
	 * @param name Der Name
	 * @param picture Das Bild
	 */
	public SchiffsmodulSet(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEModuleSetMeta getEffect()
	{
		try {
			return (IEModuleSetMeta)ItemEffectFactory.fromString(this.effect);
		}
		catch (Exception e) {
			throw new IllegalStateException("Konnte keinen Effekt fuer Item "+getID()+" ermitteln",e);
		}
	}
}
