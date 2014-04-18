package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.modules.admin.editoren.AbstractEditPlugin8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AdminMenuEntry(category = "Sonstiges", name = "Hilfetext")
public class EditGuiHelpText extends AbstractEditPlugin8<GuiHelpText>
{
	public EditGuiHelpText()
	{
		super(GuiHelpText.class);
	}

	@Override
	protected void configureFor(@Nonnull EditorForm8<GuiHelpText> form)
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
		List<GuiHelpText> list = Common.cast(getDB().createCriteria(GuiHelpText.class).list());
		Set<String> modulesWithHelp = list.stream().map(GuiHelpText::getPage).collect(Collectors.toSet());
		return AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(Module.class).stream()
				.map((cls) -> cls.getAnnotation(Module.class).name())
				.filter((s) -> !modulesWithHelp.contains(s))
				.collect(Collectors.toMap((s) -> s, (s) -> s));
	}
}
