package net.driftingsouls.ds2.server.units;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipUnitCargoEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Repraesentation eines UnitCargos auf einem Schiff.
 */
public class ShipUnitCargo extends UnitCargo
{
	private static final Log LOG = LogFactory.getLog(ShipUnitCargo.class);

	private Ship dest;

	/**
	 * Erzeugt einen leeren Unitcargo.
	 */
	public ShipUnitCargo()
	{
	}

	/**
	 * Erzeugt einen Unitcargo mit dem gegebenen Inhalt.
	 * @param unitcargo Der Inhalt
	 */
	public ShipUnitCargo(UnitCargo unitcargo)
	{
		super(unitcargo);
	}

	/**
	 * Zeugt einen Unitcargo mit dem gegebenen Inhalt fuer das gegebene Schiff.
	 * @param units Der Inahlt
	 * @param dest Das Schiff
	 */
	public ShipUnitCargo(List<UnitCargoEntry> units, Ship dest)
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
			LOG.warn("Nicht genug Daten zum speichern eines ShipUnitCargos");
			return;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		List<UnitCargoEntry> entries = Common.cast(db.createQuery("from ShipUnitCargoEntry where schiff = :dest")
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
		return new ShipUnitCargoEntry(this.dest, unitid, count);
	}

	@Override
	protected UnitCargo createEmptyCargo()
	{
		return new ShipUnitCargo();
	}

	@Override
	public ShipUnitCargo clone()
	{
		ShipUnitCargo clone = (ShipUnitCargo)super.clone();
		clone.dest = this.dest;
		return clone;
	}
}
