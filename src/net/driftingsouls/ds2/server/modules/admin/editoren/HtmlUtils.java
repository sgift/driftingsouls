package net.driftingsouls.ds2.server.modules.admin.editoren;

import java.math.BigDecimal;

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
