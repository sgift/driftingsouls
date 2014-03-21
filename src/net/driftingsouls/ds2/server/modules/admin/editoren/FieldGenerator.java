package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Generator fuer ein normales Eigabefeld.
 * @param <V> Der Typ der Entity
 * @param <T> Der Datentyp des Eingabefelds
 */
public class FieldGenerator<V, T> implements CustomFieldGenerator<V>
{
	private final String label;
	private final String name;
	private final Class<?> viewType;
	private final Class<T> dataType;
	private final Function<V, T> getter;
	private final BiConsumer<V, T> setter;
	private final Map<Serializable, Object> selectionOptions = new LinkedHashMap<>();
	private boolean readOnly;

	public FieldGenerator(String label, String name, Class<?> viewType, Class<T> dataType, Function<V, T> getter, BiConsumer<V, T> setter)
	{
		this.label = label;
		this.name = name;
		this.viewType = viewType;
		this.getter = getter;
		this.setter = setter;
		this.dataType = dataType;

		if (this.viewType.isAnnotationPresent(Entity.class))
		{
			this.selectionOptions.putAll(generateSelectionOptions(this.viewType));
		}
	}

	public FieldGenerator<V, T> withOptions(Map<? extends Serializable, ?> options)
	{
		this.selectionOptions.clear();
		this.selectionOptions.putAll(options);
		return this;
	}

	public FieldGenerator<V, T> withNullOption(String label)
	{
		this.selectionOptions.put(null, label);
		return this;
	}

	public FieldGenerator<V, T> readOnly(boolean readOnly)
	{
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public void generate(Writer echo, V entity) throws IOException
	{
		T value = getter.apply(entity);

		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");
		if (Cargo.class.isAssignableFrom(viewType))
		{
			// TODO: Readonly-Support
			echo.append("<input type=\"hidden\" name=\"").append(name).append("\" id='").append(name).append("' value=\"").append(value != null ? value.toString() : new Cargo().toString()).append("\">");
			echo.append("<script type='text/javascript'>$(document).ready(function(){new CargoEditor('#").append(name).append("')});</script>");
		}
		else if (viewType.isAnnotationPresent(Entity.class) || !this.selectionOptions.isEmpty())
		{
			editEntityBySelection(echo, name, viewType, value);
		}
		else if (Boolean.class.isAssignableFrom(viewType))
		{
			boolean bool = false;
			if (value != null)
			{
				bool = (Boolean) value;
			}
			echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked='checked' " : "").append(readOnly ? "disabled='disabled' " : "").append(" \">");
		}
		else
		{
			echo.append("<input type=\"text\" ").append("id=\"").append(name).append("\" ").append(readOnly ? "disable='disabled' " : "").append("name=\"").append(name).append("\" value=\"").append(value != null ? value.toString() : "").append("\">");
			if (Number.class.isAssignableFrom(viewType))
			{
				writeAutoNumberJavaScript(echo);
			}
		}
		echo.append("</td></tr>\n");
	}

	private void writeAutoNumberJavaScript(Writer echo) throws IOException
	{
		int mDec = 0;
		Number minValue = -999999999.99;
		Number maxValue = 999999999.99;
		if (viewType == Double.class || viewType == Float.class || viewType == BigDecimal.class)
		{
			mDec = 8;
		}
		if (viewType == Integer.class)
		{
			minValue = Integer.MIN_VALUE;
			maxValue = Integer.MAX_VALUE;
		}
		else if (viewType == Long.class)
		{
			minValue = Long.MIN_VALUE;
			maxValue = Long.MAX_VALUE;
		}
		else if (viewType == Short.class)
		{
			minValue = Short.MIN_VALUE;
			maxValue = Short.MAX_VALUE;
		}
		else if (viewType == Byte.class)
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

	@SuppressWarnings("unchecked")
	@Override
	public void applyRequestValues(Request request, V entity)
	{
		if (this.readOnly)
		{
			return;
		}
		Class<?> type = this.dataType;
		// TODO: Datentyp aus Lambda bestimmen - leider nicht so einfach wegen type erasure :(
		String val = request.getParameter(this.name);
		if (val == null)
		{
			return;
		}
		if (Integer.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) Integer.valueOf(val));
		}
		else if (Double.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) Double.valueOf(val));
		}
		else if (String.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) val);
		}
		else if (Cargo.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) new Cargo(Cargo.Type.ITEMSTRING, val));
		}
		else if (Boolean.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) Boolean.valueOf(val));
		}
		else
		{
			throw new UnsupportedOperationException("Datentyp " + type.getName() + " nicht unterstuetzt");
		}
	}

	private Map<Serializable, Object> generateSelectionOptions(Class<?> entityClass)
	{
		Map<Serializable, Object> result = new LinkedHashMap<>();
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<?> editities = Common.cast(db.createCriteria(entityClass).list());
		for (Object entity : editities)
		{
			Serializable identifier = db.getIdentifier(entity);
			result.put(identifier, entity);
		}
		return result;
	}

	private void editEntityBySelection(Writer echo, String name, Class<?> type, Object value) throws IOException
	{
		echo.append("<select size=\"1\" ").append(readOnly ? "disabled='disabled' " : "").append("name=\"").append(name).append("\">");
		org.hibernate.Session db = ContextMap.getContext().getDB();

		Serializable selected = -1;
		if (type.isInstance(value) && type.isAnnotationPresent(Entity.class))
		{
			selected = db.getIdentifier(value);
		}
		else if (value instanceof Serializable)
		{
			selected = (Serializable) value;
		}

		boolean containsIdentifier = this.selectionOptions.containsKey(selected);

		for (Map.Entry<Serializable, Object> entry : this.selectionOptions.entrySet())
		{
			Serializable identifier = entry.getKey();
			echo.append("<option value=\"").append(identifier != null ? identifier.toString() : "").append("\" ");
			if ((identifier == null && !containsIdentifier) || (containsIdentifier && identifier != null && identifier.equals(selected)))
			{
				echo.append("selected=\"selected\"");
			}
			String label;
			if (entry.getValue() instanceof String)
			{
				label = (String) entry.getValue();
			}
			else
			{
				label = new ObjectLabelGenerator().generateFor(identifier, entry.getValue());
			}
			echo.append(">").append(label).append("</option>");
		}

		echo.append("</select>");
	}
}
