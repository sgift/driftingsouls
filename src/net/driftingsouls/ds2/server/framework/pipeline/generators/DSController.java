/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.pipeline.generators;

import net.driftingsouls.ds2.server.framework.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basisklasse fuer alle DS-spezifischen Generatoren.
 *
 * @author Christopher Jung
 */
public abstract class DSController extends Generator
{
	private static final Log log = LogFactory.getLog(DSController.class);
	private static final LocalVariableTableParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

	private ActionType actionType;
	private OutputHelper actionTypeHandler;

	private boolean disableDebugOutput;
	private long startTime;
	private Map<String, String> bodyParameters;
	private ParameterReader parameterReader;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private boolean disablePageMenu;

	/**
	 * Konstruktor.
	 *
	 * @param context Der Kontext
	 */
	public DSController(Context context)
	{
		super(context);

		this.parameterReader = new ParameterReader(getRequest(), this.getDB());

		this.parameterReader.parameterString("module");
		this.parameterReader.parameterString("action");

		this.startTime = System.currentTimeMillis();

		this.disableDebugOutput = false;

		this.bodyParameters = new HashMap<>();

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<>();
		this.disablePageMenu = false;

		setActionType(ActionType.DEFAULT);
	}

	protected String getModule()
	{
		return this.parameterReader.getString("module");
	}

	/**
	 * Gibt einen als Zahl registrierten Parameter in Form eines
	 * <code>int</code> zurueck.
	 *
	 * @param parameter Der Parametername
	 * @return Der Wert
	 * @deprecated Bitte nur noch Parameter der Actionmethoden verwenden
	 */
	@Deprecated
	public int getInteger(String parameter)
	{
		return this.parameterReader.getInteger(parameter);
	}

	/**
	 * Gibt einen als String registrierten parameter zurueck.
	 *
	 * @param parameter Der Name des Parameters
	 * @return Der Wert
	 * @deprecated Bitte nur noch Parameter der Actionmethoden verwenden
	 */
	@Deprecated
	public String getString(String parameter)
	{
		return this.parameterReader.getString(parameter);
	}

	/**
	 * Entfernt einen Parameter. Bei einer anschliessenden
	 * Registrierung des Parameters, ist der Wert leer.
	 *
	 * @param parameter Der Parametername
	 */
	public void unsetParameter(String parameter)
	{
		this.parameterReader.unsetParameter(parameter);
	}

	/**
	 * Ruft die angegebene Methode des angegebenen Objekts als verschachtelte Actionmethode (SubAction) auf.
	 * Im Gegensatz zu normalen Actions kann hier ein fester Satz zusaetzlicher Argumente uebergeben werden,
	 * der <b>genau</b> in dieser Reihenfolge auf die ersten Argumente der Actionmethode angewandt wird.
	 * Alle nachfolgenden Argumente werden ueber die URL-Parameter gefuellt.
	 *
	 * @param subparam Der Prefix fuer die URL-Parameter zwecks Schaffung eines eigenen Namensraums. Falls <code>null</code> oder Leerstring wird kein Prefix verwendet
	 * @param objekt Das Objekt dessen Methode aufgerufen werden soll
	 * @param methode Der Name der Actionmethode
	 * @param args Die ersten Argumente der Methode
	 * @return Das Ergebnis der Methode
	 * @throws ReflectiveOperationException Falls die Reflection-Operation schief laeuft
	 */
	protected Object rufeAlsSubActionAuf(String subparam, Object objekt, String methode, Object... args) throws ReflectiveOperationException
	{
		if (subparam != null)
		{
			this.parameterReader.parseSubParameter(subparam);
		}
		try
		{
			Method method = getMethodForAction(objekt, methode);
			method.setAccessible(true);
			Annotation[][] annotations = method.getParameterAnnotations();
			Type[] parameterTypes = method.getGenericParameterTypes();
			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

			Object[] params = new Object[annotations.length];
			for (int i = 0; i < params.length; i++)
			{
				if (i < args.length)
				{
					params[i] = args[i];
					continue;
				}

				UrlParam paramAnnotation = null;
				for (Annotation annotation : annotations[i])
				{
					if (annotation instanceof UrlParam)
					{
						paramAnnotation = (UrlParam) annotation;
						break;
					}
				}

				Type type = parameterTypes[i];
				params[i] = this.parameterReader.readParameterAsType(paramAnnotation == null ? parameterNames[i] : paramAnnotation.name(), type);
			}

			return method.invoke(objekt, params);
		}
		finally
		{
			this.parameterReader.parseSubParameter("");
		}
	}

