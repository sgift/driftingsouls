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
import net.driftingsouls.ds2.server.framework.pipeline.ViewResult;
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

	private ParameterReader parameterReader;
	private String pageTitle;
	private List<PageMenuEntry> pageMenuEntries;
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

		this.pageTitle = null;
		this.pageMenuEntries = new ArrayList<>();
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
	 *
	 * @param subparam Der Prefix fuer die URL-Parameter zwecks Schaffung eines eigenen Namensraums. Falls <code>null</code> oder Leerstring wird kein Prefix verwendet
	 * @param objekt Das Objekt dessen Methode aufgerufen werden soll
	 * @param methode Der Name der Actionmethode
	 * @param args Die zusaetzlich zu uebergebenden Argumente (haben vorrang vor URL-Parametern)
	 * @return Das Ergebnis der Methode
	 * @throws ReflectiveOperationException Falls die Reflection-Operation schief laeuft
	 */
	protected final Object rufeAlsSubActionAuf(String subparam, Object objekt, String methode, Map<String,Object> args) throws ReflectiveOperationException
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
				UrlParam paramAnnotation = null;
				for (Annotation annotation : annotations[i])
				{
					if (annotation instanceof UrlParam)
					{
						paramAnnotation = (UrlParam) annotation;
						break;
					}
				}

				String paramName = paramAnnotation == null ? parameterNames[i].getName() : paramAnnotation.name();
				if( args.containsKey(paramName) )
				{
					params[i] = args.get(paramName);
				}
				else
				{
					Type type = parameterTypes[i];
					params[i] = this.parameterReader.readParameterAsType(paramName, type);
				}
			}

			return method.invoke(objekt, params);
		}
		finally
		{
			this.parameterReader.parseSubParameter("");
		}
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

		OutputHandler actionTypeHandler = "JSON".equals(parameterReader.getString("FORMAT")) ? new AjaxOutputHandler() : new HtmlOutputHandler();

		try
		{
			Method method = getMethodForAction(this, action);

			Action actionDescriptor = method.getAnnotation(Action.class);
			actionTypeHandler = determineOutputHandler(actionDescriptor);

			try
			{
				if ((getErrorList().length != 0) || !validateAndPrepare())
				{
					printErrorListOnly(actionTypeHandler);

					return;
				}

				if (actionDescriptor.value() == ActionType.DEFAULT)
				{
					printHeader(actionTypeHandler);
				}

				try
				{
					Object result = null;
					do
					{
						doActionOptimizations(actionDescriptor);

						result = invokeActionMethod(method, result != null ? (RedirectViewResult) result : null);
						if (result instanceof RedirectViewResult)
						{
							method = getMethodForAction(this, ((RedirectViewResult) result).getTargetAction());
							actionDescriptor = method.getAnnotation(Action.class);
						}
					} while( result instanceof RedirectViewResult);

					writeResultObject(result);
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
				printErrorListOnly(actionTypeHandler);
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

		printErrorList(actionTypeHandler);

		printFooter(actionTypeHandler);
	}

	private Object invokeActionMethod(Method method, RedirectViewResult viewResult) throws InvocationTargetException, IllegalAccessException
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
			if( type.equals(RedirectViewResult.class) )
			{
				params[i] = viewResult;
			}
			else if (viewResult != null && viewResult.getParameters().containsKey(paramName))
			{
				params[i] = viewResult.getParameters().get(paramName);
			}
			else
			{
				params[i] = this.parameterReader.readParameterAsType(paramName, type);
			}
		}

		return method.invoke(this, params);
	}

	private void writeResultObject(Object result) throws IOException
	{
		if (result != null)
		{
			if( result instanceof ViewResult )
			{
				((ViewResult) result).writeToResponse(getResponse());
				return;
			}

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

	private void printErrorList(OutputHandler handler) throws IOException
	{
		if (getErrorList().length > 0)
		{
			handler.printErrorList();
		}
	}

	private void printErrorListOnly(OutputHandler handler) throws IOException
	{
		handler.printHeader();

		printErrorList(handler);

		handler.printFooter();
	}

	private void printHeader(OutputHandler handler) throws IOException
	{
		handler.setAttribute("module", this.parameterReader.getString("module"));
		handler.printHeader();
	}

	private void printFooter(OutputHandler handler) throws IOException
	{
		handler.setAttribute("pagetitle", this.pageTitle);
		handler.setAttribute("pagemenu", this.pageMenuEntries.toArray(new PageMenuEntry[this.pageMenuEntries.size()]));
		handler.printFooter();
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

	private OutputHandler determineOutputHandler(Action type) throws IllegalAccessException, InstantiationException
	{
		OutputHandler actionTypeHandler = null;
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

		return actionTypeHandler;
	}

	protected boolean validateAndPrepare()
	{
		return true;
	}
}
