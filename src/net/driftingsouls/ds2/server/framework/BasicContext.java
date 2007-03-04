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
import java.util.Random;

import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.framework.pipeline.Error;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.framework.pipeline.Response;

/**
 * Eine einfache Klasse, welche das <code>Context</code>-Interface implementiert.
 * Die Klasse verwendet ein <code>Request</code> und <code>Response</code>-Objekt
 * fuer Ein- und Ausgabe.
 * 
 * @author Christopher Jung
 *
 */
public class BasicContext implements Context,Loggable {
	private Map<Integer,User> cachedUsers = new HashMap<Integer,User>();
	private Database database;
	private Request request;
	private Response response;
	private User activeUser = null;
	private List<Error> errorList = new ArrayList<Error>();
	private Map<Class<?>, Object> contextSingletons = new HashMap<Class<?>,Object>();
	private Map<Class<?>, Map<String,Object>> variables = new HashMap<Class<?>, Map<String, Object>>();
	private String currentSession = "";
	private List<ContextListener> listener = new ArrayList<ContextListener>();
	
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
		database = new Database();
		if( Configuration.getIntSetting("LOG_QUERIES") != 0 ) {
			database.setQueryLogStatus(true);
		}
		this.request = request;
		this.response = response;
		
		revalidate();
		
		ContextMap.addContext(this);
	}
	
	private void authenticateUser() {
		Database db = getDatabase();
		
		String sess = request.getParameter("sess");
		currentSession = sess;
		
		String errorurl = Configuration.getSetting("URL")+"ds?module=portal&action=login";
	
		if( (sess == null) || sess.equals("") ) {
			return;
		}
		
		long time = Common.time();
		SQLResultRow sessdata = db.prepare("SELECT * FROM sessions WHERE session=?")
								.first(sess);
		
		if( sessdata.isEmpty() ) {
			addError( "Fehler: Sie sind offenbar nicht eingeloggt", errorurl );

			return;
		}
		
		if( sessdata.getInt("tick") != 0 ) {
			addError( "Im Moment werden einige Tick-Berechnungen f&uuml;r sie durchgef&uuml;hrt. Bitte haben sie daher ein wenig Geduld", 
					getRequest().getRequestURL() + 
						(getRequest().getQueryString() != null ? "?" + getRequest().getQueryString() : "") 
			);

			return;
		}
	
		User user = new User( this, sessdata.getInt("id"), sessdata );
		if( !user.hasFlag(User.FLAG_DISABLE_IP_SESSIONS) && !sessdata.getString("ip").contains("<"+getRequest().getRemoteAddress()+">") ) {
			addError( "Fehler: Diese Session ist einer anderen IP zugeordnet", errorurl );

			return;
		}
	
		if( !user.hasFlag(User.FLAG_DISABLE_AUTO_LOGOUT) && (Common.time() - sessdata.getInt("lastaction") > Configuration.getIntSetting("AUTOLOGOUT_TIME")) ) {
			db.update("DELETE FROM sessions WHERE id='",sessdata.getInt("id"),"'");
			addError( "Fehler: Diese Session ist bereits abgelaufen", errorurl );

			return;
		}
		
		if( (user.getVacationCount() > 0) && (user.getWait4VacationCount() == 0) ) {
			addError( "Fehler: Dieser Account befindet sich noch im Vacationmodus", errorurl );

			return;
		}
		
		db.prepare("UPDATE sessions SET lastaction=? WHERE session=?").update(time, sess);
		
		if( !user.hasFlag(User.FLAG_NO_ACTION_BLOCKING) ) {
			// Alle zwei Sekunden Counter um 1 reduzieren, sofern mindestens 5 Sekunden Pause vorhanden waren
			int reduce = (int)(time - sessdata.getInt("lastaction")-2);
			if( time < sessdata.getInt("lastaction") + 5 ) {
				reduce = -1;
			}
			int actioncounter = sessdata.getInt("actioncounter")-reduce;
			if( actioncounter < 0 ) {
				actioncounter = 0;
			}

			if( reduce > 0 ) {
				db.prepare("UPDATE sessions SET actioncounter=IF(actioncounter- ? <0,0,actioncounter- ?) WHERE session=?")
					.update(reduce, reduce, sess);
			}
			else if( reduce < 0 ) {
				db.prepare("UPDATE sessions SET actioncounter=actioncounter+1 WHERE session=?").update(sess);
			}
			
			// Bei viel zu hoher Aktivitaet einfach die Ausfuehrung mit einem Fehler beenden
			if( actioncounter > 25 ) {
				addError( actionBlockingPhrases[new Random().nextInt(actionBlockingPhrases.length)], errorurl );

				return;
			}
			// Bei hoher Aktivitaet stattdessen nur eine Pause von 1 oder 2 Sekunden einlegen
			else if( actioncounter > 20 ) {
				try {
					Thread.sleep(2000);
				}
				catch( InterruptedException e ) {
					LOG.error(e,e);
				}
			}
			else if( actioncounter > 10 ) {
				try {
					Thread.sleep(1000);
				}
				catch( InterruptedException e ) {
					LOG.error(e,e);
				}
			}
		}
		
		// Inaktivitaet zuruecksetzen
		db.update("UPDATE users SET inakt=0 WHERE id='",sessdata.getInt("id"),"'");
		
		if( !"".equals(sessdata.getString("attach")) && !sessdata.getString("attach").equals("-1") ) {
			SQLResultRow row = db.first("SELECT id FROM sessions WHERE session='",sessdata.getString("attach"),"'");
			if( !row.isEmpty() ) {
				user.attachToUser(row.getInt("id"));	
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

	public User getCachedUser(int id) {
		return cachedUsers.get(id);
	}

	public User createUserObject(int id) {
		if( cachedUsers.containsKey(id) ) {
			return cachedUsers.get(id);
		}
		return new User( this, id );
	}
	
	public Database getDatabase() {
		return database;
	}

	public User getActiveUser() {
		return activeUser;
	}

	public void setActiveUser(User user) {
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

	public void cacheUser(User userobj) {
		cachedUsers.put( userobj.getID(), userobj );
	}

	public Request getRequest() {
		return request;
	}

	public Response getResponse() {
		return response;
	}

	/**
	 * Gibt alle allokierten Resourcen wieder frei.
	 * Dieser Schritt sollte immer dann getaetigt werden, 
	 * wenn das Objekt nicht mehr benoetigt wird.
	 *
	 */
	public void free() {
		for( int i=0; i < this.listener.size(); i++ ) {
			listener.get(i).onContextDestory();
		}
		
		database.close();
		ContextMap.removeContext();
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

	public UserIterator createUserIterator(Object ... query) {
		return new UserIterator(this, getDatabase().query(query));
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
