package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.bases.Core;
import net.driftingsouls.ds2.server.bases.DefaultCore;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Forschung;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

/**
 * Editiert die Werte von Cores.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Asteroiden", name = "Core", permission = WellKnownAdminPermission.EDIT_CORE)
public class EditCore implements EntityEditor<Core>
{
	@Override
	public Class<Core> getEntityType()
	{
		return Core.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<Core> form)
	{
		form.allowAdd();
		form.entityClass("Implementierung", DefaultCore.class);
		form.field("Name", String.class, Core::getName, Core::setName);
		form.field("Basistyp", BaseType.class, Core::getAstiType, Core::setAstiType);
		form.field("Arbeiter", Integer.class, Core::getArbeiter, Core::setArbeiter);
		form.field("Energieverbrauch", Integer.class, Core::getEVerbrauch, Core::setEVerbrauch);
		form.field("Energieproduktion", Integer.class, Core::getEProduktion, Core::setEProduktion);
		form.field("EPS", Integer.class, Core::getEPS, Core::setEps);
		form.field("Wohnraum", Integer.class, Core::getBewohner, Core::setBewohner);
		form.field("Auto Abschalten", Boolean.class, Core::isShutDown, Core::setShutDown);
		form.field("Forschung", Forschung.class, Core::getTechRequired, Core::setTechReq).withNullOption("[Keine]");
		form.field("Baukosten", Cargo.class, Core::getBuildCosts, Core::setBuildcosts);
		form.field("Verbrauch", Cargo.class, Core::getConsumes, Core::setConsumes);
		form.field("Produktion", Cargo.class, Core::getProduces, Core::setProduces);
	}
}
