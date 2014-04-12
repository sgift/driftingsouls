package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.entities.fraktionsgui.FraktionsGuiEintrag;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Spieler", name = "Fraktions-GUI")
public class EditFraktionsGuiEintrag extends AbstractEditPlugin8<FraktionsGuiEintrag>
{
	public EditFraktionsGuiEintrag()
	{
		super(FraktionsGuiEintrag.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<FraktionsGuiEintrag> form)
	{
		form.allowAdd();
		form.field("Spieler", User.class, FraktionsGuiEintrag::getUser, FraktionsGuiEintrag::setUser);
		form.multiSelection("Seiten", FraktionsGuiEintrag.Seite.class, FraktionsGuiEintrag::getSeiten, FraktionsGuiEintrag::setSeiten);
		form.textArea("Text", FraktionsGuiEintrag::getText, FraktionsGuiEintrag::setText);
	}
}
