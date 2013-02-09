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

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.framework.pipeline.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

/**
 * Verwaltungsklasse fuer Aktionen rund um das ein- und ausloggen.
 * @author Christopher Jung
 *
 */
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static final Log log = LogFactory.getLog(DefaultAuthenticationManager.class);
	private static final ServiceLoader<LoginEventListener> loginListenerList = ServiceLoader.load(LoginEventListener.class);
	private static final ServiceLoader<AuthenticateEventListener> authListenerList = ServiceLoader.load(AuthenticateEventListener.class);

	@Override
	public BasicUser login(String username, String password, boolean useGfxPak, boolean rememberMe) throws AuthenticationException {
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

		return finishLogin(user, useGfxPak, rememberMe);
	}

	private BasicUser finishLogin(BasicUser user, boolean useGfxPack, boolean rememberMe) throws AuthenticationException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Request request = context.getRequest();

		checkDisabled(user);

		for( LoginEventListener listener : loginListenerList ) {
			listener.onLogin(user);
		}

		log.info("Login "+user.getId());
		Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Login von Browser <"+request.getUserAgent()+">\n");

		JavaSession jsession = context.get(JavaSession.class);
		jsession.setUser(user);
		jsession.setUseGfxPak(useGfxPack);
		jsession.setIP("<"+context.getRequest().getRemoteAddress()+">");


		if(rememberMe)
		{
			UUID uuid = UUID.randomUUID();
			String value = user.getId() + "####" + uuid;
			context.getResponse().setCookie("dsRememberMe", value, 157680000);

			PermanentSession permanentSession = new PermanentSession();
			permanentSession.setTick(context.get(ContextCommon.class).getTick());
			permanentSession.setToken(Common.md5(uuid.toString()));
			permanentSession.setUserId(user.getId());
			permanentSession.setUseGfxPack(useGfxPack);

			db.save(permanentSession);
		}

		return user;
	}

	private void checkLoginDisabled(Context context) throws LoginDisabledException {
		ConfigValue value = (ConfigValue)context.getDB().get(ConfigValue.class, "disablelogin");
		String disablelogin = value.getValue();
		if( !disablelogin.isEmpty() ) {
			throw new LoginDisabledException(disablelogin);
		}
	}

	@Override
	public void logout() {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		JavaSession session = context.get(JavaSession.class);
		if(session != null)
		{
			db.createQuery("delete from PermanentSession where userId=:userId")
			  .setParameter("userId", session.getUser().getId())
			  .executeUpdate();
		}

		context.remove(JavaSession.class);
	}

	@Override
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

	@Override
	public boolean authenticateCurrentSession(boolean automaticAccess) {
		Context context = ContextMap.getContext();

		try
		{
			checkLoginDisabled(context);
		}
		catch (LoginDisabledException e)
		{
			return false;
		}

		JavaSession jsession = context.get(JavaSession.class);

		BasicUser user;
		if( (jsession == null || jsession.getUser() == null) && !automaticAccess )
		{
			try
			{
				user = checkRememberMe();
			}
			catch(AuthenticationException e)
			{
				return false;
			}
		}
		else
		{
			user = jsession.getUser();
		}

		if(user == null)
		{
			return false;
		}

		try
		{
			checkDisabled(user);
		}
		catch (AccountDisabledException e)
		{
			return false;
		}

		user.setSessionData(jsession.getUseGfxPak());

		try {
			for( AuthenticateEventListener listener : authListenerList ) {
				listener.onAuthenticate(user);
			}
		}
		catch( AuthenticationException e ) {
			return false;
		}

		if(!automaticAccess)
		{
			user.setInactivity(0);
		}

		if( jsession.getAttach() != null ) {
			user.attachToUser(jsession.getAttach());
		}

		context.setActiveUser(user);

		int accessLevel = user.getAccessLevel();
		Set<Permission> permissions = new HashSet<Permission>(user.getPermissions());
		if( jsession.getAttach() != null && accessLevel < jsession.getAttach().getAccessLevel() )
		{
			accessLevel = jsession.getAttach().getAccessLevel();
			permissions.addAll(jsession.getAttach().getPermissions());
		}

		context.setPermissionResolver(
				new PermissionDelegatePermissionResolver(
						new AccessLevelPermissionResolver(accessLevel),
						permissions
				)
		);

		return true;
	}

	/**
	 * Checks, if the user account has been disabled.
	 *
	 * @param user The current user account.
	 */
	public void checkDisabled(BasicUser user) throws AccountDisabledException
	{
		Context context = ContextMap.getContext();
		if( user.getDisabled() )
		{
			Session db = context.getDB();
			Request request = context.getRequest();
			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> ***ACC DISABLED*** von Browser <"+request.getUserAgent()+">\n");

			db.createQuery("delete from PermanentSession where userId=:user")
				.setEntity("user", user)
				.executeUpdate();

			throw new AccountDisabledException();
		}
	}

	/**
	 * Checks, if the player is remembered by ds.
	 *
	 * @return <code>true</code> if ds remembers the player, <code>false</code> otherwise.
	 */
	@Override
	public boolean isRemembered()
	{
		return getPermanentSession() != null;
	}

	//Prueft, ob der User einen remember me Token hat, um automatisch neu authentifiziert zu werden
	private BasicUser checkRememberMe() throws AuthenticationException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		PermanentSession session = getPermanentSession();
		if(session == null)
		{
			return null;
		}

		db.delete(session);
		BasicUser user = (BasicUser)db.get(BasicUser.class, session.getUserId());
		user.setSessionData(session.isUseGfxPack());

		return finishLogin(user, session.isUseGfxPack(), true);
	}

	private PermanentSession getPermanentSession()
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Request request = context.getRequest();

		String value = request.getCookie("dsRememberMe");

		if(value == null)
		{
			return null;
		}

		if(!value.contains("####"))
		{
			return null;
		}

		String[] parts = value.split("####");
		int userId = Integer.parseInt(parts[0]);
		String token = parts[1];

		PermanentSession session = (PermanentSession)db
			.createQuery("from PermanentSession where userId=:userId and token=:token")
			.setParameter("userId", userId)
			.setParameter("token", Common.md5(token))
			.uniqueResult();

		return session;
	}
}
