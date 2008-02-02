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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.Transaction;

/**
 * Eine einfache Klasse, welche das <code>Context</code>-Interface implementiert.
 * Die Klasse verwendet ein <code>Request</code> und <code>Response</code>-Objekt
 * fuer Ein- und Ausgabe.
 * 
 * @author Christopher Jung
 *
 */
public class BasicContext implements Context,Loggable {
	private Database database;
	private Request request;
	private Response response;
	private BasicUser activeUser = null;
	private List<Error> errorList = new ArrayList<Error>();
	private Map<Class<?>, Object> contextSingletons = new HashMap<Class<?>,Object>();
	private Map<Class<?>, Map<String,Object>> variables = new HashMap<Class<?>, Map<String, Object>>();
	private String currentSession = "";
	private List<ContextListener> listener = new ArrayList<ContextListener>();
	private org.hibernate.Session session;
	private Transaction transaction;
	
	private static String[] actionBlockingPhrases = {
		"Immer mit der Ruhe!\nDer arme Server ist schon total erschl&ouml;pft.\nG&ouml;nn ihm mal ein oder zwei Minuten Pause :)",
		"Laaaaangsaaaaam!\nDeine Maus hat ja schon nen Krampf von dem vielen geklicke bekommen!\nMach mal eine kleine Pause und g&ouml;nn ihr die Entspannung...",
		"In der Ruhe liegt die Kraft!\nNur der arme Server hat wegen deiner Klickerei keine Ruhe und somit auch keine Kraft mehr. Lass ihm eine kleine Verschnaufpause und mach dann weiter.",
		"Vorsicht mein junger Padawan!\nBei diesem Klicktempo k&ouml;nntest du aus versehen auf den Selbstzerst&ouml;rungsknopf dr&uuml;cken! Mach eine kurze Pause und klicke dann langsam und gewissenhaft weiter..."
	};
	
	/**
	 * Erstellt eine neue Instanz der Klasse unter Verwendung eines
	 * <code>Request</code> und einer <code>Response</code>-Objekts
	 * @param request Die mit dem Kontext zu verbindende <code>Request</code>
	 * @param response Die mit dem Kontext zu verbindende <code>Response</code>
	 */
	public BasicContext(Request request, Response response) {
		ContextMap.addContext(this);
		
		session = HibernateFacade.openSession();
		transaction = session.beginTransaction();
		
		database = new Database(session.connection());
		
		if( Configuration.getIntSetting("LOG_QUERIES") != 0 ) {
			database.setQueryLogStatus(true);
		}
		this.request = request;
		this.response = response;
		
		revalidate();
	}
	
	private void authenticateUser() {
		String sess = request.getParameter("sess");
		currentSession = sess;
		
		String errorurl = Configuration.getSetting("URL")+"ds?module=portal&action=login";
	
		if( (sess == null) || sess.equals("") ) {
			return;
		}
		
		long time = Common.time();
		Session sessdata = (Session)getDB().get(Session.class, sess);
		
		if( sessdata == null ) {
			addError( "Sie sind offenbar nicht eingeloggt", errorurl );

			return;
		}

		if( sessdata.getTick() ) {
			addError( "Im Moment werden einige Tick-Berechnungen f&uuml;r sie durchgef&uuml;hrt. Bitte haben sie daher ein wenig Geduld", 
					getRequest().getRequestURL() + 
						(getRequest().getQueryString() != null ? "?" + getRequest().getQueryString() : "") 
			);

			return;
		}
	
		BasicUser user = sessdata.getUser();
		user.setSessionData(sessdata);
		if( !user.hasFlag(BasicUser.FLAG_DISABLE_IP_SESSIONS) && !sessdata.isValidIP(getRequest().getRemoteAddress()) ) {
			addError( "Diese Session ist einer anderen IP zugeordnet", errorurl );

			return;
		}
	
		if( !user.hasFlag(BasicUser.FLAG_DISABLE_AUTO_LOGOUT) && (Common.time() - sessdata.getLastAction() > Configuration.getIntSetting("AUTOLOGOUT_TIME")) ) {
			getDB().delete(sessdata);
			addError( "Diese Session ist bereits abgelaufen", errorurl );

			return;
		}
		
		final long oldtime = sessdata.getLastAction();
		
		sessdata.setLastAction(time);
		commit();
		
		if( !user.hasFlag(BasicUser.FLAG_NO_ACTION_BLOCKING) ) {
			// Alle 1.5 Sekunden Counter um 1 reduzieren, sofern mindestens 5 Sekunden Pause vorhanden waren
			int reduce = (int)((time - oldtime)/1.5);
			if( time < oldtime + 5 ) {
				reduce = -1;
			}
			int actioncounter = sessdata.getActionCounter()-reduce;
			if( actioncounter < 0 ) {
				actioncounter = 0;
			}

			if( reduce > 0 ) {
				final int value = sessdata.getActionCounter() - reduce;
				
				sessdata.setActionCounter(value > 0 ? value : 0);
			}
			else if( reduce < 0 ) {
				sessdata.setActionCounter(sessdata.getActionCounter()+1);
			}
			
			int sleep = -1;
			
			// Bei viel zu hoher Aktivitaet einfach die Ausfuehrung mit einem Fehler beenden
			if( actioncounter >= 35 ) {
				addError( actionBlockingPhrases[RandomUtils.nextInt(actionBlockingPhrases.length)],
						getRequest().getRequestURL() + 
						(getRequest().getQueryString() != null ? "?" + getRequest().getQueryString() : "") );
				
				return;
			}
			// Bei hoher Aktivitaet stattdessen nur eine Pause einlegen
			else if( actioncounter > 10 ) {
				sleep = 100 * actioncounter;
			}
			
			if( sleep > 0 ) {
				try {
					Thread.sleep(2500);
				}
				catch( InterruptedException e ) {
					LOG.error(e,e);
				}
			}
		}

		// Inaktivitaet zuruecksetzen
		user.setInactivity(0);
		
		if( (sessdata.getAttach() != null) && !sessdata.getAttach().equals("-1") ) {
			Session attachSession = (Session)getDB().get(Session.class, sessdata.getAttach());
			if( attachSession != null) {
				user.attachToUser(attachSession.getUser());	
			}
		}
		
		setActiveUser(user);
		
		return;
	}
	
