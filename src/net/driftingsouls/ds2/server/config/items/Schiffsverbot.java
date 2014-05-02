package net.driftingsouls.ds2.server.config.items;

import net.driftingsouls.ds2.server.config.items.effects.IEDisableShip;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Ein Item, dass den Bau eines bestimmten Schiffs verbietet.
 */
@Entity
@DiscriminatorValue("Schiffsverbot")
public class Schiffsverbot extends Item
{
	@ManyToOne
	@JoinColumn
	@ForeignKey(name = "schiffsverbot_fk_schiffstyp")
	private ShipType schiffstyp;
	private boolean allianzEffekt;

	/**
	 * Konstruktor.
	 */
	protected Schiffsverbot()
	{
	}

	/**
	 * Konstruktor.
	 *
	 * @param id Die ID
	 * @param name Der Name
	 */
	public Schiffsverbot(int id, String name)
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
	public Schiffsverbot(int id, String name, String picture)
	{
		super(id, name, picture);
	}

	@Override
	public IEDisableShip getEffect()
	{
		return new IEDisableShip(allianzEffekt, schiffstyp);
	}

	/**
	 * Gibt den Schiffstyp zurueck, dessen Bau verboten ist.
	 * @return Der Schiffstyp
	 */
	public ShipType getSchiffstyp()
	{
		return schiffstyp;
	}

	/**
	 * Setzt den Schiffstyp, dessen Bau verboten ist.
	 * @param schiffstyp Der Schiffstyp
	 */
	public void setSchiffstyp(ShipType schiffstyp)
	{
		this.schiffstyp = schiffstyp;
	}

	/**
	 * Gibt zurueck, ob das Verbot allianzweit zur Verfuegung gestellt werden kann.
	 * @return true, falls dem so ist
	 */
	public boolean isAllianzEffekt()
	{
		return allianzEffekt;
	}

	/**
	 * Setzt, ob das Verbot allianzweit zur Verfuegung gestellt werden kann.
	 * @param allianzEffekt true, falls dem so ist
	 */
	public void setAllianzEffekt(boolean allianzEffekt)
	{
		this.allianzEffekt = allianzEffekt;
	}
}
