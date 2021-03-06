package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@AdminMenuEntry(category = "Spieler", name = "Rang", permission = WellKnownAdminPermission.EDIT_RANG)
@Component
public class EditRang implements EntityEditor<Rang>
{
	@Override
	public Class<Rang> getEntityType()
	{
		return Rang.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<Rang> form)
	{
		form.allowAdd();

		form.ifAdding().field("Id", Integer.class, Rang::getId, Rang::setId);
		form.ifUpdating().label("Id", Rang::getId);
		form.field("Name", String.class, Rang::getName, Rang::setName);
		form.dynamicContentField("Bild", Rang::getImage, Rang::setImage);
	}
}
