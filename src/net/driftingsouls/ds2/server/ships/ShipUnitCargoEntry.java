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
	/**
	 * Default.
	 */
	public ShipUnitCargoEntry()
	{}
	
	/**
	 * @see UnitCargoEntry#UnitCargoEntry(int, int, int, long)
	 * @param type Der Typ des Eintrages
	 * @param destid Die ID des Zielobjekts
	 * @param unittype Die ID des Einheitentyps
	 * @param amount Die Menge
	 */
	public ShipUnitCargoEntry(int type, int destid, int unittype, long amount)
	{
		super(type, destid, unittype, amount);
	}
}
