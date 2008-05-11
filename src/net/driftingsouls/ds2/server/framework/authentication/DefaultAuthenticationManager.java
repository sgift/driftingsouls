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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Session;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

/**
 * Verwaltungsklasse fuer Aktionen rund um das ein- und ausloggen.
 * @author Christopher Jung
 *
 */
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static ServiceLoader<LoginEventListener> listenerList = ServiceLoader.load(LoginEventListener.class);
	
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
}
