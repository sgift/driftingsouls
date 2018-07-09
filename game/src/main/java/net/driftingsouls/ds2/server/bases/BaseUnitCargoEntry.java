package net.driftingsouls.ds2.server.bases;

import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.units.UnitType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.proxy.HibernateProxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A unit cargo entry for bases.
 * The cargo entry filters all ship related cargos from results.
 *
 * @author Drifting-Souls Team
 */
@Entity
@DiscriminatorValue("1")
public class BaseUnitCargoEntry extends UnitCargoEntry
{
	@ManyToOne(cascade = {})
	@JoinColumn
	@ForeignKey(name="cargo_entries_units_fk_basis")
	private Base basis;

	/**
	 * Default.
	 */
	public BaseUnitCargoEntry()
	{}

	/**
	 * @param base Die Basis zu der der UnitCargo-Eintrag gehoert
	 * @param unittype Der Einheitentyp
	 * @param amount Die Menge
	 */
	public BaseUnitCargoEntry(Base base, UnitType unittype, long amount)
	{
		super(unittype, amount);
		this.basis = base;
	}

	@Override
	public UnitCargoEntry createCopy()
	{
		return new BaseUnitCargoEntry(this.basis,getUnitType(),getAmount());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + basis.hashCode();
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
		BaseUnitCargoEntry other = (BaseUnitCargoEntry)obj;
		if( !super.equals(other) )
		{
			return false;
		}
		if( basis == null )
		{
			return other.basis == null;
		}
		else {
			return basis.equals(other.basis);
		}
	}
}
