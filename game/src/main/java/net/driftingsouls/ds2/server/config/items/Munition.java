package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Eine Munitionseinheit, die von einer geeigneten Waffe verschossen werden kann.
 */
@Entity
@DiscriminatorValue("Munition")
public class Munition extends Item
{
	@ManyToOne
	@JoinColumn
	@ForeignKey(name="items_fk_munitionsdefinition")
	private Munitionsdefinition munitionsdefinition;

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
		return new IEAmmo(munitionsdefinition);
	}

	/**
	 * Gibt die zugehoerige Munitionsdefinition zurueck.
	 * @return Die Munitionsdefinition
	 */
	public Munitionsdefinition getMunitionsdefinition()
	{
		return munitionsdefinition;
	}

	/**
	 * Setzt die zugehoerige Munitionsdefinition.
	 * @param munitionsdefinition Die Munitionsdefinition
	 */
	public void setMunitionsdefinition(Munitionsdefinition munitionsdefinition)
	{
		this.munitionsdefinition = munitionsdefinition;
	}
}
