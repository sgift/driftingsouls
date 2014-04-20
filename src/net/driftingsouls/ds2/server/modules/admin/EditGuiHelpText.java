package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AdminMenuEntry(category = "Sonstiges", name = "Hilfetext", permission = WellKnownAdminPermission.EDIT_GUI_HELP_TEXT)
public class EditGuiHelpText implements EntityEditor<GuiHelpText>
{
	@Override
	public Class<GuiHelpText> getEntityType()
	{
		return GuiHelpText.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<GuiHelpText> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.ifAdding().field("Seite", String.class, GuiHelpText::getPage, GuiHelpText::setPage)
				.withOptions(generatePageOptions());
		form.ifUpdating().label("Seite", GuiHelpText::getPage);
		form.textArea("Text", GuiHelpText::getText, GuiHelpText::setText);
	}

	private Map<String, String> generatePageOptions()
	{
		List<GuiHelpText> list = Common.cast(ContextMap.getContext().getDB().createCriteria(GuiHelpText.class).list());
		Set<String> modulesWithHelp = list.stream().map(GuiHelpText::getPage).collect(Collectors.toSet());
		return AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class).stream()
				.map((cls) -> cls.getAnnotation(Module.class).name())
				.filter((s) -> !modulesWithHelp.contains(s))
				.collect(Collectors.toMap((s) -> s, (s) -> s));
	}
}
