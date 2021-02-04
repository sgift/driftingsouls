package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import net.driftingsouls.ds2.server.framework.AnnotationUtils;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.utils.StringToTypeConverter;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasse zum Parsen und Konvertieren von Requestparametern.
 */
@Service
@RequestScope
public class ParameterReader implements ApplicationContextAware
{
	private static final Map<Class<?>, UrlParamKonverter<?>> konverter = new HashMap<>();

	private final Map<String, Object> parameter = new HashMap<>();

	@PersistenceContext
	private EntityManager em;

	private String subParameter = "";
	private Request request;

	public void setRequest(Request request) {
		this.request = request;
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
	public Object readParameterAsType(String paramName, Type typeDescription)
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
		var metaModel = em.getEntityManagerFactory().getMetamodel();
		var managedType = metaModel.managedType(type);

		Serializable id = (Serializable) readParameterAsType(paramName, managedType.getAttribute("id").getJavaType());
		if (id == null)
		{
			return null;
		}
		return em.find(type, id);
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
			var metaModel = em.getEntityManagerFactory().getMetamodel();
			var managedType = metaModel.managedType(firstParamType).getAttribute("id");
			Class<?> idClass = managedType.getJavaType();
			return em.find(firstParamType, StringToTypeConverter.convert(idClass, mapKey));
		}

		return StringToTypeConverter.convert(firstParamType, mapKey);
	}

	private Class<?> extractClass(Type typeDescription)
	{
		if (typeDescription instanceof Class)
		{
			return (Class<?>) typeDescription;
		}
		if (typeDescription instanceof ParameterizedType)
		{
			return (Class<?>) ((ParameterizedType) typeDescription).getRawType();
		}
		throw new IllegalArgumentException("Kann Klasse fuer Typ " + typeDescription + " nicht ermitteln");
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		if (!konverter.isEmpty()) {
			return;
		}

		try (var scanResult = AnnotationUtils.INSTANCE.scanDsClasses()) {
			//Classes need the UrlParamKonverterFuer annotation and need to implement UrlParamKonverter
			var urlParamKonverterInfo = scanResult.getClassInfo(UrlParamKonverter.class.getName());
			var classInfo = scanResult.getClassesWithAnnotation(UrlParamKonverterFuer.class.getName())
				.getAssignableTo(urlParamKonverterInfo);
			for (ClassInfo cls : classInfo) {
				AnnotationInfo annotationInfo = cls.getAnnotationInfo(UrlParamKonverterFuer.class.getName());
				@SuppressWarnings("unchecked")
				var clazz = (Class<?>) ((AnnotationClassRef) annotationInfo.getParameterValues().getValue("value")).loadClass();
				@SuppressWarnings("unchecked")
				var converter = (Class<UrlParamKonverter<?>>)cls.loadClass();
				konverter.put(clazz, applicationContext.getBean(converter));
			}
		}
	}
}
