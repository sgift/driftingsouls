package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import java.io.IOException;
import java.util.function.Function;

/**
 * Generator fuer ein Anzeigebild.
 * @param <E> Der Entitytyp
 */
public class PictureGenerator<E> implements CustomFieldGenerator<E>
{
	private final String id;
	private final String label;
	private final Function<E,String> getter;

	public PictureGenerator(String id, String label, Function<E, String> getter)
	{
		this.id = id;
		this.label = label;
		this.getter = getter;
	}

	@Override
	public void generate(StringBuilder echo, E entity) throws IOException
	{
		String value = getter.apply(entity);
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>");
		if( value != null )
		{
			echo.append("<img src=\"").append(value).append("\" alt=\"\"/>");
		}
		echo.append("</td></tr>\n");
	}

	@Override
	public void applyRequestValues(Request request, E entity)
	{
	}

	@Override
	public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition<>(id, label, String.class, "picture");
	}

	@Override
	public String serializedValueOf(E entity)
	{
		return getter.apply(entity);
	}
}