	public void revalidate() {
		if( (currentSession == null && request.getParameter("sess") != null) || 
			(currentSession != null && !currentSession.equals(request.getParameter("sess"))) ) {
			errorList.clear();
			authenticateUser();
		}
	}

	public Database getDatabase() {
		return database;
	}

	public org.hibernate.Session getDB() {
		return session;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> query(String query, Class<T> classType) {
		return this.session.createQuery(query).list();
	}

	
	public BasicUser getActiveUser() {
		return activeUser;
	}

	public void setActiveUser(BasicUser user) {
		activeUser = user;
	}

	public void addError(String error) {
		errorList.add(new Error(error));
	}

	public void addError(String error, String link) {
		errorList.add(new Error(error,link));
	}

	public Error getLastError() {
		return errorList.get(errorList.size()-1);
	}

	public Error[] getErrorList() {
		return errorList.toArray(new Error[errorList.size()]);
	}

	public Request getRequest() {
		return request;
	}

	public Response getResponse() {
		return response;
	}
	
	public void setResponse(Response response) {
		this.response = response;
	}

	/**
	 * Gibt alle allokierten Resourcen wieder frei.
	 * Dieser Schritt sollte immer dann getaetigt werden, 
	 * wenn das Objekt nicht mehr benoetigt wird.
	 *
	 */
	public void free() {
		RuntimeException e = null;
		
		for( int i=0; i < this.listener.size(); i++ ) {
			try {
				listener.get(i).onContextDestory();
			}
			catch( RuntimeException ex ) {
				e = ex;
			}
		}

		if( transaction.isActive() && !transaction.wasRolledBack() ) {
			try {
				transaction.commit();
			}
			catch( RuntimeException ex ) {
				transaction.rollback();
				e = ex;
			}
		}
		database.close();
		session.close();
		ContextMap.removeContext();
		
		if( e != null ) {
			throw e;
		}
	}
	
	public void rollback() {
		if( transaction.isActive() ) {
			transaction.rollback();
		}
		transaction = session.beginTransaction();
	}
	
	public void commit() {
		if( transaction.isActive() && !transaction.wasRolledBack() ) {
			transaction.commit();
		}
		transaction = session.beginTransaction();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls) {
		/* TODO: Exceptions? */
		if( !contextSingletons.containsKey(cls) )  {
			if( !cls.isAnnotationPresent(ContextInstance.class) ) {
				LOG.fatal("ContextInstance Annotation not present: "+cls.getName());
				return null;
			}
			if( cls.getAnnotation(ContextInstance.class).value() != ContextInstance.Type.SINGLETON ) {
				LOG.fatal("Context-Class is not a singleton: "+cls.getName());
				return null;
			}
			try {
				Constructor<T> cons = cls.getConstructor(Context.class);
				contextSingletons.put(cls, cons.newInstance(this));
			}
			catch( Exception e ) {
				LOG.error(e,e);
				return null;
			}
		}

		return (T)contextSingletons.get(cls);
	}

	public String getSession() {
		String session = getRequest().getParameter("sess");
		if( session == null ) {
			session = "";
		}
		return session;
	}

	public Object getVariable(Class<?> cls, String varname) {
		if( variables.containsKey(cls) ) {
			Map<String,Object> map = variables.get(cls);
			if( map.containsKey(varname) ) {
				return map.get(varname);
			}
		}
		return null;
	}

	public void putVariable(Class<?> cls, String varname, Object value) {
		synchronized(variables) {
			if( !variables.containsKey(cls) ) {
				variables.put(cls, new HashMap<String,Object>());
			}
		}
		Map<String,Object> map = variables.get(cls);
		synchronized(map) {
			map.put(varname, value);
		}
	}

	public void registerListener(ContextListener listener) {
		this.listener.add(listener);
	}
}
