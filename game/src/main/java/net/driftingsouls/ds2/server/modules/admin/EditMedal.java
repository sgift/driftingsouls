package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@AdminMenuEntry(category = "Spieler", name = "Orden", permission = WellKnownAdminPermission.EDIT_MEDAL)
@Component
public class EditMedal implements EntityEditor<Medal>
{
	@Override
	public Class<Medal> getEntityType()
	{
		return Medal.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<Medal> form)
	{
		form.allowAdd();
		form.field("Name", String.class, Medal::getName, Medal::setName);
		form.field("Nur Admins", Boolean.class, Medal::isAdminOnly, Medal::setAdminOnly);
		form.dynamicContentField("Grafik (normal)", Medal::getImage, Medal::setImage);
		form.dynamicContentField("Grafik (icon)", Medal::getImageSmall, Medal::setImageSmall);
	}
}
