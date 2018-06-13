package net.driftingsouls.ds2.server.framework.pipeline.controllers;

import com.google.gson.Gson;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.ViewModel;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.ViewResult;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

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
 * Service zum Aufrufen von Actions von Controllern.
 */
@Service
public class ActionMethodInvoker
{
	private static final Log log = LogFactory.getLog(ActionMethodInvoker.class);
	private static List<Class<? extends ActionMethodInterceptor>> DEFAULT_INTERCEPTORS = new ArrayList<>();
	static {
		DEFAULT_INTERCEPTORS.add(AccountVacationMethodInterceptor.class);
		DEFAULT_INTERCEPTORS.add(UserAuthenticationMethodInterceptor.class);
		DEFAULT_INTERCEPTORS.add(TickMethodInterceptor.class);
	}

	private static final class RedirectInvocationException extends RuntimeException
	{
		public RedirectInvocationException(Exception cause)
		{
			super(cause);
		}
	}

	private Object invokeActionMethod(Object controller, ParameterReader parameterReader, Method method, RedirectViewResult viewResult) throws InvocationTargetException, IllegalAccessException
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
				params[i] = parameterReader.readParameterAsType(paramName, type);
			}
		}

		MethodInvocation methodInvocation = new ActionMethodInvocation(controller, method, params);
		for (Class<? extends ActionMethodInterceptor> interceptorCls : DEFAULT_INTERCEPTORS)
		{
			ActionMethodInterceptor interceptor = ContextMap.getContext().getBean(interceptorCls, null);
			methodInvocation = new InterceptingActionMethodInvocation(interceptor, methodInvocation);
		}

		try
		{
			return methodInvocation.proceed();
		}
		catch (InvocationTargetException | IllegalAccessException e)
		{
			throw e;
		}
		catch( Throwable t ) {
			throw new InvocationTargetException(t);
		}
	}

	private void writeResultObject(Object result) throws IOException
	{
		if (result != null)
		{
			Context context = ContextMap.getContext();
			if( result instanceof ViewResult)
			{
				((ViewResult) result).writeToResponse(context.getResponse());
				return;
			}

			if( result.getClass().isAnnotationPresent(ViewModel.class) ) {
				result = new Gson().toJson(result);
			}
			context.getResponse().getWriter().append(result.toString());
		}
	}

	private void doActionOptimizations(final Action actionDescriptor)
	{
		final Session db = ContextMap.getContext().getDB();
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

	private OutputHandler determineOutputHandler(Action type) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
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
			actionTypeHandler = type.outputHandler().getDeclaredConstructor().newInstance();
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
						actionTypeHandler = annotation.outputHandler().getDeclaredConstructor().newInstance();
						break;
					}
				}
			} while ((aClass = aClass.getSuperclass()) != null);
		}

		ContextMap.getContext().autowireBean(actionTypeHandler);

		return actionTypeHandler;
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

		throw new NoSuchMethodException("Keine Methode fuer Action '"+action+"' in Klasse '"+objekt.getClass().getName()+"' gefunden");
	}

	private void printHeader(ParameterReader parameterReader, OutputHandler handler) throws IOException
	{
		handler.setAttribute("module", parameterReader.getString("module"));
		handler.printHeader();
	}

	/**
	 * Ruft die genannte Action auf den entsprechenden Controller auf.
	 * @param controller Der Controller
	 * @param action Der Name der aufzurufenden Action
	 * @throws IOException
	 */
	public void rufeActionAuf(Controller controller, String action) throws IOException
	{
		Context context = ContextMap.getContext();
		ParameterReader parameterReader = new ParameterReader(context.getRequest(), context.getDB());
		parameterReader.parameterString("module");
		parameterReader.parameterString("action");

		if ((action == null) || action.isEmpty())
		{
			action = "default";
		}

		OutputHandler actionTypeHandler = "JSON".equals(parameterReader.getString("FORMAT")) ? new AjaxOutputHandler() : new HtmlOutputHandler();

		try
		{
			Method method = getMethodForAction(controller, action);

			Action actionDescriptor = method.getAnnotation(Action.class);
			actionTypeHandler = determineOutputHandler(actionDescriptor);

			try
			{
				if ((context.getErrorList().length != 0) || !controller.validateAndPrepare())
				{
					printErrorListOnly(actionTypeHandler);

					return;
				}

				if (actionDescriptor.value() == ActionType.DEFAULT)
				{
					printHeader(parameterReader, actionTypeHandler);
				}

				try
				{
					Object result = null;
					do
					{
						doActionOptimizations(actionDescriptor);

						result = invokeActionMethod(controller, parameterReader, method, result != null ? (RedirectViewResult) result : null);
						if (result instanceof RedirectViewResult)
						{
							method = getMethodForAction(controller, ((RedirectViewResult) result).getTargetAction());
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
				context.addError(e.getMessage(), e.getUrl());
				printErrorListOnly(actionTypeHandler);
				return;
			}
		}
		catch (NoSuchMethodException e)
		{
			log.error("", e);
			context.addError("Die Aktion '" + action + "' existiert nicht!");
		}
		catch (RuntimeException | java.lang.Error e)
		{
			throw e;
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}

		parameterReader.parseSubParameter("");

		printErrorList(actionTypeHandler);

		controller.printFooter(actionTypeHandler);
	}

	private void printErrorList(OutputHandler handler) throws IOException
	{
		if (ContextMap.getContext().getErrorList().length > 0)
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
		Context context = ContextMap.getContext();
		ParameterReader parameterReader = new ParameterReader(context.getRequest(), context.getDB());
		parameterReader.parameterString("module");
		parameterReader.parameterString("action");

		if (subparam != null)
		{
			parameterReader.parseSubParameter(subparam);
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
					params[i] = parameterReader.readParameterAsType(paramName, type);
				}
			}

			return method.invoke(objekt, params);
		}
		finally
		{
			parameterReader.parseSubParameter("");
		}
	}
}
