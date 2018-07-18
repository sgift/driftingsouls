package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.metamodel.SingularAttribute;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generator fuer ein normales Eigabefeld.
 * @param <E> Der Typ der Entity
 * @param <T> Der Datentyp des Eingabefelds
 */
public class FieldGenerator<E, T> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private final Class<?> viewType;
	private final Class<T> dataType;
	private final Function<E, T> getter;
	private final BiConsumer<E, T> setter;
	private final Map<Serializable, Object> selectionOptions = new LinkedHashMap<>();
	private Function<E,Boolean> readOnly;
	private SingularAttribute<E, T> dbColumn;

	public FieldGenerator(String label, String name, Class<?> viewType, Class<T> dataType, Function<E, T> getter, BiConsumer<E, T> setter)
	{
		this.label = label;
		this.name = name;
		this.viewType = viewType;
		this.getter = getter;
		this.setter = setter;
		this.dataType = dataType;
		this.readOnly = (e) -> false;

		if (this.viewType.isAnnotationPresent(Entity.class) || this.viewType.isEnum())
		{
			this.selectionOptions.putAll(generateSelectionOptions(this.viewType));
		}
	}

	public FieldGenerator<E, T> withOptions(Map<? extends Serializable, ?> options)
	{
		this.selectionOptions.clear();
		this.selectionOptions.putAll(options);
		return this;
	}

	public FieldGenerator<E, T> withNullOption(String label)
	{
		this.selectionOptions.put(null, label);
		return this;
	}

	public FieldGenerator<E, T> readOnly(Function<E,Boolean> readOnly)
	{
		this.readOnly = readOnly;
		return this;
	}

	public FieldGenerator<E, T> readOnly(boolean readOnly)
	{
		this.readOnly = (e) -> readOnly;
		return this;
	}

	public FieldGenerator<E,T> dbColumn(SingularAttribute<E,T> dbColumn)
	{
		this.dbColumn = dbColumn;
		return this;
	}

	@Override
	public void generate(StringBuilder echo, E entity) throws IOException
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
			editEntityBySelection(echo, name, viewType, value, entity);
		}
		else if (Boolean.class.isAssignableFrom(viewType))
		{
			boolean bool = false;
			if (value != null)
			{
				bool = (Boolean) value;
			}
			echo.append("<input type=\"checkbox\" name=\"").append(name).append("\" value=\"true\" ").append(bool ? "checked=\"checked\" " : "").append(readOnly.apply(entity) ? "disabled=\"disabled\" " : "").append("/>");
		}
		else
		{
			HtmlUtils.textInput(echo, name, readOnly.apply(entity), viewType, value);
		}
		echo.append("</td></tr>\n");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void applyRequestValues(Request request, E entity)
	{
		if (this.readOnly.apply(entity))
		{
			return;
		}
		Class<T> type = this.dataType;

		String val = request.getParameter(this.name);
		if (!this.dataType.isAssignableFrom(Boolean.class) && val == null)
		{
			return;
		}

		if (this.dataType.isAnnotationPresent(Entity.class) )
		{
			applyEntityAsRequestValue(entity, val);
		}
		else if (Cargo.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T) new Cargo(Cargo.Type.ITEMSTRING, val));
		}
		else if (Boolean.class.isAssignableFrom(type))
		{
			setter.accept(entity, (T)Boolean.valueOf(val != null && !val.isEmpty()));
		}
		else
		{
			setter.accept(entity, StringToTypeConverter.convert(type, val));
		}
	}

	@Override
	public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
	{
		ColumnDefinition<E> def = new ColumnDefinition<>(name, label, viewType, Cargo.class.isAssignableFrom(viewType) ? "cargo" : null, dbColumn);
		if( forEditing )
		{
			def.setEditable(!Cargo.class.isAssignableFrom(viewType));
			if( !this.selectionOptions.isEmpty() )
			{
				def.setEditType("select");
				for (Map.Entry<Serializable, Object> entry : this.selectionOptions.entrySet())
				{
					def.addEditOption(HtmlUtils.identifierToString(entry.getKey()), HtmlUtils.objectLabelToString(entry.getKey(), entry.getValue()));
				}
			}
		}
		return def;
	}

	@Override
	public String serializedValueOf(E entity)
	{
		T value = getter.apply(entity);

		if (Cargo.class.isAssignableFrom(viewType))
		{
			return value != null ? value.toString() : new Cargo().toString();
		}
		else if (viewType.isAnnotationPresent(Entity.class) || !this.selectionOptions.isEmpty())
		{
			if( value == null ) {
				Object val = this.selectionOptions.get(null);
				if( val == null )
				{
					val = "";
				}
				return val.toString();
			}
			return new ObjectLabelGenerator().generateFor(selectedValueIdentifier(viewType, entity), value);
		}
		else if (Boolean.class.isAssignableFrom(viewType))
		{
			boolean bool = false;
			if (value != null)
			{
				bool = (Boolean) value;
			}
			return Boolean.toString(bool);
		}
		else
		{
			return value != null ? value.toString() : "";
		}
	}

	@SuppressWarnings("unchecked")
	private void applyEntityAsRequestValue(@Nonnull E entity, @Nonnull String val)
	{
		if( val.isEmpty() )
		{
			setter.accept(entity, null);
			return;
		}
		Session db = ContextMap.getContext().getDB();
		Class<?> identifierCls = db.getSessionFactory().getClassMetadata(this.dataType).getIdentifierType().getReturnedClass();
		setter.accept(entity, (T) db.get(this.dataType, (Serializable)StringToTypeConverter.convert(identifierCls, val)));
	}

	private Map<Serializable, Object> generateSelectionOptions(Class<?> entityClass)
	{
		Map<Serializable, Object> result = new LinkedHashMap<>();
		if( entityClass.isEnum() )
		{
			Object[] enumConstants = entityClass.getEnumConstants();
			result.putAll(Arrays.stream(enumConstants).collect(Collectors.toMap((o) -> (Serializable)o, (o) -> o)));
		}
		else
		{
			org.hibernate.Session db = ContextMap.getContext().getDB();

			List<?> editities = Common.cast(db.createCriteria(entityClass).list());
			for (Object entity : editities)
			{
				Serializable identifier = db.getIdentifier(entity);
				result.put(identifier, entity);
			}
		}
		return result;
	}

	private void editEntityBySelection(StringBuilder echo, String name, Class<?> type, Object value, E entity) throws IOException
	{
		Serializable selected = selectedValueIdentifier(type, value);

		HtmlUtils.select(echo, name, readOnly.apply(entity), this.selectionOptions, selected);
	}

	private Serializable selectedValueIdentifier(Class<?> type, Object value)
	{
		Serializable selected = null;
		if (type.isInstance(value) && type.isAnnotationPresent(Entity.class))
		{
			org.hibernate.Session db = ContextMap.getContext().getDB();

			selected = db.getIdentifier(value);
		}
		else if (value instanceof Serializable)
		{
			selected = (Serializable) value;
		}
		return selected;
	}
}
