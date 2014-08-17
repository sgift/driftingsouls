package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.apache.commons.fileupload.FileItem;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionGenerator<E,T,V extends Collection<T>> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private final Class<T> type;
	private final Function<E, V> getter;
	private final BiConsumer<E, V> setter;
	private final Consumer<FormElementCreator<T>> subFormGenerator;
	private final EditorMode mode;
	private final Class<?> plugin;

	public CollectionGenerator(String label,
			String name,
			Class<T> type,
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
		model.pager = "gridpager";

		echo.append("<div class='subTableWrapper'><table id='").append(name).append("'><tr><td></td></tr></table><div id=\"gridpager\"></div></div>");
		echo.append("<script type='text/javascript'>\n");
		echo.append("Admin.createEditTable('").append(name).append("',").append(new Gson().toJson(model)).append(");\n");

		writeTableData(echo, valueCollection, form, db);

		echo.append("</script>");
		echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id=\"").append(name).append("_data\" />");
		echo.append("</td></tr>\n");
	}

	private void writeTableData(StringBuilder echo, V valueCollection, EditorForm8<T> form, Session db)
	{
		int rowIdx = 0;
		for (T value : valueCollection)
		{
			List<String> entityValues = form.getEntityValues(value);

			Map<String,String> rowMap = new HashMap<>();
			rowMap.put("idx", String.valueOf(rowIdx));
			rowMap.put("id", String.valueOf(db.getIdentifier(value)));

			List<ColumnDefinition<T>> columnDefinitions = form.getColumnDefinitions(false);
			for (int i = 0; i < columnDefinitions.size(); i++)
			{
				ColumnDefinition columnDefinition = columnDefinitions.get(i);
				rowMap.put(columnDefinition.getId(), entityValues.get(i));
			}

			echo.append("$('#").append(name).append("').jqGrid('addRowData',").append(rowIdx++).append(",").append(new Gson().toJson(rowMap)).append(");\n");
		}
	}

	private JqGridViewModel generateTableModel(EditorForm8<T> form, Session db)
	{
		JqGridViewModel model = new JqGridViewModel();

		model.colNames.add("Idx");
		JqGridColumnViewModel idxCol = new JqGridColumnViewModel("idx", null);
		idxCol.key = true;
		idxCol.hidden = true;
		model.colModel.add(idxCol);

		model.colNames.add("Id");
		JqGridColumnViewModel idCol = new JqGridColumnViewModel("id", null);
		model.colModel.add(idCol);

		Class identifierClass = db.getSessionFactory().getClassMetadata(this.type).getIdentifierType().getReturnedClass();
		if( Number.class.isAssignableFrom(identifierClass) )
		{
			idCol.width = 50;
		}
		for (ColumnDefinition columnDefinition : form.getColumnDefinitions(true))
		{
			model.colNames.add(columnDefinition.getLabel());
			JqGridColumnViewModel colViewModel = convertColumnDefinitionToModel(columnDefinition);
			model.colModel.add(colViewModel);
		}
		return model;
	}

	private JqGridColumnViewModel convertColumnDefinitionToModel(ColumnDefinition columnDefinition)
	{
		JqGridColumnViewModel colViewModel = new JqGridColumnViewModel(columnDefinition.getId(), columnDefinition.getFormatter());
		colViewModel.editable = columnDefinition.isEditable();
		colViewModel.edittype = columnDefinition.getEditType();
		Map<String,String> editOptions = columnDefinition.getEditOptions();
		if( !editOptions.isEmpty() )
		{
			String options = editOptions.entrySet().stream()
					.map(entry -> entry.getKey() + ":" + entry.getValue())
					.collect(Collectors.joining(";"));
			colViewModel.editoptions.put("value", options);
			colViewModel.edittype = "select";

		}
		colViewModel.editable = true;
		return colViewModel;
	}

	private static class RowRequestAdapter implements Request {
		private final Request inner;
		private final Map<String,String> parameters;

		private RowRequestAdapter(Request inner, Map<String, String> parameters)
		{
			this.inner = inner;
			this.parameters = parameters;
		}

		@Nonnull
		@Override
		public String[] getParameterValues(@Nonnull String parameter)
		{
			return parameters.containsKey(parameter) ?
					new String[] {parameters.get(parameter)} :
					new String[0];
		}

		@Override
		public String getParameter(String parameter)
		{
			return parameters.get(parameter);
		}

		@Override
		public String getParameterString(String parameter)
		{
			return parameters.containsKey(parameter) ?
					parameters.get(parameter) :
					"";
		}

		@Override
		public int getParameterInt(String parameter)
		{
			try {
				return Integer.parseInt(parameters.get(parameter));
			}
			catch( NumberFormatException e ) {
				return 0;
			}
		}

		@Override
		public void setParameter(String parameter, String value)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType()
		{
			return inner.getContentType();
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public String getQueryString()
		{
			return inner.getQueryString();
		}

		@Override
		public String getPath()
		{
			return inner.getPath();
		}

		@Override
		public String getCharacterEncoding()
		{
			return inner.getCharacterEncoding();
		}

		@Override
		public int getContentLength()
		{
			return inner.getContentLength();
		}

		@Override
		public String getHeader(String header)
		{
			return inner.getHeader(header);
		}

		@Override
		public String getRemoteAddress()
		{
			return inner.getRemoteAddress();
		}

		@Override
		public String getRequestURL()
		{
			return inner.getRequestURL();
		}

		@Override
		public String getUserAgent()
		{
			return inner.getUserAgent();
		}

		@Override
		public List<FileItem> getUploadedFiles()
		{
			return inner.getUploadedFiles();
		}

		@Override
		public <T> T getFromSession(Class<T> cls)
		{
			return inner.getFromSession(cls);
		}

		@Override
		public void removeFromSession(Class<?> cls)
		{
			inner.removeFromSession(cls);
		}

		@Override
		public String getCookie(String name)
		{
			return inner.getCookie(name);
		}

		@Override
		public Map<String, String> getParameterMap()
		{
			return parameters;
		}
	}

	@Override
	public void applyRequestValues(Request request, E entity) throws IOException
	{
		Session db = ContextMap.getContext().getDB();
		Class identifierClass = db.getSessionFactory().getClassMetadata(this.type).getIdentifierType().getReturnedClass();

		EditorForm8<T> form = new EditorForm8<>(mode, plugin);
		subFormGenerator.accept(form);

		V valueCollection = getter.apply(entity);

		String valStr = request.getParameter(this.name);
		if( valStr == null || valStr.isEmpty() ) {
			return;
		}

		List<Map<String,String>> val = new Gson().fromJson(valStr, new TypeToken<List<Map<String,String>>>() {}.getType());

		Set<T> remainingEntries = new HashSet<>(valueCollection);
		for (Map<String, String> row : val)
		{
			Optional<T> backingEntity = findEntityByIdString(db, identifierClass, valueCollection, row.get("id"));
			if( backingEntity.isPresent() ) {
				form.applyRequestValues(new RowRequestAdapter(request, row), backingEntity.get());
				remainingEntries.remove(backingEntity.get());
			}
			else {
				T newEntity = EntityUtils.createEntity(type);
				form.applyRequestValues(new RowRequestAdapter(request, row), newEntity);
				valueCollection.add(newEntity);
				db.persist(newEntity);
			}
		}

		for (T remainingEntry : remainingEntries)
		{
			valueCollection.remove(remainingEntry);
			db.delete(remainingEntry);
		}

		setter.accept(entity, valueCollection);
	}

	private Optional<T> findEntityByIdString(Session db, Class identifierClass, V valueCollection, String idStr)
	{
		if( idStr == null || idStr.isEmpty() )
		{
			return Optional.empty();
		}
		Object id = StringToTypeConverter.convert(identifierClass, idStr);
		return valueCollection.stream().filter(e -> db.getIdentifier(e).equals(id)).findFirst();
	}

	@Override
	public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
	{
		return null;
	}

	@Override
	public String serializedValueOf(E entity)
	{
		return null;
	}
}
