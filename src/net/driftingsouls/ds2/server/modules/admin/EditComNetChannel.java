package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.ComNetChannel_;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Sonstiges", name = "ComNet Kanal", permission = WellKnownAdminPermission.EDIT_COMNET_CHANNEL)
public class EditComNetChannel implements EntityEditor<ComNetChannel>
{
	@Override
	public Class<ComNetChannel> getEntityType()
	{
		return ComNetChannel.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<ComNetChannel> form)
	{
		form.field("Name", String.class, ComNetChannel::getName, ComNetChannel::setName);
		form.field("Allianz (Besitzer)", Ally.class, ComNetChannel::getAllyOwner, ComNetChannel::setAllyOwner).withNullOption("[Kein Besitzer]").dbColumn(ComNetChannel_.allyOwner);
		form.field("Alle schreiben", Boolean.class, ComNetChannel::isWriteAll, ComNetChannel::setWriteAll);
		form.field("Alle lesen", Boolean.class, ComNetChannel::isReadAll, ComNetChannel::setReadAll);
		form.field("NPCs schreiben", Boolean.class, ComNetChannel::isWriteNpc, ComNetChannel::setWriteNpc);
		form.field("NPCs lesen", Boolean.class, ComNetChannel::isReadNpc, ComNetChannel::setReadNpc);
		form.field("Allianz schreiben", Ally.class, ComNetChannel::getWriteAlly, ComNetChannel::setWriteAlly).withNullOption("[Keine]");
		form.field("Allianz lesen", Ally.class, ComNetChannel::getReadAlly, ComNetChannel::setReadAlly).withNullOption("[Keine]");
		form.multiSelection("Spieler schreiben", User.class, ComNetChannel::getWritePlayer, ComNetChannel::setWritePlayer);
		form.multiSelection("Spieler lesen", User.class, ComNetChannel::getReadPlayer, ComNetChannel::setReadPlayer);
	}
}
