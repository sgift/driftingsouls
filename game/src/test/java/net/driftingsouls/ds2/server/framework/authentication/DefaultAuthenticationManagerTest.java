package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;

import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultAuthenticationManagerTest extends DBSingleTransactionTest
{
	@Test
	public void givenARegularAccountAndMatchingCredentials_login_shouldLogIn() throws AuthenticationException
	{
		// setup
		ConfigService configService = new ConfigService(getEM());
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService, getEM());
		User user = persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));

		// run
		BasicUser authenticated = authenticationManager.login("foo", "bar");

		// assert
		assertSame(user, authenticated);
		assertSame(user, getContext().get(JavaSession.class).getUser());
	}

	@Test(expected = WrongPasswordException.class)
	public void givenARegularAccountButTheWrongPassword_login_shouldThrowAWrongPasswordException() throws AuthenticationException
	{
		// setup
		ConfigService configService = new ConfigService(getEM());
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService, getEM());
		persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));

		// run
		authenticationManager.login("foo", "barx");
	}

	@Test
	public void givenAnAccountInVacationMode_login_shouldThrowTheMatchingException()
	{
		// setup
		ConfigService configService = new ConfigService(getEM());
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService, getEM());
		User user = persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));
		user.setVacationCount(10);

		// run
		try
		{
			authenticationManager.login("foo", "bar");
			// assert

			fail("AccountInVacationModeException erwartet");
		}
		catch( AccountInVacationModeException e ) {
			assertEquals(10, e.getDuration());
		}
		catch (AuthenticationException e)
		{
			fail("AccountInVacationModeException erwartet");
		}
	}
}