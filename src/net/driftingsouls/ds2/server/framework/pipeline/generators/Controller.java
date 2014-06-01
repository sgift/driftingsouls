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

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.PermissionDescriptor;
import net.driftingsouls.ds2.server.framework.PermissionResolver;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
public abstract class Controller implements PermissionResolver
{
	private static final Log log = LogFactory.getLog(Controller.class);

	private ActionType actionType;
	private OutputHandler actionTypeHandler;

	private boolean disableDebugOutput;
	private long startTime;
	private Map<String, String> bodyParameters;
	private ParameterReader parameterReader;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
	private boolean disablePageMenu;
	private Context context;

	/**
	 * Konstruktor.
	 *
	 */
	public Controller()
	{
		this.context = ContextMap.getContext();
		this.parameterReader = new ParameterReader(getRequest(), this.getDB());

		this.parameterReader.parameterString("module");
		this.parameterReader.parameterString("action");

		this.startTime = System.currentTimeMillis();

		this.disableDebugOutput = false;

		this.bodyParameters = new HashMap<>();

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<>();
		this.disablePageMenu = false;
	}

	protected final String getModule()
	{
		return this.parameterReader.getString("module");
	}

	/**
	 * Entfernt einen Parameter. Bei einer anschliessenden
	 * Registrierung des Parameters, ist der Wert leer.
	 *
	 * @param parameter Der Parametername
	 */
	public final void unsetParameter(String parameter)
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
	protected final Object rufeAlsSubActionAuf(String subparam, Object objekt, String methode, Object... args) throws ReflectiveOperationException
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
			Parameter[] parameterNames = method.getParameters();

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
				params[i] = this.parameterReader.readParameterAsType(paramAnnotation == null ? parameterNames[i].getName() : paramAnnotation.name(), type);
			}

			return method.invoke(objekt, params);
		}
		finally
		{
			this.parameterReader.parseSubParameter("");
		}
	}

	/**
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen.
	 *
	 * @param action Der Name der Aktion
	 */
	protected final void redirect(String action)
	{
		redirect(action, new HashMap<>());
	}

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu.
	 *
	 * @param error Die Beschreibung des Fehlers
	 */
	public final void addError( String error ) {
		context.addError(error);
	}

	/**
	 * Fuegt einen Fehler zur Fehlerliste hinzu und bietet zudem eine Ausweich-URL an.
	 *
	 * @param error Die Beschreibung des Fehlers
	 * @param link Die Ausweich-URL
	 */
	public final void addError( String error, String link ) {
		context.addError(error, link);
	}

	/**
	 * Liefert eine Liste aller Fehler zurueck.
	 *
	 * @return Eine Liste aller Fehlerbeschreibungen
	 */
	public final net.driftingsouls.ds2.server.framework.pipeline.Error[] getErrorList() {
		return context.getErrorList();
	}

	/**
	 * Liefert die Request fuer diesen Aufruf.
	 * @return Die Request des Aufrufs
	 */
	public final Response getResponse() {
		return context.getResponse();
	}

	/**
	 * Liefert die zum Aufruf gehoerende Response.
	 * @return Die Response des Aufrufs
	 */
	public final Request getRequest() {
		return context.getRequest();
	}

	/**
	 * Gibt den aktuellen Kontext zurueck.
	 * @return Der Kontext
	 */
	public final Context getContext() {
		return context;
	}

	/**
	 * Gibt die aktuelle Hibernate-Session zurueck.
	 * @return Die aktuelle Hibernate-Session
	 */
	public final Session getDB() {
		return context.getDB();
	}

	/**
	 * Gibt den aktuellen Hibernate-EntityManager zurueck.
	 * @return Der aktuelle Hibernate-EntityManager
	 */
	public final EntityManager getEM() {
		return context.getEM();
	}

	/**
	 * Gibt den aktiven User zurueck. Falls kein User eingeloggt ist
	 * wird <code>null</code> zurueckgegeben.
	 * @return Der User oder <code>null</code>
	 */
	public final BasicUser getUser() {
		return getContext().getActiveUser();
	}

	@Override
	public final boolean hasPermission(PermissionDescriptor permission)
	{
		return this.context.hasPermission(permission);
	}

	private static final class RedirectInvocationException extends RuntimeException
	{
		public RedirectInvocationException(Exception cause)
		{
			super(cause);
		}
	}

	/**
	 * Fuehrt eine Aktion aus. Die zur Aktion gehoerende Funktion wird aufgerufen.
	 *
	 * @param action Der Name der Aktion
	 * @param arguments An diese Action zu uebergebende Parameter. Diese Parameter haben vorrang vor URL-Parametern.
	 */
	protected final void redirect(String action, Map<String, Object> arguments)
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
			throw new RedirectInvocationException(e);
		}
	}

	/**
	 * Ruft die Standardaktion auf.
	 */
	protected final void redirect()
	{
		redirect("default", new HashMap<>());
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

	public final void handleAction(String action) throws IOException
	{
		if ((action == null) || action.isEmpty())
		{
			action = "default";
		}

		try
		{
			Method method = getMethodForAction(this, action);

			Action actionDescriptor = method.getAnnotation(Action.class);
			setActionType(actionDescriptor);

			try
			{
				if ((getErrorList().length != 0) || !validateAndPrepare())
				{
					printErrorListOnly();

					return;
				}

				if (actionDescriptor.value() == ActionType.DEFAULT)
				{
					printHeader();
				}

				doActionOptimizations(actionDescriptor);

				try
				{
					Object result = invokeActionMethod(method, new HashMap<>());
					writeResultObject(result, actionDescriptor.value());
				}
				catch (InvocationTargetException | RedirectInvocationException e)
				{
					Throwable ex = e;
					while (ex instanceof InvocationTargetException || ex instanceof RedirectInvocationException)
					{
						ex = ex.getCause();
					}
					throw ex;
				}
			}
			catch (ValidierungException e)
			{
				addError(e.getMessage(), e.getUrl());
				printErrorListOnly();
				return;
			}
		}
		catch (NoSuchMethodException e)
		{
			log.error("", e);
			addError("Die Aktion '" + action + "' existiert nicht!");
		}
		catch (RuntimeException | java.lang.Error e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}

		this.parameterReader.parseSubParameter("");

		printErrorList();

		printFooter(action);
	}

	private Object invokeActionMethod(Method method, Map<String, Object> arguments) throws InvocationTargetException, IllegalAccessException
	{
		method.setAccessible(true);
		Annotation[][] annotations = method.getParameterAnnotations();
		Type[] parameterTypes = method.getGenericParameterTypes();
		Parameter[] parameterNames = method.getParameters();

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
			String paramName = paramAnnotation == null ? parameterNames[i].getName() : paramAnnotation.name();
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

	protected final void writeResultObject(Object result, ActionType value) throws IOException
	{
		if (result != null)
		{
			if( result.getClass().isAnnotationPresent(ViewModel.class) ) {
				result = new Gson().toJson(result);
			}
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

	private void printErrorList() throws IOException
	{
		if (getErrorList().length > 0)
		{
			actionTypeHandler.printErrorList();
		}
	}

	private void printErrorListOnly() throws IOException
	{
		actionTypeHandler.printHeader();

		printErrorList();

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
	public final void setDisableDebugOutput(boolean value)
	{
		disableDebugOutput = value;
	}

	/**
	 * Setzt die Bezeichnung der aktuellen Seite.
	 *
	 * @param title Die Bezeichnung
	 */
	public final void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	/**
	 * Fuegt dem Seitenmenue einen Eintrag hinzu.
	 *
	 * @param title Die Titel des Eintrags
	 * @param url Die URL
	 */
	public final void addPageMenuEntry(String title, String url)
	{
		this.pageMenuEntries.add(new PageMenuEntry(title, url));
	}

	/**
	 * Setzt, ob das Seitenmenue nicht verwendet werden soll.
	 *
	 * @param value <code>true</code>, falls es nicht verwendet werden soll
	 */
	public final void setDisablePageMenu(boolean value)
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
	public final void addBodyParameter(String parameter, String value)
	{
		bodyParameters.put(parameter, value);
	}

	private void setActionType(Action type) throws IllegalAccessException, InstantiationException
	{
		if (type.value() == ActionType.DEFAULT)
		{
			actionTypeHandler = new HtmlOutputHandler();
		}
		else if (type.value() == ActionType.AJAX)
		{
			actionTypeHandler = new AjaxOutputHandler();
		}
		else if (type.value() == ActionType.BINARY)
		{
			actionTypeHandler = new BinaryOutputHandler();
		}

		if (type.outputHandler() != OutputHandler.class)
		{
			actionTypeHandler = type.outputHandler().newInstance();
		}
		else
		{
			Class<?> aClass = getClass();
			do
			{
				if (aClass.isAnnotationPresent(Module.class))
				{
					Module annotation = aClass.getAnnotation(Module.class);
					if (annotation.outputHandler() != OutputHandler.class)
					{
						actionTypeHandler = annotation.outputHandler().newInstance();
						break;
					}
				}
			} while ((aClass = aClass.getSuperclass()) != null);
		}

		getContext().autowireBean(actionTypeHandler);

		actionType = type.value();
	}

	/**
	 * Gibt den aktuellen Aktionstyp zurueck.
	 *
	 * @return Der Aktionstyp
	 */
	protected final ActionType getActionType()
	{
		return actionType;
	}

	/**
	 * Gibt die Ausgabehilfe zurueck.
	 *
	 * @return Die Ausgabehilfe
	 */
	protected final OutputHandler getOutputHelper()
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
