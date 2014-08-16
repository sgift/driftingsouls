package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import org.hibernate.Session;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CollectionGenerator<E,T,V extends Collection<T>> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private final Class<?> type;
	private final Function<E, V> getter;
	private final BiConsumer<E, V> setter;
	private final Consumer<FormElementCreator<T>> subFormGenerator;
	private final EditorMode mode;
	private final Class<?> plugin;

	public CollectionGenerator(String label,
			String name,
			Class<?> type,
			Function<E, V> getter,
			BiConsumer<E, V> setter, Consumer<FormElementCreator<T>> subFormGenerator,
			EditorMode mode,
			Class<?> plugin)
	{
		this.label = label;
		this.name = name;
		this.type = type;
		this.getter = getter;
		this.setter = setter;
		this.subFormGenerator = subFormGenerator;
		this.mode = mode;
		this.plugin = plugin;
	}

	@Override
	public void generate(StringBuilder echo, E entity) throws IOException
	{
		V valueCollection = getter.apply(entity);

		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");

		EditorForm8<T> form = new EditorForm8<>(mode, plugin);
		subFormGenerator.accept(form);

		Session db = ContextMap.getContext().getDB();
		JqGridViewModel model = generateTableModel(form, db);

		echo.append("<div class='subTableWrapper'><table id='").append(name).append("'><tr><td></td></tr></table></div>");
		echo.append("<script type='text/javascript'>");
		echo.append("$('#").append(name).append("').jqGrid(").append(new Gson().toJson(model)).append(");\n");

		int rowIdx = 0;
		for (T value : valueCollection)
		{
			List<String> entityValues = form.getEntityValues(value);

			Map<String,String> rowMap = new HashMap<>();
			rowMap.put("id", String.valueOf(db.getIdentifier(value)));

			List<ColumnDefinition<T>> columnDefinitions = form.getColumnDefinitions();
			for (int i = 0; i < columnDefinitions.size(); i++)
			{
				ColumnDefinition columnDefinition = columnDefinitions.get(i);
				rowMap.put(columnDefinition.getId(), entityValues.get(i));
			}

			echo.append("$('#").append(name).append("').jqGrid('addRowData',").append(rowIdx++).append(",").append(new Gson().toJson(rowMap)).append(");\n");
		}

		echo.append("</script>");

		echo.append("</td></tr>\n");
	}

	private JqGridViewModel generateTableModel(EditorForm8<T> form, Session db)
	{
		JqGridViewModel model = new JqGridViewModel();

		model.colNames.add("Id");
		model.colModel.add(new JqGridColumnViewModel("id", null));
		Class identifierClass = db.getSessionFactory().getClassMetadata(this.type).getIdentifierType().getReturnedClass();
		if( Number.class.isAssignableFrom(identifierClass) )
		{
			model.colModel.get(0).width = 50;
		}
		for (ColumnDefinition columnDefinition : form.getColumnDefinitions())
		{
			model.colNames.add(columnDefinition.getLabel());
			model.colModel.add(new JqGridColumnViewModel(columnDefinition.getId(), columnDefinition.getFormatter()));
		}
		return model;
	}

	@Override
	public void applyRequestValues(Request request, E entity) throws IOException
	{

	}

	@Override
	public ColumnDefinition<E> getColumnDefinition()
	{
		return null;
	}

	@Override
	public String serializedValueOf(E entity)
	{
		return null;
	}
}
