package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generator fuer die Auswahl der Entity-Klasse.
 * @param <E> Der Typ der Entity
 */
public class EntityClassGenerator<E> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private boolean editierbar;
	private Class<? extends E> currentEntityClass;
	private final List<Class<?>> options;

	public EntityClassGenerator(String label, String name, boolean editierbar, Class<? extends E> currentEntityClass, Class<?> ... options)
	{
		this.label = label;
		this.name = name;
		this.editierbar = editierbar;
		this.currentEntityClass = currentEntityClass;
		this.options = new ArrayList<>(Arrays.asList(options));
		this.options.add(0, currentEntityClass);
	}

	private String displayName(Class<?> cls)
	{
		String clsName = cls.getName();
		if( !clsName.startsWith("net.driftingsouls.ds2.server.") )
		{
			return clsName;
		}
		return clsName.substring("net.driftingsouls.ds2.server.".length());
	}

	public Class<? extends E> getCurrentEntityClass()
	{
		return this.currentEntityClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void generate(StringBuilder echo, E entity) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");

		Class<? extends E> aClass = (Class<? extends E>) entity.getClass();
		if( editierbar && this.options.size() > 1 )
		{
			editEntityBySelection(echo, name, aClass);
		}
		else
		{
			echo.append(displayName(aClass));
		}

		echo.append("</td></tr>\n");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void applyRequestValues(Request request, E entity)
	{
		if (!editierbar)
		{
			return;
		}
		String val = request.getParameter(this.name);
		if (val == null)
		{
			return;
		}

		try
		{
			this.currentEntityClass = (Class<? extends E>) Class.forName(val);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalArgumentException("Konnte Entity-Klasse nicht finden", e);
		}
	}

	@Override
	public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition<>(name, label, Class.class);
	}

	@Override
	public String serializedValueOf(E entity)
	{
		return displayName(entity.getClass());
	}

	private void editEntityBySelection(StringBuilder echo, String name, Class<? extends E> value) throws IOException
	{
		Serializable selected = value.getName();
		Map<Serializable,Object> selectionOptions = this.options.stream().collect(Collectors.toMap(Class::getName, this::displayName));

		HtmlUtils.select(echo, name, false, selectionOptions, selected);
	}
}
