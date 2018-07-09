package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.pipeline.Request;

import javax.persistence.Entity;
import java.io.IOException;
import java.util.function.Function;

/**
 * Generator fuer ein Anzeigelabel.
 * @param <V> Der Entitytyp
 * @param <T> Der Datentyp des Labels
 */
public class LabelGenerator<V, T> implements CustomFieldGenerator<V>
{
	private final String id;
	private final String label;
	private final Function<V,T> getter;

	public LabelGenerator(String id, String label, Function<V, T> getter)
	{
		this.id = id;
		this.label = label;
		this.getter = getter;
	}

	@Override
	public void generate(StringBuilder echo, V entity) throws IOException
	{
		String valueStr = serializedValueOf(entity);
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>").append("<td>").append(valueStr).append("</td></tr>\n");
	}

	@Override
	public void applyRequestValues(Request request, V entity)
	{
	}

	@Override
	public ColumnDefinition getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition(id, label, String.class);
	}

	@Override
	public String serializedValueOf(V entity)
	{
		T value = this.getter.apply(entity);
		String valueStr;
		if( value != null && value.getClass().isAnnotationPresent(Entity.class) )
		{
			valueStr = new ObjectLabelGenerator().generateFor(null, value);
		}
		else
		{
			valueStr = value != null ? value.toString() : "";
		}
		return valueStr;
	}
}
