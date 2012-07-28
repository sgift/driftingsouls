package net.driftingsouls.ds2.server.bases;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;
import net.driftingsouls.ds2.server.units.UnitType;

/**
 * A unit cargo entry for bases.
 * The cargo entry filters all ship related cargos from results.
 *
 * @author Drifting-Souls Team
 */
@Entity
@DiscriminatorValue("" + UnitCargo.CARGO_ENTRY_BASE)
public class BaseUnitCargoEntry extends UnitCargoEntry
{
	/**
	 * Default.
	 */
	public BaseUnitCargoEntry()
	{}

	/**
	 * @see UnitCargoEntry#UnitCargoEntry(int, int, int, long)
	 * @param type Der Typ des Eintrages
	 * @param destid Die ID des Zielobjekts
	 * @param unittype Der Einheitentyp
	 * @param amount Die Menge
	 */
	public BaseUnitCargoEntry(int type, int destid, UnitType unittype, long amount)
	{
		super(type, destid, unittype, amount);
	}
}
