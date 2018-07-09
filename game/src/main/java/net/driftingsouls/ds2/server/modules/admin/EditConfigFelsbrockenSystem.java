package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Systeme", name="Felsbrocken-System", permission = WellKnownAdminPermission.EDIT_CONFIG_FELSBROCKEN_SYSTEM)
public class EditConfigFelsbrockenSystem implements EntityEditor<ConfigFelsbrockenSystem>
{
	@Override
	public Class<ConfigFelsbrockenSystem> getEntityType()
	{
		return ConfigFelsbrockenSystem.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<ConfigFelsbrockenSystem> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.ifUpdating().label("Sternensystem", ConfigFelsbrockenSystem::getSystem);
		form.ifAdding().field("Sternensystem", StarSystem.class, ConfigFelsbrockenSystem::getSystem, ConfigFelsbrockenSystem::setSystem);
		form.field("Name der Felsbrocken", String.class, ConfigFelsbrockenSystem::getName, ConfigFelsbrockenSystem::setName);
        form.field("Anzahl Felsbrocken", Integer.class, ConfigFelsbrockenSystem::getCount, ConfigFelsbrockenSystem::setCount);
		form.label("Anzahl Layouts", (t) -> t.getFelsbrocken().size());
	}
}
