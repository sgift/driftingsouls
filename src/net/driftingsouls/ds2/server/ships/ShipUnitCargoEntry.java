package net.driftingsouls.ds2.server.ships;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;

/**
 * A unit cargo entry for ships.
 * The cargo entry filters all base related cargos from results.
 * 
 * @author Drifting-Souls Team
 */
@Entity
@Table(name="cargo_entries_units")
@DiscriminatorValue("" + UnitCargo.CARGO_ENTRY_SHIP)
public class ShipUnitCargoEntry extends UnitCargoEntry 
{
	public ShipUnitCargoEntry()
	{}
	
	public ShipUnitCargoEntry(int type, int destid, int unittype, long amount)
	{
		super(type, destid, unittype, amount);
	}
}
