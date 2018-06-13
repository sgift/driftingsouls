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
package net.driftingsouls.ds2.server.framework;

import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eine einfache Klasse, welche das <code>Context</code>-Interface implementiert. Die Klasse
 * verwendet ein <code>Request</code> und <code>Response</code>-Objekt fuer Ein- und Ausgabe.
 *
 * @author Christopher Jung
 *
 */
public class BasicContext implements Context
{
	private static final Log log = LogFactory.getLog(BasicContext.class);

	private Request request;
	private Response response;
	private BasicUser activeUser = null;
	private List<Error> errorList = new ArrayList<>();
	private Map<Class<?>, Object> contextSingletons = new HashMap<>();
	private final Map<Class<?>, Map<String, Object>> variables = new HashMap<>();
	private List<ContextListener> listener = new ArrayList<>();
	private PermissionResolver permissionResolver;
	private ApplicationContext applicationContext;

	/**
	 * Erstellt eine neue Instanz der Klasse unter Verwendung eines <code>Request</code> und einer
	 * <code>Response</code>-Objekts.
	 *
	 * @param request Die mit dem Kontext zu verbindende <code>Request</code>
	 * @param response Die mit dem Kontext zu verbindende <code>Response</code>
	 * @param presolver Der zu verwendende PermissionResolver
	 * @param applicationContext Der zur Aufloesung von Bean/Autowiring zu verwendende Spring ApplicationContext.
	 *                           Der ApplicationContext muss Autowiring unterstuetzen
	 */
	public BasicContext(Request request, Response response, PermissionResolver presolver, ApplicationContext applicationContext)
	{
		if( applicationContext == null || applicationContext.getAutowireCapableBeanFactory() == null )
		{
			throw new IllegalArgumentException("Es wurde kein oder keine gueltiger ApplicationContext uebergeben");
		}

		ContextMap.addContext(this);

		this.request = request;
		this.response = response;
		this.permissionResolver = presolver;
		this.applicationContext = applicationContext;
	}

	@Override
	public void autowireBean(Object bean)
	{
		this.applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
		if( bean instanceof ApplicationContextAware )
		{
			((ApplicationContextAware)bean).setApplicationContext(this.applicationContext);
		}
	}

	@Override
	public <T> T getBean(Class<T> cls, String name) throws IllegalArgumentException
	{
		try {
			if( name != null )
			{
				return this.applicationContext.getBean(name, cls);
			}
			return this.applicationContext.getBean(cls);
		}
		catch( BeansException e ) {
			throw new IllegalArgumentException("Die angegebene Bean konnte nicht gefunden werden", e);
		}
	}

	@Override
	public EntityManager getEM()
	{
		return HibernateUtil.getCurrentEntityManager();
	}

	@Override
	public org.hibernate.Session getDB()
	{
		return (org.hibernate.Session) HibernateUtil.getCurrentEntityManager().getDelegate();
	}

	@Override
	public BasicUser getActiveUser()
	{
		return activeUser;
	}

	@Override
	public void setActiveUser(BasicUser user)
	{
		activeUser = user;
	}

	@Override
	public void addError(String error)
	{
		errorList.add(new Error(error));
	}

	@Override
	public void addError(String error, String link)
	{
		errorList.add(new Error(error, link));
	}

	@Override
	public Error getLastError()
	{
		return errorList.get(errorList.size() - 1);
	}

	@Override
	public Error[] getErrorList()
	{
		return errorList.toArray(new Error[errorList.size()]);
	}

	@Override
	public Request getRequest()
	{
		return request;
	}

	@Override
	public Response getResponse()
	{
		return response;
	}

	@Override
	public void setResponse(Response response)
	{
		this.response = response;
	}

	/**
	 * Gibt alle allokierten Resourcen wieder frei. Dieser Schritt sollte immer dann getaetigt
	 * werden, wenn das Objekt nicht mehr benoetigt wird.
	 *
	 */
	public void free()
	{
		RuntimeException e = null;

		try
		{
			// Allen Listenern signalisieren, dass der Context geschlossen wird
			for (ContextListener aListener : this.listener)
			{
				try
				{
					aListener.onContextDestory();
				}
				catch (RuntimeException ex)
				{
					e = ex;
				}
			}
		}
		finally
		{
			// Context aus der Contextliste entfernen
			ContextMap.removeContext();
		}

		if( e != null )
		{
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> cls)
	{
		if( !cls.isAnnotationPresent(ContextInstance.class) )
		{
			log.fatal("ContextInstance Annotation not present: " + cls.getName());
			return null;
		}

		ContextInstance.Scope scope = cls.getAnnotation(ContextInstance.class).value();

		/* TODO: Exceptions? */
		if( scope == ContextInstance.Scope.REQUEST )
		{
			if( !contextSingletons.containsKey(cls) )
			{
				try
				{
					contextSingletons.put(cls, cls.getDeclaredConstructor().newInstance());
				}
				catch( Exception e )
				{
					log.error(String.format("Unable to add class to context singleton: %s", cls), e);
					return null;
				}
			}

			return (T)contextSingletons.get(cls);
		}

		return this.request.getFromSession(cls);
	}

	@Override
	public Object getVariable(Class<?> cls, String varname)
	{
		if( variables.containsKey(cls) )
		{
			Map<String, Object> map = variables.get(cls);
			if( map.containsKey(varname) )
			{
				return map.get(varname);
			}
		}
		return null;
	}

	@Override
	public void putVariable(Class<?> cls, String varname, Object value)
	{
		synchronized( variables )
		{
			if( !variables.containsKey(cls) )
			{
				variables.put(cls, new HashMap<>());
			}
		}
		Map<String, Object> map = variables.get(cls);
		synchronized( map )
		{
			map.put(varname, value);
		}
	}

	@Override
	public void registerListener(ContextListener listener)
	{
		this.listener.add(listener);
	}

	@Override
	public void remove(Class<?> cls)
	{
		if( !cls.isAnnotationPresent(ContextInstance.class) )
		{
			log.fatal("ContextInstance Annotation not present: " + cls.getName());
			return;
		}

		ContextInstance.Scope scope = cls.getAnnotation(ContextInstance.class).value();
		if( scope == ContextInstance.Scope.REQUEST )
		{
			this.contextSingletons.remove(cls);
		}
		else
		{
			this.request.removeFromSession(cls);
		}
	}

	@Override
	public boolean hasPermission(PermissionDescriptor permission)
	{
		return this.permissionResolver.hasPermission(permission);
	}

	@Override
	public void setPermissionResolver(PermissionResolver permissionResolver)
	{
		this.permissionResolver = permissionResolver;
	}
}
