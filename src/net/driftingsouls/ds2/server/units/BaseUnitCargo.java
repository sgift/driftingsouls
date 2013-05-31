package net.driftingsouls.ds2.server.units;

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseUnitCargoEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Repraesentation eines UnitCargos auf einer Basis.
 */
public class BaseUnitCargo extends UnitCargo
{
	private static final Log LOG = LogFactory.getLog(BaseUnitCargo.class);

	private Base dest;

	/**
	 * Erzeugt einen leeren Unitcargo.
	 */
	public BaseUnitCargo()
	{
	}

	/**
	 * Erzeugt einen Unitcargo mit dem gegebenen Inhalt.
	 * @param unitcargo Der Inhalt
	 */
	public BaseUnitCargo(UnitCargo unitcargo)
	{
		super(unitcargo);
	}

	/**
	 * Zeugt einen Unitcargo mit dem gegebenen Inhalt fuer die angebene Basis.
	 * @param units Der Inahlt
	 * @param dest Die Basis
	 */
	public BaseUnitCargo(List<UnitCargoEntry> units, Base dest)
	{
		super(units);
		this.dest = dest;
	}

	/**
	 * Speichert das aktuelle UnitCargoObjekt.
	 */
	@Override
	public void save()
	{
		if( this.dest == null)
		{
			LOG.warn("Nicht genug Daten zum speichern eines BaseUnitCargos");
			return;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<UnitCargoEntry> entries = Common.cast(db.createQuery("from BaseUnitCargoEntry where basis = :dest")
				.setEntity("dest", this.dest)
				.list());

		for(UnitCargoEntry entry: entries)
		{
			db.delete(entry);
		}

		for(UnitCargoEntry entry: units)
		{
			if( entry.getAmount() > 0 )
			{
				db.persist(entry);
			}
		}
	}

	@Override
	protected UnitCargoEntry createUnitCargoEntry(UnitType unitid, long count)
	{
		return new BaseUnitCargoEntry(this.dest, unitid, count);
	}

	@Override
	protected UnitCargo createEmptyCargo()
	{
		return new BaseUnitCargo();
	}

	@Override
	public BaseUnitCargo clone()
	{
		BaseUnitCargo clone = (BaseUnitCargo)super.clone();
		clone.dest = this.dest;
		return clone;
	}
}
