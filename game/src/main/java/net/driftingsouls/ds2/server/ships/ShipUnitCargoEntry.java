package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.units.UnitType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A unit cargo entry for ships.
 * The cargo entry filters all base related cargos from results.
 *
 * @author Drifting-Souls Team
 */
@Entity
@DiscriminatorValue("2")
public class ShipUnitCargoEntry extends UnitCargoEntry
{
	@ManyToOne(cascade={})
	@JoinColumn
	@ForeignKey(name="cargo_entries_units_fk_schiff")
	private Ship schiff;

	/**
	 * Default.
	 */
	public ShipUnitCargoEntry()
	{}

	/**
	 * @param schiff Das Schiff zu dem der UnitCargo-Eintrag gehoert
	 * @param unittype Der Einheitentyp
	 * @param amount Die Menge
	 */
	public ShipUnitCargoEntry(Ship schiff, UnitType unittype, long amount)
	{
		super(unittype, amount);
		this.schiff = schiff;
	}

	@Override
	public UnitCargoEntry createCopy()
	{
		return new ShipUnitCargoEntry(this.schiff,getUnitType(),getAmount());
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + schiff.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if( this == obj )
		{
			return true;
		}
		if( obj == null )
		{
			return false;
		}
		if( obj instanceof HibernateProxy)
		{
			obj = ((HibernateProxy)obj).getHibernateLazyInitializer().getImplementation();
		}
		if( getClass() != obj.getClass() )
		{
			return false;
		}
		ShipUnitCargoEntry other = (ShipUnitCargoEntry)obj;
		if( !super.equals(other) )
		{
			return false;
		}
		if( schiff == null )
		{
			return other.schiff == null;
		}
		else {
			return this.schiff.equals(other.schiff);
		}
	}
}
