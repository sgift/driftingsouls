package net.driftingsouls.ds2.server.modules.admin.editoren;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.Common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Hilfsfunktionen fuer die Generierung von HTML-Code.
 */
public final class HtmlUtils
{
	private HtmlUtils()
	{
		// EMPTY
	}

	/**
	 * Generiert das ViewModel zu einem HTML-Select.
	 * @param name Der Formname des Elements, gleichzeitig auch die ID
	 * @param readOnly <code>true</code>, falls das Element readonly sein soll
	 * @param options Die Auswahloptionen des Selects, Key ist der interne Wert, Value der Anzeigewert
	 * @param selected Der momentan ausgewaehlte Wert (interner Wert)
	 * @return Das ViewModel
	 */
	public static SelectViewModel jsSelect(String name, boolean readOnly, Map<Serializable,Object> options, Serializable selected)
	{
		SelectViewModel model = new SelectViewModel(name);
		model.disabled = readOnly;
		model.selected = selected != null ? identifierToString(selected) : null;

		// TreeMap mag keine null-Keys
		Map<Serializable,Object> optionsIntrl = new HashMap<>(options);
		if( optionsIntrl.containsKey(null) )
		{
			Object label = optionsIntrl.remove(null);
			model.nullOption = label instanceof String ? (String)label : new ObjectLabelGenerator().generateFor(null, label);
		}

		for (Map.Entry<Serializable, Object> entry : new TreeMap<>(optionsIntrl).entrySet())
		{
			String label = objectLabelToString(entry.getKey(), entry.getValue());

			model.options.put(identifierToString(entry.getKey()), label);
		}
		return model;
	}

	/**
	 * Generiert ein HTML-Select.
	 * @param echo Der Buffer in den der HTML-Code geschrieben werden soll
	 * @param name Der Formname des Elements, gleichzeitig auch die ID
	 * @param readOnly <code>true</code>, falls das Element readonly sein soll
	 * @param options Die Auswahloptionen des Selects, Key ist der interne Wert, Value der Anzeigewert
	 * @param selected Der momentan ausgewaehlte Wert (interner Wert)
	 */
	public static void select(StringBuilder echo, String name, boolean readOnly, Map<Serializable,Object> options, Serializable selected)
	{
		echo.append("<select size=\"1\" ").append(readOnly ? "disabled=\"disabled\" " : "").append("name=\"").append(name).append("\">");

		// TreeMap mag keine null-Keys
		Map<Serializable,Object> optionsIntrl = new HashMap<>(options);
		if( optionsIntrl.containsKey(null) )
		{
			option(echo, selected, null, optionsIntrl.get(null));
			optionsIntrl.remove(null);
		}

		for (Map.Entry<Serializable, Object> entry : new TreeMap<>(optionsIntrl).entrySet())
		{
			option(echo, selected, entry.getKey(), entry.getValue());
		}

		echo.append("</select>");
	}

	private static void option(StringBuilder echo, Serializable selected, Serializable identifier, Object value)
	{
		echo.append("<option ");
		echo.append("value=\"").append(identifierToString(identifier)).append("\"");
		if ((identifier == null && selected == null) || (identifier != null && identifier.equals(selected)))
		{
			echo.append(" selected=\"selected\"");
		}
		String label;
		label = objectLabelToString(identifier, value);
		echo.append(">").append(label).append("</option>");
	}

	public static String objectLabelToString(Serializable identifier, Object value)
	{
		String label;
		if (value instanceof String || identifier == value)
		{
			label = value != null ? value.toString() : "";
		}
		else
		{
			label = new ObjectLabelGenerator().generateFor(identifier, value);
		}
		return label;
	}

	public static String identifierToString(Serializable identifier)
	{
		if( identifier instanceof Enum )
		{
			return ((Enum)identifier).name();
		}
		return identifier != null ? identifier.toString() : "";
	}

	/**
	 * Generiert ein <code>&lt;input type="text"/&gt;</code>-Element.
	 * @param echo Der Buffer in den der HTML-Code geschrieben werden soll
	 * @param name Der Formname des Elements, gleichzeitig auch die ID
	 * @param readOnly <code>true</code>, falls das Element readonly sein soll
	 * @param dataType Der Datentyp des hinterlegten Werts, wird u.a. zur Generierung von clientseitiger Validierungslogik verwendet
	 * @param value Der momentane Wert oder <code>null</code>
	 */
	public static void textInput(StringBuilder echo, String name, boolean readOnly, Class<?> dataType, Object value)
	{
		echo.append("<input type=\"text\" ").append("id=\"").append(name).append("\" ").append(readOnly ? "disabled=\"disabled\" " : "").append("name=\"").append(name).append("\" value=\"").append(value != null ? Common.escapeHTML(value.toString()) : "").append("\" />");
		if (Number.class.isAssignableFrom(dataType))
		{
			writeAutoNumberJavaScript(echo, name, dataType);
		}
	}

	private static void writeAutoNumberJavaScript(StringBuilder echo, String name, Class<?> dataType)
	{
		AutoNumericViewModel model = AutoNumericViewModel.forClass(dataType);
		echo.append("<script type=\"text/javascript\">\n");
		echo.append("$('#").append(name).append("').autoNumeric('init', ").append(new Gson().toJson(model)).append(");\n");
		echo.append("</script>");
	}

	/**
	 * Generiert ein ViewModel zu einem <code>&lt;input type="text"/&gt;</code>-Element.
	 * @param name Der Formname des Elements, gleichzeitig auch die ID
	 * @param readOnly <code>true</code>, falls das Element readonly sein soll
	 * @param dataType Der Datentyp des hinterlegten Werts, wird u.a. zur Generierung von clientseitiger Validierungslogik verwendet
	 * @param value Der momentane Wert oder <code>null</code>
	 * @return Das ViewModel
	 */
	public static TextFieldViewModel jsTextInput(String name, boolean readOnly, Class<?> dataType, Object value)
	{
		TextFieldViewModel model = new TextFieldViewModel(name);
		model.id = name;
		model.disabled = readOnly;
		model.value = value != null ? value.toString() : "";
		model.autoNumeric = AutoNumericViewModel.forClass(dataType);
		return model;
	}
}
