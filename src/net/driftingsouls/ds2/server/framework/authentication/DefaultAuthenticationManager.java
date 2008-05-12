/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.framework.authentication;

import java.util.ServiceLoader;

import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Session;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Verwaltungsklasse fuer Aktionen rund um das ein- und ausloggen.
 * @author Christopher Jung
 *
 */
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static final Log log = LogFactory.getLog(DefaultAuthenticationManager.class);
	private static final ServiceLoader<LoginEventListener> listenerList = ServiceLoader.load(LoginEventListener.class);
	
	private static final String[] actionBlockingPhrases = {
		"Immer mit der Ruhe!\nDer arme Server ist schon total erschl&ouml;pft.\nG&ouml;nn ihm mal ein oder zwei Minuten Pause :)",
		"Laaaaangsaaaaam!\nDeine Maus hat ja schon nen Krampf von dem vielen geklicke bekommen!\nMach mal eine kleine Pause und g&ouml;nn ihr die Entspannung...",
		"In der Ruhe liegt die Kraft!\nNur der arme Server hat wegen deiner Klickerei keine Ruhe und somit auch keine Kraft mehr. Lass ihm eine kleine Verschnaufpause und mach dann weiter.",
		"Vorsicht mein junger Padawan!\nBei diesem Klicktempo k&ouml;nntest du aus versehen auf den Selbstzerst&ouml;rungsknopf dr&uuml;cken! Mach eine kurze Pause und klicke dann langsam und gewissenhaft weiter..."
	};
	
	public Session login(String username, String password, boolean useGfxPak) throws AuthenticationException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Request request = context.getRequest();
		
		checkLoginDisabled(context);
		
		String enc_pw = Common.md5(password);

		BasicUser user = (BasicUser)db.createQuery("from BasicUser where un=:username")
			.setString("username", username)
			.uniqueResult();
		
		if( user == null ) {
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+username+") <"+username+"> Password <"+password+"> ***UNGUELTIGER ACCOUNT*** von Browser <"+request.getUserAgent()+">\n");
			
			throw new WrongPasswordException();
		}

		if( !user.getPassword().equals(enc_pw) ) {
			user.setLoginFailedCount(user.getLoginFailedCount()+1);
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+username+"> Password <"+password+"> ***LOGIN GESCHEITERT*** von Browser <"+request.getUserAgent()+">\n");
			
			throw new WrongPasswordException();
		}
		if( user.getDisabled() ) {
			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+username+"> Password <"+password+"> ***ACC DISABLED*** von Browser <"+request.getUserAgent()+">\n");

			db.createQuery("delete from Session where user=?")
				.setEntity(0, user)
				.executeUpdate();
			
			throw new AccountDisabledException();
		}
		
		for( LoginEventListener listener : listenerList ) {
			listener.onLogin(user);
		}
		
		checkTickInProgress(context, user);

		Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Login von Browser <"+request.getUserAgent()+">\n");

		return createNewSession(context, user, useGfxPak);
	}

	private void checkTickInProgress(Context context, BasicUser user) throws TickInProgressException {
		org.hibernate.Session db = context.getDB();
		
		Session session = (Session)db.createQuery("from Session where user=? and tick!=0")
			.setEntity(0, user)
			.uniqueResult();

		if( session != null ) {
			throw new TickInProgressException();
		}
	}

	private Session createNewSession(Context context, BasicUser user, boolean useGfxPak) {
		org.hibernate.Session db = context.getDB();
		
		db.createQuery("delete from Session where user=? and attach is null")
			.setEntity(0, user)
			.executeUpdate();
		
		Session session = new Session(user);
		session.setIP("<"+context.getRequest().getRemoteAddress()+">");
		session.setUseGfxPak(useGfxPak);
		db.persist(session);
		
		context.commit();
		return session;
	}

	private void checkLoginDisabled(Context context) throws LoginDisabledException {
		Database database = context.getDatabase();
		
		String disablelogin = database.first("SELECT disablelogin FROM config").getString("disablelogin");
		if( !disablelogin.isEmpty() ) {
			throw new LoginDisabledException(disablelogin);
		}
	}
	
	public void logout() {
		Context context = ContextMap.getContext();
		
		context.getDB().createQuery("delete from Session where session= :sess or attach= :sess")
			.setString("sess", context.getSession())
			.executeUpdate();
	}

	public Session adminLogin(BasicUser user, boolean attach) throws AuthenticationException {
		Context context = ContextMap.getContext();
		
		Session session = new Session(user);
		session.setIP("<"+context.getRequest().getRemoteAddress()+">");
		session.setUseGfxPak(false);
		if( attach ) {
			session.setAttach(context.getSession());
		}
		context.getDB().save(session);
		
		return session;
	}
	
	public void authenticateCurrentSession() {
		Context context = ContextMap.getContext();
		
		Request request = context.getRequest();
		org.hibernate.Session db = context.getDB();
		
		String sess = request.getParameter("sess");
		
		String errorurl = Configuration.getSetting("URL")+"ds?module=portal&action=login";
	
		if( (sess == null) || sess.equals("") ) {
			return;
		}
		
		long time = Common.time();
		Session sessdata = (Session)db.get(Session.class, sess);
		
		if( sessdata == null ) {
			context.addError( "Sie sind offenbar nicht eingeloggt", errorurl );

			return;
		}

		if( sessdata.getTick() ) {
			context.addError( "Im Moment werden einige Tick-Berechnungen f&uuml;r sie durchgef&uuml;hrt. Bitte haben sie daher ein wenig Geduld", 
					request.getRequestURL() + 
						(request.getQueryString() != null ? "?" + request.getQueryString() : "") 
			);

			return;
		}
	
		BasicUser user = sessdata.getUser();
		user.setSessionData(sessdata);
		if( !user.hasFlag(BasicUser.FLAG_DISABLE_IP_SESSIONS) && !sessdata.isValidIP(request.getRemoteAddress()) ) {
			context.addError( "Diese Session ist einer anderen IP zugeordnet", errorurl );

			return;
		}
	
		if( !user.hasFlag(BasicUser.FLAG_DISABLE_AUTO_LOGOUT) && (Common.time() - sessdata.getLastAction() > Configuration.getIntSetting("AUTOLOGOUT_TIME")) ) {
			db.delete(sessdata);
			context.addError( "Diese Session ist bereits abgelaufen", errorurl );

			return;
		}
		
		final long oldtime = sessdata.getLastAction();
		
		sessdata.setLastAction(time);
		context.commit();
		
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
				context.addError( actionBlockingPhrases[RandomUtils.nextInt(actionBlockingPhrases.length)],
						request.getRequestURL() + 
						(request.getQueryString() != null ? "?" + request.getQueryString() : "") );
				
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
					log.error(e,e);
				}
			}
		}

		// Inaktivitaet zuruecksetzen
		user.setInactivity(0);
		
		if( (sessdata.getAttach() != null) && !sessdata.getAttach().equals("-1") ) {
			Session attachSession = (Session)db.get(Session.class, sessdata.getAttach());
			if( attachSession != null) {
				user.attachToUser(attachSession.getUser());	
			}
		}
		
		context.setActiveUser(user);
		
		return;
	}
}
