package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CollectionEditorUtils
{
	private CollectionEditorUtils()
	{
		// EMPTY
	}

	public static JqGridColumnViewModel convertColumnDefinitionToModel(ColumnDefinition columnDefinition)
	{
		JqGridColumnViewModel colViewModel = new JqGridColumnViewModel(columnDefinition.getId(), columnDefinition.getFormatter());
		colViewModel.editable = columnDefinition.isEditable();
		colViewModel.edittype = columnDefinition.getEditType();
		Map<String,String> editOptions = columnDefinition.getEditOptions();
		if( !editOptions.isEmpty() )
		{
			String options = editOptions.entrySet().stream()
					.map(entry -> entry.getKey() + ":" + safeDropDownOption(entry.getValue()))
					.collect(Collectors.joining(";"));
			colViewModel.editoptions.put("value", options);
			colViewModel.edittype = "select";

		}
		colViewModel.editable = true;
		return colViewModel;
	}

	public static <T> void writeRowDataToRowMap(Map<String,String> rowMap, EditorForm8<T> form, List<String> entityValues)
	{
		List<ColumnDefinition<T>> columnDefinitions = form.getColumnDefinitions(false);
		List<ColumnDefinition<T>> columnDefinitionsForEditing = form.getColumnDefinitions(true);
		for (int i = 0; i < columnDefinitions.size(); i++)
		{
			ColumnDefinition columnDefinition = columnDefinitions.get(i);
			ColumnDefinition columnDefinitionForEditing = columnDefinitionsForEditing.get(i);
			if (!columnDefinitionForEditing.getEditOptions().isEmpty())
			{
				rowMap.put(columnDefinition.getId(), CollectionEditorUtils.safeDropDownOption(entityValues.get(i)));
			}
			else
			{
				rowMap.put(columnDefinition.getId(), entityValues.get(i));
			}
		}
	}

	public static String safeDropDownOption(String label) {
		return label.replace(':', ' ').replace(';', ' ');
	}

	public static void writeRowMapJS(StringBuilder echo, String name, int rowIdx, Map<String,String> rowMap) {
		echo.append("$('#").append(name).append("').jqGrid('addRowData',").append(rowIdx).append(",").append(new Gson().toJson(rowMap)).append(");\n");
	}

	public static <T> void addColumnModelsOfForm(JqGridViewModel model, EditorForm8<T> form) {
		for (ColumnDefinition columnDefinition : form.getColumnDefinitions(true))
		{
			model.colNames.add(columnDefinition.getLabel());
			JqGridColumnViewModel colViewModel = CollectionEditorUtils.convertColumnDefinitionToModel(columnDefinition);
			model.colModel.add(colViewModel);
		}
	}

	public static void writeGridModelJs(StringBuilder echo, String name, JqGridViewModel model) {
		echo.append("Admin.createEditTable('").append(name).append("',").append(new Gson().toJson(model)).append(");\n");
	}
}
