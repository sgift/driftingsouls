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
	// The legacy hash of "test123": md5 is cc03e747..., the legacy encoding drops the zero of the 0x03 byte.
	private static final String LEGACY_TEST123 = "cc3e747a6afbbcbf8be7668acfebee5";

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
	public void givenA31CharacterLegacyHash_login_shouldLogInAndRehashWithBcrypt() throws AuthenticationException
	{
		// setup
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(new ConfigService(getEM()), getEM());
		User user = persist(new User("foo", LEGACY_TEST123, 0, "", new Cargo(), "foo@localhost"));

		// run
		BasicUser authenticated = authenticationManager.login("foo", "test123");

		// assert
		assertSame(user, authenticated);
		assertTrue(user.getPassword(), user.getPassword().startsWith("$2a$"));
		assertTrue(new PasswordHasher().matches("test123", user.getPassword()));
	}

	@Test
	public void givenARehashedLegacyAccount_login_shouldLogInAgainViaBcrypt() throws AuthenticationException
	{
		// setup
		User user = persist(new User("foo", LEGACY_TEST123, 0, "", new Cargo(), "foo@localhost"));
		new DefaultAuthenticationManager(new ConfigService(getEM()), getEM()).login("foo", "test123");
		String rehashed = user.getPassword();

		// run
		BasicUser authenticated = new DefaultAuthenticationManager(new ConfigService(getEM()), getEM()).login("foo", "test123");

		// assert
		assertSame(user, authenticated);
		assertEquals(rehashed, user.getPassword());
	}

	@Test
	public void givenALegacyHashButTheWrongPassword_login_shouldKeepTheLegacyHash()
	{
		// setup
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(new ConfigService(getEM()), getEM());
		User user = persist(new User("foo", LEGACY_TEST123, 0, "", new Cargo(), "foo@localhost"));

		// run
		try
		{
			authenticationManager.login("foo", "test124");
			fail("WrongPasswordException erwartet");
		}
		catch (AuthenticationException e)
		{
			assertTrue(e instanceof WrongPasswordException);
		}

		// assert
		assertEquals(LEGACY_TEST123, user.getPassword());
	}

	@Test
	public void givenABcryptHash_login_shouldLogInWithoutRehashing() throws AuthenticationException
	{
		// setup
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(new ConfigService(getEM()), getEM());
		String hash = new PasswordHasher().hash("bar");
		User user = persist(new User("foo", hash, 0, "", new Cargo(), "foo@localhost"));

		// run
		BasicUser authenticated = authenticationManager.login("foo", "bar");

		// assert
		assertSame(user, authenticated);
		assertEquals(hash, user.getPassword());
	}

	@Test(expected = WrongPasswordException.class)
	public void givenABcryptHashButTheWrongPassword_login_shouldThrowAWrongPasswordException() throws AuthenticationException
	{
		// setup
		DefaultAuthenticationManager authenticationManager = new DefaultAuthenticationManager(new ConfigService(getEM()), getEM());
		persist(new User("foo", new PasswordHasher().hash("bar"), 0, "", new Cargo(), "foo@localhost"));

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