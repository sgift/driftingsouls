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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.*;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Verwaltungsklasse fuer Aktionen rund um das ein- und ausloggen.
 * @author Christopher Jung
 *
 */
@Service
@Lazy
@RequestScope
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static final Log log = LogFactory.getLog(DefaultAuthenticationManager.class);
	private static final ServiceLoader<LoginEventListener> loginListenerList = ServiceLoader.load(LoginEventListener.class);
	private static final ServiceLoader<AuthenticateEventListener> authListenerList = ServiceLoader.load(AuthenticateEventListener.class);
	private static final boolean DEV_MODE = !Configuration.isProduction();

	private final ConfigService configService;
	private final EntityManager db;

	@Autowired
	public DefaultAuthenticationManager(ConfigService configService, EntityManager db)
	{
		this.configService = configService;
        this.db = db;
    }

	@Override
	public BasicUser login(String username, String password) throws AuthenticationException {
		log.info("Trying login for user: " + username);

		Context context = ContextMap.getContext();

		log.info("Context loaded");

		var db = context.getEM();

		log.info("Session loaded");

		Request request = context.getRequest();

		log.info("Checking login disabled");

		checkLoginDisabled();

		log.info("Login disabled checked");

		String enc_pw = Common.md5(password);

		log.info("Loading user from DB");

		BasicUser user = (BasicUser)db.createQuery("from BasicUser where un=:username", BasicUser.class)
			.setParameter("username", username)
			.getResultList().stream().findFirst().orElse(null);

		log.info("User loaded");

		if( user == null ) {
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+username+") <"+username+"> Password <"+password+"> ***UNGUELTIGER ACCOUNT*** von Browser <"+request.getUserAgent()+">\n");

			log.info("User does not exist: " + username);

			throw new WrongPasswordException();
		}

		log.info("Checking password");

		if( !user.getPassword().equals(enc_pw) ) {
			user.setLoginFailedCount(user.getLoginFailedCount()+1);
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+username+"> Password <"+password+"> ***LOGIN GESCHEITERT*** von Browser <"+request.getUserAgent()+">\n");

			log.info("Wrong password");

			throw new WrongPasswordException();
		}

		log.info("Password checked, checking vacation");

		checkAccountNotInVacationMode(user);

		log.info("vaction checked, finishing login");

		return finishLogin(user);
	}

	private void checkAccountNotInVacationMode(BasicUser basicuser)
	{
		User user = (User) basicuser;
		if (user.isInVacation())
		{
			throw new AccountInVacationModeException(user.getVacationCount());
		}
	}

	private BasicUser finishLogin(BasicUser user) throws AuthenticationException
	{
		Context context = ContextMap.getContext();
		Request request = context.getRequest();

		log.info("Checking for disabled user");

		checkDisabled(user);

		log.info("User not disabled");

		for( LoginEventListener listener : loginListenerList ) {
			listener.onLogin(user);
		}

		log.info("Login listener informed, finishing login");

		log.info("Login "+user.getId());
		Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": <"+request.getRemoteAddress()+"> ("+user.getId()+") <"+user.getUN()+"> Login von Browser <"+request.getUserAgent()+">\n");

		JavaSession jsession = context.get(JavaSession.class);
		jsession.setUser(user);
		jsession.setIP("<"+context.getRequest().getRemoteAddress()+">");

		return user;
	}

	private void checkLoginDisabled() throws LoginDisabledException
	{
		String disablelogin = configService.getValue(WellKnownConfigValue.DISABLE_LOGIN);
		if (!disablelogin.isEmpty())
		{
			throw new LoginDisabledException(disablelogin);
		}
	}

	@Override
	public void logout() {
		Context context = ContextMap.getContext();
		JavaSession session = context.get(JavaSession.class);
		if(session != null)
		{
			db.createQuery("delete from PermanentSession where user=:user")
			  .setParameter("user", session.getUser())
			  .executeUpdate();
		}

		context.remove(JavaSession.class);
	}

	@Override
	public BasicUser adminLogin(BasicUser user, boolean attach) {
		Context context = ContextMap.getContext();

		BasicUser oldUser = context.getActiveUser();

		JavaSession jsession = context.get(JavaSession.class);
		jsession.setUser(user);
		jsession.setIP("<"+context.getRequest().getRemoteAddress()+">");
		if( attach && user.getId() != oldUser.getId() ) {
			jsession.setAttach(oldUser);
		}

		return user;
	}

	@Override
	public boolean authenticateCurrentSession() {
		Context context = ContextMap.getContext();

		try
		{
			checkLoginDisabled();
		}
		catch (LoginDisabledException e)
		{
			return false;
		}

		JavaSession jsession = context.get(JavaSession.class);

		BasicUser user;
		if( DEV_MODE && context.getRequest().getParameterInt("devUserId") != 0 )
		{
			// In der lokalen Entwicklungsumgebung den schnellen Wechsel zwischen Usern erlauben
			user = (BasicUser)context.getDB().get(BasicUser.class, context.getRequest().getParameterInt("devUserId"));
			if( user != null )
			{
				try
				{
					finishLogin(user);
				}
				catch (AuthenticationException e)
				{
					return false;
				}
			}
		}
		else
		{
			// User aus der Session laden
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

		for( AuthenticateEventListener listener : authListenerList ) {
			listener.onAuthenticate(user);
		}

		if( jsession.getAttach() != null )
		{
			user.attachToUser(jsession.getAttach());
		}
		else
		{
			user.setInactivity(0);
		}

		context.setActiveUser(user);

		int accessLevel = user.getAccessLevel();
		Set<Permission> permissions = new HashSet<>(user.getPermissions());
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

			db.createQuery("delete from PermanentSession where user=:user")
				.setEntity("user", user)
				.executeUpdate();

			throw new AccountDisabledException();
		}
	}
}
