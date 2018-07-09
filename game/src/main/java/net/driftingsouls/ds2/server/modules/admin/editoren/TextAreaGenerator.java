package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Generator fuer eine Textarea.
 * @param <V> Der Typ der Entity.
 */
public class TextAreaGenerator<V> implements CustomFieldGenerator<V>
{
	private final String label;
	private final String name;
	private final Function<V,String> getter;
	private final BiConsumer<V,String> setter;

	public TextAreaGenerator(String label, String name, Function<V, String> getter, BiConsumer<V, String> setter)
	{
		this.label = label;
		this.name = name;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void generate(StringBuilder echo, V entity) throws IOException
	{
		String value = getter.apply(entity);
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");
		echo.append("<textarea rows='3' cols='60' name=\"").append(name).append("\">").append(value != null ? value : "").append("</textarea>");
		echo.append("</td></tr>\n");
	}

	@Override
	public void applyRequestValues(Request request, V entity)
	{
		String value = request.getParameterString(this.name);
		setter.accept(entity,value);
	}

	@Override
	public ColumnDefinition<V> getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition<>(name, label, String.class, "textarea");
	}

	@Override
	public String serializedValueOf(V entity)
	{
		String val = this.getter.apply(entity);
		return val != null ? val : "";
	}
}
