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
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Verwaltungsklasse fuer Aktionen rund um das ein- und ausloggen.
 * @author Christopher Jung
 *
 */
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static final Log log = LogFactory.getLog(DefaultAuthenticationManager.class);
	private static final ServiceLoader<LoginEventListener> loginListenerList = ServiceLoader.load(LoginEventListener.class);
	private static final ServiceLoader<AuthenticateEventListener> authListenerList = ServiceLoader.load(AuthenticateEventListener.class);
	
	public BasicUser login(String username, String password, boolean useGfxPak) throws AuthenticationException {
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
		
		for( LoginEventListener listener : loginListenerList ) {
			listener.onLogin(user);
		}

		log.info("Login "+user.getId());
		Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Login von Browser <"+request.getUserAgent()+">\n");

		JavaSession jsession = context.get(JavaSession.class);
		jsession.setUser(user);
		jsession.setUseGfxPak(useGfxPak);
		jsession.setIP("<"+context.getRequest().getRemoteAddress()+">");
		
		return user;
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
		context.remove(JavaSession.class);
	}

	public BasicUser adminLogin(BasicUser user, boolean attach) throws AuthenticationException {
		Context context = ContextMap.getContext();
		
		BasicUser oldUser = context.getActiveUser();

		JavaSession jsession = context.get(JavaSession.class);
		jsession.setUser(user);
		jsession.setIP("<"+context.getRequest().getRemoteAddress()+">");
		jsession.setUseGfxPak(false);
		if( attach ) {
			jsession.setAttach(oldUser);
		}
		
		return user;
	}
	
	public void authenticateCurrentSession() {
		Context context = ContextMap.getContext();

		String errorurl = Configuration.getSetting("URL")+"ds?module=portal&action=login";
		
		JavaSession jsession = context.get(JavaSession.class);
		
		if( jsession == null || jsession.getUser() == null ) {
			return;
		}

		BasicUser user = jsession.getUser();
		user.setSessionData(jsession.getUseGfxPak());
		if( !user.hasFlag(BasicUser.FLAG_DISABLE_IP_SESSIONS) && !jsession.isValidIP(context.getRequest().getRemoteAddress()) ) {
			context.addError( "Diese Session ist einer anderen IP zugeordnet", errorurl );

			return;
		}
		
		try {
			for( AuthenticateEventListener listener : authListenerList ) {
				listener.onAuthenticate(user);
			}
		}
		catch( AuthenticationException e ) {
			return;
		}

		// Inaktivitaet zuruecksetzen
		user.setInactivity(0);
		
		if( jsession.getAttach() != null ) {
			user.attachToUser(jsession.getAttach());
		}
		
		context.setActiveUser(user);
		
		return;
	}
}
