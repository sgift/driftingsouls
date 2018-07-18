package net.driftingsouls.ds2.server.modules.admin.editoren;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generator fuer ein Auswahlfeld mit mehrfacher Auswahl.
 * @param <E> Der Typ der Entity
 * @param <T> Der Datentyp des Eingabefelds
 */
public class MultiSelectionGenerator<E, T> implements CustomFieldGenerator<E>
{
	private final String label;
	private final String name;
	private final Class<?> viewType;
	private final Class<T> dataType;
	private final Function<E, Set<T>> getter;
	private final BiConsumer<E, Set<T>> setter;
	private final Map<Serializable, Object> selectionOptions = new LinkedHashMap<>();
	private Function<E,Boolean> readOnly;

	public MultiSelectionGenerator(String label, String name, Class<?> viewType, Class<T> dataType, Function<E, Set<T>> getter, BiConsumer<E, Set<T>> setter)
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

	public MultiSelectionGenerator<E, T> withOptions(Map<? extends Serializable, ?> options)
	{
		this.selectionOptions.clear();
		this.selectionOptions.putAll(new TreeMap<>(options));
		return this;
	}

	public MultiSelectionGenerator<E, T> readOnly(Function<E,Boolean> readOnly)
	{
		this.readOnly = readOnly;
		return this;
	}

	public MultiSelectionGenerator<E, T> readOnly(boolean readOnly)
	{
		this.readOnly = (e) -> readOnly;
		return this;
	}

	@Override
	public void generate(StringBuilder echo, E entity) throws IOException
	{
		echo.append("<tr>");
		echo.append("<td colspan='2'>").append(label.trim().isEmpty() ? "" : label + ":").append("</td>");
		echo.append("<td>");

		editEntityBySelection(echo, name, viewType, entity);

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
		String[] vals = request.getParameterValues(this.name);

		if (this.dataType.isAnnotationPresent(Entity.class) )
		{
			setter.accept(entity, Arrays.stream(vals).map(this::convertToEntity).collect(Collectors.toSet()));
		}
		else
		{
			setter.accept(entity, Arrays.stream(vals).map((s) -> StringToTypeConverter.convert(this.dataType, s)).collect(Collectors.toSet()));
		}
	}

	@Override
	public ColumnDefinition<E> getColumnDefinition(boolean forEditing)
	{
		return new ColumnDefinition<>(name, label, viewType);
	}

	@Override
	public String serializedValueOf(E entity)
	{
		Set<T> selectedValues = getter.apply(entity);
		Set<Serializable> selectedIdentifiers = selectedValues.stream().map((v) -> getIdentifierFor(viewType, v)).collect(Collectors.toSet());

		return this.selectionOptions.entrySet().stream().filter((e) -> selectedIdentifiers.contains(e.getKey())).map((e) -> generateLabel(e.getKey(), e.getValue())).collect(Collectors.joining(","));
	}

	@SuppressWarnings("unchecked")
	private T convertToEntity(@Nonnull String val)
	{
		if( val.isEmpty() )
		{
			return null;
		}
		Session db = ContextMap.getContext().getDB();
		Class<?> identifierCls = db.getSessionFactory().getClassMetadata(this.dataType).getIdentifierType().getReturnedClass();
		try
		{
			Method valueOf = identifierCls.getMethod("valueOf", String.class);
			return (T) db.get(this.dataType, (Serializable)valueOf.invoke(null, val));
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
		{
			throw new UnsupportedOperationException("Kann Identifier fuer Entity nicht konvertieren. Datentyp "+identifierCls+" nicht unterstuetzt.");
		}
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
			Session db = ContextMap.getContext().getDB();

			List<?> editities = Common.cast(db.createCriteria(entityClass).list());
			for (Object entity : editities)
			{
				Serializable identifier = db.getIdentifier(entity);
				result.put(identifier, entity);
			}
		}
		return result;
	}

	private void editEntityBySelection(StringBuilder echo, String name, Class<?> type, E entity) throws IOException
	{
		echo.append("<select multiple=\"multiple\" size=\"10\" ").append(readOnly.apply(entity) ? "disabled='disabled' " : "").append("name=\"").append(name).append("\">");

		Set<T> selectedValues = getter.apply(entity);
		Set<Serializable> selectedIdentifiers = selectedValues.stream().map((v) -> getIdentifierFor(type, v)).collect(Collectors.toSet());

		for (Map.Entry<Serializable, Object> entry : this.selectionOptions.entrySet())
		{
			Serializable identifier = entry.getKey();
			echo.append("<option ");
			echo.append(" value=\"").append(identifier != null ? identifier.toString() : "").append("\"");
			if (selectedIdentifiers.contains(identifier))
			{
				echo.append(" selected=\"selected\"");
			}
			Object value = entry.getValue();
			echo.append(">").append(generateLabel(identifier, value)).append("</option>");
		}

		echo.append("</select>");
	}

	private String generateLabel(Serializable identifier, Object value)
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

	private Serializable getIdentifierFor(Class<?> type, T value)
	{
		Session db = ContextMap.getContext().getDB();

		Serializable selected = -1;
		if (type.isInstance(value) && type.isAnnotationPresent(Entity.class))
		{
			selected = db.getIdentifier(value);
		}
		else if (value instanceof Serializable)
		{
			selected = (Serializable)value;
		}
		else if( value != null ) {
			// Workaround fuer per XML konfigurierte Objekte
			try
			{
				return (Serializable)value.getClass().getMethod("getId").invoke(value);
			}
			catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
			{
				// Ignore
			}
		}
		return selected;
	}
}
