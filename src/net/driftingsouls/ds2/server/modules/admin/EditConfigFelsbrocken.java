package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.ships.ShipType;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Systeme", name="Felsbrocken-Layout")
public class EditConfigFelsbrocken extends AbstractEditPlugin8<ConfigFelsbrocken>
{
	public EditConfigFelsbrocken()
	{
		super(ConfigFelsbrocken.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<ConfigFelsbrocken> form)
	{
		form.allowAdd();
		form.field("System mit Felsbrocken", ConfigFelsbrockenSystem.class, ConfigFelsbrocken::getSystem, ConfigFelsbrocken::setSystem);
		form.field("Schiffstyp", ShipType.class, ConfigFelsbrocken::getShiptype, ConfigFelsbrocken::setShiptype);
		form.field("Wahrscheinlichkeitsfaktor", Integer.class, ConfigFelsbrocken::getChance, ConfigFelsbrocken::setChance);
		form.field("Cargo", Cargo.class, ConfigFelsbrocken::getCargo, ConfigFelsbrocken::setCargo);
	}
}
