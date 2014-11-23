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
	public void gegebenEinNormalerAccountUndDiePassendenLogindaten_login_sollteDenLoginDurchfuehren() throws AuthenticationException
	{
		// setup
		ConfigService configService = new ConfigService();
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService);
		User user = persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));

		// run
		BasicUser authenticated = authenticationManager.login("foo", "bar", false);

		// assert
		assertSame(user, authenticated);
		assertSame(user, getContext().get(JavaSession.class).getUser());
	}

	@Test(expected = WrongPasswordException.class)
	public void gegebenEinNormalerAccountAberDasFalschePasswort_login_sollteEineWrongPasswordExceptionWerfen() throws AuthenticationException
	{
		// setup
		ConfigService configService = new ConfigService();
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService);
		persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));

		// run
		authenticationManager.login("foo", "barx", false);
	}

	@Test
	public void gegebenEinAccountImVacationModus_login_sollteDieZugehoerigeExceptionWerfen()
	{
		// setup
		ConfigService configService = new ConfigService();
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(configService);
		User user = persist(new User("foo", Common.md5("bar"), 0, "", new Cargo(), "foo@localhost"));
		user.setVacationCount(10);

		// run
		try
		{
			authenticationManager.login("foo", "bar", false);
			// assert

			fail("AccountInVacationModeException erwartet");
		}
		catch( AccountInVacationModeException e ) {
			assertEquals(10, e.getDauer());
		}
		catch (AuthenticationException e)
		{
			fail("AccountInVacationModeException erwartet");
		}
	}
}