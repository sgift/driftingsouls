package net.driftingsouls.ds2.server.units;

/**
 * Ein Eintrag in einem nicht persistierbaren UnitCargo.
 */
public class TransientUnitCargoEntry extends UnitCargoEntry
{

	/**
	 * Default.
	 */
	public TransientUnitCargoEntry()
	{}

	/**
	 * @param unittype Der Einheitentyp
	 * @param amount Die Menge
	 */
	public TransientUnitCargoEntry(UnitType unittype, long amount)
	{
		super(unittype, amount);
	}

	@Override
	public UnitCargoEntry createCopy()
	{
		return new TransientUnitCargoEntry(getUnitType(),getAmount());
	}
}
