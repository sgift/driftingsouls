package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsAngebot;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsAngebot_;
import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AdminMenuEntry(category = "Spieler", name = "Fraktionsangebot", permission = WellKnownAdminPermission.EDIT_FRAKTIONS_ANGEBOT)
public class EditFraktionsAngebot implements EntityEditor<FraktionsAngebot>
{

	@Override
	public Class<FraktionsAngebot> getEntityType()
	{
		return FraktionsAngebot.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<FraktionsAngebot> form)
	{
		form.allowAdd();
		form.allowDelete();

		Session db = ContextMap.getContext().getDB();
		List<FraktionsGuiEintrag> gui = Common.cast(db.createQuery("from FraktionsGuiEintrag").list());
		Map<Integer,User> userMap = gui.stream().map(FraktionsGuiEintrag::getUser).collect(Collectors.toMap(User::getId, (User u) -> u));

		form.field("Fraktion", User.class, FraktionsAngebot::getFaction, FraktionsAngebot::setFaction).withOptions(userMap).dbColumn(FraktionsAngebot_.faction);
		form.field("Titel", String.class, FraktionsAngebot::getTitle, FraktionsAngebot::setTitle);
		form.dynamicContentField("Bild", FraktionsAngebot::getImage, FraktionsAngebot::setImage);
		form.textArea("Beschreibung", FraktionsAngebot::getDescription, FraktionsAngebot::setDescription);
	}
}
