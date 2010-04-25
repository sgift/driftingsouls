package net.driftingsouls.ds2.server.bases;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;

import net.driftingsouls.ds2.server.units.UnitCargo;
import net.driftingsouls.ds2.server.units.UnitCargoEntry;

/**
 * A unit cargo entry for bases.
 * The cargo entry filters all ship related cargos from results.
 * 
 * @author Sebastian Gift
 */
@Entity
@Table(name="cargo_entries_units")
@DiscriminatorColumn(name="type")
@DiscriminatorValue(value="" + UnitCargo.CARGO_ENTRY_BASE)
public class BaseUnitCargoEntry extends UnitCargoEntry 
{}
