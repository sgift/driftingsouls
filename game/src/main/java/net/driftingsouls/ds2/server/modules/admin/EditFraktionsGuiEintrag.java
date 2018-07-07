package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FactionShopEntry;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nonnull;
import java.util.List;

@AdminMenuEntry(category = "Spieler", name = "Fraktions-GUI", permission = WellKnownAdminPermission.EDIT_FRAKTIONS_GUI_EINTRAG)
public class EditFraktionsGuiEintrag implements EntityEditor<FraktionsGuiEintrag>
{
	@Override
	public Class<FraktionsGuiEintrag> getEntityType()
	{
		return FraktionsGuiEintrag.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<FraktionsGuiEintrag> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.preDeleteTask("Angebote im Shop entfernen", (entity) -> {
			Session db = ContextMap.getContext().getDB();
			List<FactionShopEntry> entries = Common.cast(db.createCriteria(FactionShopEntry.class).add(Restrictions.eq("faction", entity.getUser())).list());
			entries.forEach(db::delete);
		});
		form.field("Spieler", User.class, FraktionsGuiEintrag::getUser, FraktionsGuiEintrag::setUser);
		form.multiSelection("Seiten", FraktionsGuiEintrag.Seite.class, FraktionsGuiEintrag::getSeiten, FraktionsGuiEintrag::setSeiten);
		form.textArea("Text", FraktionsGuiEintrag::getText, FraktionsGuiEintrag::setText);
	}
}