	/**
	 * Registriert einen Parameter im System als Zahl. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 *
	 * @param parameter Der Name des Parameters
	 * @deprecated Bitte nur noch Parameter der Actionmethoden verwenden
	 */
	@Deprecated
	public void parameterNumber(String parameter)
	{
		this.parameterReader.parameterNumber(parameter);
	}

	/**
	 * Registriert einen Parameter im System als String. Der Parameter
	 * kann anschliessend ueber entsprechende Funktionen erfragt werden.
	 *
	 * @param parameter Der Name des Parameters
	 * @deprecated Bitte nur noch Parameter der Actionmethoden verwenden
	 */
	@Deprecated
	public void parameterString(String parameter)
	{
		this.parameterReader.parameterString(parameter);
	}

	/**
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen.
	 *
	 * @param action Der Name der Aktion
	 */
	protected void redirect(String action)
	{
		redirect(action, new HashMap<String,Object>());
	}

	/**
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen.
	 *
	 * @param action Der Name der Aktion
	 * @param arguments An diese Action zu uebergebende Parameter. Diese Parameter haben vorrang vor URL-Parametern.
	 */
	protected void redirect(String action, Map<String, Object> arguments)
	{
		try
		{
			Method method = getMethodForAction(this, action);

			final Action actionDescriptor = method.getAnnotation(Action.class);
			doActionOptimizations(actionDescriptor);

			Object result = invokeActionMethod(method, arguments);
			writeResultObject(result, actionDescriptor.value());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Ruft die Standardaktion auf.
	 */
	protected void redirect()
	{
		redirect("default", new HashMap<String,Object>());
	}

	private Method getMethodForAction(Object objekt, String action) throws NoSuchMethodException
	{
		Method[] methods = objekt.getClass().getMethods();
		for (Method method : methods)
		{
			Action actionAnnotation = method.getAnnotation(Action.class);
			if (actionAnnotation == null)
			{
				continue;
			}

			if (method.getName().equals(action + "Action"))
			{
				return method;
			}

			if (method.getName().equals(action + "AjaxAct"))
			{
				return method;
			}

			if (method.getName().equals(action))
			{
				return method;
			}
		}

		throw new NoSuchMethodException();
	}

	@Override
	public void handleAction(String action) throws IOException
	{
		if ((action == null) || action.isEmpty())
		{
			action = "default";
		}

		try
		{
			Method method = getMethodForAction(this, action);

			Action actionDescriptor = method.getAnnotation(Action.class);
			setActionType(actionDescriptor.value());

			try
			{
				if ((getErrorList().length != 0) || !validateAndPrepare())
				{
					printErrorListOnly(actionDescriptor.value());

					return;
				}

				if (actionDescriptor.value() == ActionType.DEFAULT)
				{
					printHeader();
				}

				doActionOptimizations(actionDescriptor);

				try
				{
					Object result = invokeActionMethod(method, new HashMap<String,Object>());
					writeResultObject(result, actionDescriptor.value());
				}
				catch (InvocationTargetException e)
				{
					Throwable ex = e;
					while (ex instanceof InvocationTargetException)
					{
						ex = ex.getCause();
					}
					throw ex;
				}
			}
			catch (ValidierungException e)
			{
				addError(e.getMessage(), e.getUrl());
				printErrorListOnly(actionDescriptor.value());
				return;
			}
		}
		catch (NoSuchMethodException e)
		{
			log.error("", e);
			addError("Die Aktion '" + action + "' existiert nicht!");
		}
		catch (RuntimeException | Error e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}

		this.parameterReader.parseSubParameter("");

		printErrorList(this.actionType);

		printFooter(action);
	}

	private Object invokeActionMethod(Method method, Map<String, Object> arguments) throws InvocationTargetException, IllegalAccessException
	{
		method.setAccessible(true);
		Annotation[][] annotations = method.getParameterAnnotations();
		Type[] parameterTypes = method.getGenericParameterTypes();
		String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

		Object[] params = new Object[annotations.length];
		for (int i = 0; i < params.length; i++)
		{
			UrlParam paramAnnotation = null;
			for (Annotation annotation : annotations[i])
			{
				if (annotation instanceof UrlParam)
				{
					paramAnnotation = (UrlParam) annotation;
					break;
				}
			}

			Type type = parameterTypes[i];
			String paramName = paramAnnotation == null ? parameterNames[i] : paramAnnotation.name();
			if (arguments.containsKey(paramName))
			{
				params[i] = arguments.get(paramName);
			}
			else
			{
				params[i] = this.parameterReader.readParameterAsType(paramName, type);
			}
		}

		return method.invoke(this, params);
	}

	protected void writeResultObject(Object result, ActionType value) throws IOException
	{
		if (result != null)
		{
			getResponse().getWriter().append(result.toString());
		}
	}

	private void doActionOptimizations(final Action actionDescriptor)
	{
		final Session db = this.getDB();
		if (actionDescriptor.readOnly())
		{
			// Nur lesender Zugriff -> flushes deaktivieren
			db.flush();
			db.setFlushMode(FlushMode.MANUAL);
		}
		else
		{
			db.setFlushMode(FlushMode.AUTO);
		}
	}

	protected void printErrorList(ActionType type) throws IOException
	{
		if (getErrorList().length > 0)
		{
			actionTypeHandler.printErrorList();
		}
	}

	protected void printErrorListOnly(ActionType type) throws IOException
	{
		actionTypeHandler.printHeader();

		printErrorList(type);

		actionTypeHandler.printFooter();
	}

	protected void printHeader() throws IOException
	{
		actionTypeHandler.setAttribute("module", this.parameterReader.getString("module"));
		actionTypeHandler.setAttribute("bodyParameters", this.getBodyParameters());
		actionTypeHandler.setAttribute("startTime", this.startTime);
		actionTypeHandler.printHeader();
	}

	protected void printFooter(String action) throws IOException
	{
		actionTypeHandler.setAttribute("enableDebugOutput", !this.disableDebugOutput ? true : null);
		if (!this.disablePageMenu)
		{
			actionTypeHandler.setAttribute("pagetitle", this.pageTitle);
			actionTypeHandler.setAttribute("pagemenu", this.pageMenuEntries.toArray(new PageMenuEntry[this.pageMenuEntries.size()]));
		}
		actionTypeHandler.printFooter();
	}

	/**
	 * (De)aktiviert die Debug-Ausgaben.
	 *
	 * @param value <code>true</code> zur Deaktivierung
	 */
	public void setDisableDebugOutput(boolean value)
	{
		disableDebugOutput = value;
	}

	/**
	 * Setzt die Bezeichnung der aktuellen Seite.
	 *
	 * @param title Die Bezeichnung
	 */
	public void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	/**
	 * Fuegt dem Seitenmenue einen Eintrag hinzu.
	 *
	 * @param title Die Titel des Eintrags
	 * @param url Die URL
	 */
	public void addPageMenuEntry(String title, String url)
	{
		this.pageMenuEntries.add(new PageMenuEntry(title, url));
	}

	/**
	 * Setzt, ob das Seitenmenue nicht verwendet werden soll.
	 *
	 * @param value <code>true</code>, falls es nicht verwendet werden soll
	 */
	public void setDisablePageMenu(boolean value)
	{
		this.disablePageMenu = value;
	}

	/**
	 * Gibt weitere HTML-Body-Tag-Attribute zurueck.
	 *
	 * @return Weitere HTML-Body-Tag-Attribute
	 */
	private String getBodyParameters()
	{
		StringBuilder text = new StringBuilder();

		if (bodyParameters.size() > 0)
		{
			for (String key : bodyParameters.keySet())
			{
				text.append(key).append("=\"").append(bodyParameters.get(key)).append("\" ");
			}
		}

		return text.toString();
	}

	/**
	 * Fuegt ein weiteres HTML-Body-Tag-Attribut hinzu.
	 * Sollte das Attribut bereits gesetzt seit, so wird es
	 * ueberschrieben.
	 *
	 * @param parameter Der Name des Attributs
	 * @param value Der Wert
	 */
	public void addBodyParameter(String parameter, String value)
	{
		bodyParameters.put(parameter, value);
	}

	protected void setActionType(ActionType type)
	{
		if (type == ActionType.DEFAULT)
		{
			actionTypeHandler = new HtmlOutputHelper();
		}
		else if (type == ActionType.AJAX)
		{
			actionTypeHandler = new AjaxOutputHelper();
		}
		else if (type == ActionType.BINARY)
		{
			actionTypeHandler = new BinaryOutputHelper();
		}

		getContext().autowireBean(actionTypeHandler);

		actionType = type;
	}

	/**
	 * Gibt den aktuellen Aktionstyp zurueck.
	 *
	 * @return Der Aktionstyp
	 */
	protected ActionType getActionType()
	{
		return actionType;
	}

	/**
	 * Gibt die Ausgabehilfe zurueck.
	 *
	 * @return Die Ausgabehilfe
	 */
	protected OutputHelper getOutputHelper()
	{
		return actionTypeHandler;
	}

	protected boolean validateAndPrepare()
	{
		return true;
	}

	/**
	 * Die Default-HTML-Aktion.
	 *
	 * @throws IOException
	 */
	public void defaultAction() throws IOException
	{
		getResponse().getWriter().append("DEFAULT");
	}
}
