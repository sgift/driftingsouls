package net.driftingsouls.ds2.server.modules.admin;

import io.github.classgraph.AnnotationInfo;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.entities.GuiHelpText;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@AdminMenuEntry(category = "Sonstiges", name = "Hilfetext", permission = WellKnownAdminPermission.EDIT_GUI_HELP_TEXT)
@Component
public class EditGuiHelpText implements EntityEditor<GuiHelpText>
{
	@Override
	public Class<GuiHelpText> getEntityType()
	{
		return GuiHelpText.class;
	}

	@Override
	public void configureFor(@NonNull EditorForm8<GuiHelpText> form)
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
		try(var scanResult = AnnotationUtils.INSTANCE.scanDsClasses()) {
			return scanResult.getClassesWithAnnotation(Module.class.getName()).stream()
				.map(classInfo -> classInfo.getAnnotationInfo(Module.class.getName()))
				.map(AnnotationInfo::getParameterValues)
				.map(parameterValues -> parameterValues.getValue("name"))
				.map(Object::toString)
				.filter(s -> !modulesWithHelp.contains(s))
				.collect(toMap(s -> s, s -> s));
		}
	}
}
