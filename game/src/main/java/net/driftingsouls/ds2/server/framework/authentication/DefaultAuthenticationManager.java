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
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Permission;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
public class DefaultAuthenticationManager implements AuthenticationManager {
	private static final Log log = LogFactory.getLog(DefaultAuthenticationManager.class);
	private static final ServiceLoader<LoginEventListener> loginListenerList = ServiceLoader.load(LoginEventListener.class);
	private static final ServiceLoader<AuthenticateEventListener> authListenerList = ServiceLoader.load(AuthenticateEventListener.class);

	private final ConfigService configService;
	private final JavaSession javaSession;
	@PersistenceContext
	private EntityManager em;

	@Autowired
	public DefaultAuthenticationManager(ConfigService configService, JavaSession javaSession)
	{
		this.configService = configService;
		this.javaSession = javaSession;
	}

	@Override
	public BasicUser login(String username, String password) throws AuthenticationException {
		log.info("Trying login for user: " + username);

		log.info("Context loaded");

		log.info("Session loaded");

		log.info("Checking login disabled");

		checkLoginDisabled();

		log.info("Login disabled checked");

		String encryptedPassword = Common.md5(password);

		log.info("Loading user from DB");

		BasicUser user = em.createQuery("from BasicUser where un=:username", BasicUser.class)
			.setParameter("username", username)
			.getSingleResult();

		log.info("User loaded");

		if( user == null ) {
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": ("+username+") <"+username+"> Password <"+password+"> ***UNGUELTIGER ACCOUNT***\n");

			log.info("User does not exist: " + username);

			throw new WrongPasswordException();
		}

		log.info("Checking password");

		if( !user.getPassword().equals(encryptedPassword) ) {
			user.setLoginFailedCount(user.getLoginFailedCount()+1);
			Common.writeLog("login.log", Common.date("j.m.Y H:i:s")+": ("+user.getId()+") <"+username+"> Password <"+password+"> ***LOGIN GESCHEITERT***\n");

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

		log.info("Checking for disabled user");

		checkDisabled(user);

		log.info("User not disabled");

		for( LoginEventListener listener : loginListenerList ) {
			listener.onLogin(user);
		}

		log.info("Login listener informed, finishing login");

		log.info("Login "+user.getId());
		Common.writeLog("login.log",Common.date( "j.m.Y H:i:s")+": ("+user.getId()+") <"+user.getUN()+">\n");

		javaSession.setUser(user);

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
		javaSession.setUser(null);
	}

	@Override
	public BasicUser adminLogin(BasicUser user, boolean attach) {
		javaSession.setUser(user);

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

		BasicUser user = javaSession.getUser();

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

		user.setInactivity(0);

		context.setActiveUser(user);

		int accessLevel = user.getAccessLevel();
		Set<Permission> permissions = new HashSet<>(user.getPermissions());

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
		if( user.getDisabled() )
		{
			Common.writeLog("login.log", Common.date( "j.m.Y H:i:s")+": ("+user.getId()+") <"+user.getUN()+"> ***ACC DISABLED***\n");

			throw new AccountDisabledException();
		}
	}
}
