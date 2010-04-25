package net.driftingsouls.ds2.server.ships;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;

/**
 * A unit cargo entry for ships.
 * The cargo entry filters all base related cargos from results.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="cargo_entries_units")
@DiscriminatorColumn(name="type")
@DiscriminatorValue(value="" + UnitCargo.CARGO_ENTRY_SHIP)
public class ShipUnitCargoEntry extends UnitCargoEntry 
{}
