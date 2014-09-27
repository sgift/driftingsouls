package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.hibernate.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapGenerator<E, KT,VT, V extends Map<KT,VT>> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private final Class<KT> keyType;
	private final Class<VT> valueType;
	private final Function<E, V> getter;
	private final BiConsumer<E, V> setter;
	private final Consumer<FormElementCreator<MapEntryRef<KT,VT>>> subFormGenerator;
	private final EditorMode mode;
	private final Class<?> plugin;

	public MapGenerator(String label,
						String name,
						Class<KT> keyType,
						Class<VT> valueType,
						Function<E, V> getter,
						BiConsumer<E, V> setter, Consumer<FormElementCreator<MapEntryRef<KT,VT>>> subFormGenerator,
						EditorMode mode,
						Class<?> plugin)
	{
		this.label = label;
		this.name = name;
		this.keyType = keyType;
		this.valueType = valueType;
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

		EditorForm8<MapEntryRef<KT,VT>> form = new EditorForm8<>(mode, plugin);
		subFormGenerator.accept(form);

		JqGridViewModel model = generateTableModel(form);
		model.pager = "gridpager";

		echo.append("<div class='subTableWrapper'><table id='").append(name).append("'><tr><td></td></tr></table><div id=\"gridpager\"></div></div>");
		echo.append("<script type='text/javascript'>\n");
		CollectionEditorUtils.writeGridModelJs(echo, name, model);

		writeTableData(echo, valueCollection, form);

		echo.append("</script>");
		echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id=\"").append(name).append("_data\" />");
		echo.append("</td></tr>\n");
	}

	private void writeTableData(StringBuilder echo, V valueCollection, EditorForm8<MapEntryRef<KT, VT>> form)
	{
		int rowIdx = 0;
		for (Map.Entry<KT,VT> value : valueCollection.entrySet())
		{
			List<String> entityValues = form.getEntityValues(new MapEntryRef<>(value));

			Map<String,String> rowMap = new HashMap<>();
			rowMap.put("idx", String.valueOf(rowIdx));
			rowMap.put("id", String.valueOf(value.getKey()));

			CollectionEditorUtils.writeRowDataToRowMap(rowMap, form, entityValues);
			CollectionEditorUtils.writeRowMapJS(echo, name, rowIdx++, rowMap);
		}
	}

	private JqGridViewModel generateTableModel(EditorForm8<MapEntryRef<KT, VT>> form)
	{
		JqGridViewModel model = new JqGridViewModel();

		model.colNames.add("Idx");
		JqGridColumnViewModel idxCol = new JqGridColumnViewModel("idx", null);
		idxCol.key = true;
		idxCol.hidden = true;
		model.colModel.add(idxCol);

		model.colNames.add("Id");
		JqGridColumnViewModel idCol = new JqGridColumnViewModel("id", null);
		idCol.hidden = true;
		model.colModel.add(idCol);

		CollectionEditorUtils.addColumnModelsOfForm(model, form);

		return model;
	}

	@Override
	public void applyRequestValues(Request request, E entity) throws IOException
	{
		Session db = ContextMap.getContext().getDB();

		EditorForm8<MapEntryRef<KT,VT>> form = new EditorForm8<>(mode, plugin);
		subFormGenerator.accept(form);

		V map = getter.apply(entity);

		String valStr = request.getParameter(this.name);
		if( valStr == null || valStr.isEmpty() ) {
			return;
		}

		List<Map<String,String>> val = new Gson().fromJson(valStr, new TypeToken<List<Map<String,String>>>() {}.getType());
		Set<KT> oldKeys = new HashSet<>(map.keySet());
		for (Map<String, String> row : val)
		{
			KT mapKey = row.get("id") != null && !row.get("id").isEmpty() ? StringToTypeConverter.convert(keyType, row.get("id")) : null;
			VT value = mapKey != null ? map.get(mapKey) : null;
			if( value != null) {
				oldKeys.remove(mapKey);
				MapEntryRef<KT,VT> ref = new MapEntryRef<>(mapKey, value);
				form.applyRequestValues(new RowRequestAdapter(request, row), ref);
				if( !mapKey.equals(ref.getKey()) )
				{
					map.remove(mapKey);
				}
				map.put(ref.getKey(), ref.getValue());
			}
			else {
				VT newEntity = EntityUtils.createEntity(valueType);
				MapEntryRef<KT,VT> ref = new MapEntryRef<>(mapKey, newEntity);
				form.applyRequestValues(new RowRequestAdapter(request, row), ref);
				map.put(ref.getKey(), ref.getValue());
				db.persist(newEntity);
			}
		}

		oldKeys.forEach(map::remove);

		setter.accept(entity, map);
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
