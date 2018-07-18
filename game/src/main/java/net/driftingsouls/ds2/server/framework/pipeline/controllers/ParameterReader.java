package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import javax.persistence.Entity;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * Klasse zum Parsen und Konvertieren von Requestparametern.
 */
public class ParameterReader
{
	private static final Logger LOG = LogManager.getLogger(ParameterReader.class);

	private static final Map<Class<?>, UrlParamKonverter> konverter = new HashMap<>();

	static
	{
		SortedSet<Class<?>> klassenNamen = AnnotationUtils.INSTANCE.findeKlassenMitAnnotation(UrlParamKonverterFuer.class);
		for (Class<?> cls : klassenNamen)
		{
			try
			{
				if (!UrlParamKonverter.class.isAssignableFrom(cls))
				{
					LOG.warn("Konverterklasse " + cls.getName() + " implementiert nicht das korrekte Interface");
					continue;
				}
				UrlParamKonverterFuer annotation = cls.getAnnotation(UrlParamKonverterFuer.class);
				konverter.put(annotation.value(), cls.asSubclass(UrlParamKonverter.class).getDeclaredConstructor().newInstance());
			}
			catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
			{
				LOG.warn("Konnte Konverterklasse " + cls.getName() + " nicht instantiieren", e);
			}
		}

	}

	private Request request;
	private String subParameter;
	private Map<String, Object> parameter = new HashMap<>();
	private org.hibernate.Session session;

	public ParameterReader(Request request, Session session)
	{
		this.request = request;
		this.session = session;
		this.subParameter = "";

	}

	protected void parseSubParameter(String subparam)
	{
		subParameter = subparam;
	}

	private String buildParameterName(String parameter)
	{
		if (!subParameter.equals(""))
		{
			parameter = subParameter + "[" + parameter + "]";
		}
		return parameter;
	}

	private boolean parameterExists(String parameter)
	{
		parameter = buildParameterName(parameter);
		return (this.request.getParameter(parameter) != null) && !this.request.getParameter(parameter).isEmpty();
	}

	private String string(String parameter)
	{
		parameterString(parameter);
		return (String) getParameter(parameter);
	}

	private Number number(String parameter)
	{
		String value = string(parameter);
		try
		{
			return Common.getNumberFormat().parse(value.trim());
		}
		catch (NumberFormatException | ParseException e)
		{
			return 0d;
		}
	}

	/**
	 * Registriert einen Parameter im System als String. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 *
	 * @param parameter Der Name des Parameters
	 */
	public void parameterString(String parameter)
	{
		parameter = buildParameterName(parameter);
		if (this.request.getParameter(parameter) != null)
		{
			this.parameter.put(parameter, this.request.getParameter(parameter));
		}
		else
		{
			this.parameter.put(parameter, "");
		}
	}

