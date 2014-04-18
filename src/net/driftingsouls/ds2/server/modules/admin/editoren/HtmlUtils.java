package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.io.Serializable;
import java.math.BigDecimal;
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
	 * Generiert ein HTML-Select.
	 * @param echo Der Buffer in den der HTML-Code geschrieben werden soll
	 * @param name Der Formname des Elements, gleichzeitig auch die ID
	 * @param readOnly <code>true</code>, falls das Element readonly sein soll
	 * @param options Die Auswahloptionen des Selects, Key ist der interne Wert, Value der Anzeigewert
	 * @param selected Der momentan ausgewaehlte Wert (interner Wert)
	 */
	public static void select(StringBuilder echo, String name, boolean readOnly, Map<Serializable,Object> options, Serializable selected)
	{
		echo.append("<select size=\"1\" ").append(readOnly ? "disabled='disabled' " : "").append("name=\"").append(name).append("\">");

		for (Map.Entry<Serializable, Object> entry : new TreeMap<>(options).entrySet())
		{
			Serializable identifier = entry.getKey();
			echo.append("<option ");
			echo.append(" value=\"").append(identifier != null ? identifier.toString() : "").append("\"");
			if ((identifier == null && selected == null) || (identifier != null && identifier.equals(selected)))
			{
				echo.append(" selected=\"selected\"");
			}
			String label;
			if (entry.getValue() instanceof String || entry.getKey() == entry.getValue())
			{
				label = entry.getValue() != null ? entry.getValue().toString() : "";
			}
			else
			{
				label = new ObjectLabelGenerator().generateFor(identifier, entry.getValue());
			}
			echo.append(">").append(label).append("</option>");
		}

		echo.append("</select>");
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
		echo.append("<input type=\"text\" ").append("id=\"").append(name).append("\" ").append(readOnly ? "disable='disabled' " : "").append("name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
		if (Number.class.isAssignableFrom(dataType))
		{
			writeAutoNumberJavaScript(echo, name, dataType);
		}
	}

	private static void writeAutoNumberJavaScript(StringBuilder echo, String name, Class<?> dataType)
	{
		int mDec = 0;
		Number minValue = -999999999.99;
		Number maxValue = 999999999.99;
		if (dataType == Double.class || dataType == Float.class || dataType == BigDecimal.class)
		{
			mDec = 8;
		}
		if (dataType == Integer.class)
		{
			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
		}
		else if (dataType == Long.class)
		{
			minValue = Long.MIN_VALUE;
			maxValue = Long.MAX_VALUE;
		}
		else if (dataType == Short.class)
		{
			minValue = Short.MIN_VALUE;
			maxValue = Short.MAX_VALUE;
		}
		else if (dataType == Byte.class)
		{
			minValue = Byte.MIN_VALUE;
			maxValue = Byte.MAX_VALUE;
		}

		echo.append("<script type=\"text/javascript\">\n");
		echo.append("$('#").append(name).append("').autoNumeric('init', {aSep:'', vMin:").append(minValue.toString())
				.append(", vMax:").append(maxValue.toString())
				.append(", lZero: 'deny', mDec:").append(Integer.toString(mDec)).append("});\n");
		echo.append("</script>");
	}
}
