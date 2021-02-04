package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.ModuleSlot;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@AdminMenuEntry(category = "Schiffe", name = "Modulslot", permission = WellKnownAdminPermission.EDIT_MODULE_SLOT)
@Component
public class EditModuleSlot implements EntityEditor<ModuleSlot>
{

	@Override
	public Class<ModuleSlot> getEntityType()
	{
		return ModuleSlot.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<ModuleSlot> form)
	{
		form.allowAdd();
		form.ifAdding().field("Slot-ID", String.class, ModuleSlot::getSlotType, ModuleSlot::setSlotType);
		form.ifUpdating().label("Slot-ID", ModuleSlot::getSlotType);
		form.field("Name", String.class, ModuleSlot::getName, ModuleSlot::setName);
		form.field("Eltern-Slot", ModuleSlot.class, ModuleSlot::getParent, ModuleSlot::setParent).withNullOption("[Keiner]");
	}
}