	private Object getParameter(String parameter)
	{
		return this.parameter.get(buildParameterName(parameter));
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>int</code> zurueck.
	 *
	 * @param parameter Der Parametername
	 * @return Der Wert
	 */
	public int getInteger(String parameter)
	{
		return ((Number) getParameter(parameter)).intValue();
	}

	/**
	 * Gibt einen als String registrierten parameter zurueck.
	 *
	 * @param parameter Der Name des Parameters
	 * @return Der Wert
	 */
	public String getString(String parameter)
	{
		return (String) getParameter(parameter);
	}

	/**
	 * Registriert den Parameter im System unter dem angegebenen (oder einem kompatiblen)
	 * Typ und gibt den Parameter als ein in eine Instanz dieses Typs konvertiertes Objekt zurueck.
	 * Diese Methode liefert immer einen Wert zurueck auch wenn sich der Parameter nicht konvertieren
	 * laesst. In einen solchen Fall wird dann der Defaultwert (z.B. <code>0</code>, <code>null</code>)
	 * zurueckgegeben.
	 *
	 * @param paramName Der Name des Parameters
	 * @param typeDescription Der Typ des Parameters
	 * @return Ein Objekt vom angegebenen Typ das den Wert des Parameters enthaelt
	 * @throws IllegalArgumentException Falls der angegebene Typ nicht unterstuetzt wird
	 */
	public Object readParameterAsType(String paramName, Type typeDescription) throws IllegalArgumentException
	{
		Class<?> type = extractClass(typeDescription);

		if (konverter.containsKey(type))
		{
			return konverter.get(type).konvertiere(this, paramName);
		}
		else if (type == Boolean.TYPE)
		{
			return number(paramName).intValue() == 1;
		}
		else if (type == Boolean.class)
		{
			return parameterExists(paramName) ? number(paramName).intValue() == 1 : null;
		}
		else if (type == Integer.class)
		{
			return parameterExists(paramName) ? number(paramName).intValue() : null;
		}
		else if (type == Integer.TYPE)
		{
			return parameterExists(paramName) ? number(paramName).intValue() : 0;
		}
		else if (type == Long.class)
		{
			return parameterExists(paramName) ? number(paramName).longValue() : null;
		}
		else if (type == Long.TYPE)
		{
			return parameterExists(paramName) ? number(paramName).longValue() : 0L;
		}
		else if (type == Double.class)
		{
			return parameterExists(paramName) ? number(paramName).doubleValue() : null;
		}
		else if (type == Double.TYPE)
		{
			return parameterExists(paramName) ? number(paramName).doubleValue() : 0d;
		}
		else if (Enum.class.isAssignableFrom(type))
		{
			String strValue = string(paramName);
			try
			{
				if (NumberUtils.isDigits(strValue))
				{
					return type.getEnumConstants()[Integer.parseInt(strValue)];
				}
				return Enum.valueOf(type.asSubclass(Enum.class), strValue);
			}
			catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e)
			{
				return null;
			}
		}
		else if (type == String.class)
		{
			return string(paramName);
		}
		else if (type.isAnnotationPresent(Entity.class))
		{
			return readEntity(paramName, type);
		}
		else if (Map.class.isAssignableFrom(type))
		{
			return readMap(paramName, (ParameterizedType) typeDescription);
		}
		throw new IllegalArgumentException(type.getName() + " ist kein gueltiger Parametertyp fuer eine Action-Methode");
	}

	private Object readEntity(String paramName, Class<?> type)
	{
		Class<?> idClass = this.session.getSessionFactory().getClassMetadata(type).getIdentifierType().getReturnedClass();
		Serializable id = (Serializable) readParameterAsType(paramName, idClass);
		if (id == null)
		{
			return null;
		}
		return this.session.get(type, id);
	}

	private Object readMap(String paramName, ParameterizedType typeDescription)
	{
		Type[] pTypes = typeDescription.getActualTypeArguments();
		Class<?> firstParamType = extractClass(pTypes[0]);
		Class<?> secondParamType = extractClass(pTypes[1]);

		Map<Object, Object> resultMap = new HashMap<>();

		String prefix = paramName;
		String suffix = "";
		int idx = prefix.indexOf('#');
		if (idx != -1)
		{
			suffix = idx < prefix.length() - 1 ? prefix.substring(idx + 1) : "";
			prefix = prefix.substring(0, idx);
		}

		for (String key : this.request.getParameterMap().keySet())
		{
			if (!key.startsWith(prefix) || !key.endsWith(suffix) || (prefix + suffix).equals(key))
			{
				continue;
			}
			String mapKey = key.substring(0, key.length() - suffix.length());
			mapKey = mapKey.substring(prefix.length());

			resultMap.put(mapKeyToType(firstParamType, mapKey), readParameterAsType(key, secondParamType));
		}
		return resultMap;
	}

	private Object mapKeyToType(Class<?> firstParamType, String mapKey)
	{
		if(firstParamType.isAnnotationPresent(Entity.class))
		{
			Session db = ContextMap.getContext().getDB();
			Class<?> idClass = db.getSessionFactory().getClassMetadata(firstParamType).getIdentifierType().getReturnedClass();
			return db.get(firstParamType, (Serializable)StringToTypeConverter.convert(idClass, mapKey));
		}

		return StringToTypeConverter.convert(firstParamType, mapKey);
	}

	private Class<?> extractClass(Type typeDescription)
	{
		if (typeDescription instanceof Class)
		{
			return (Class) typeDescription;
		}
		if (typeDescription instanceof ParameterizedType)
		{
			return (Class) ((ParameterizedType) typeDescription).getRawType();
		}
		throw new IllegalArgumentException("Kann Klasse fuer Typ " + typeDescription + " nicht ermitteln");
	}
}
