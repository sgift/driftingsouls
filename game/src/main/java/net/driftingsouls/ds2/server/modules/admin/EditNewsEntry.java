package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;

@AdminMenuEntry(category = "Portal", name="News", permission = WellKnownAdminPermission.EDIT_NEWS_ENTRY)
public class EditNewsEntry implements EntityEditor<NewsEntry>
{
	@Override
	public Class<NewsEntry> getEntityType()
	{
		return NewsEntry.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<NewsEntry> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.field("Titel", String.class, NewsEntry::getTitle, NewsEntry::setTitle);
		form.field("Autor", String.class, NewsEntry::getAuthor, NewsEntry::setAuthor);
		form.field("Zeitpunkt (in Unixtime)", Long.class, NewsEntry::getDate, NewsEntry::setDate);
		form.textArea("Text", NewsEntry::getNewsText, NewsEntry::setNewsText);
		form.textArea("Titel", NewsEntry::getShortDescription, NewsEntry::setShortDescription);
	}
}
