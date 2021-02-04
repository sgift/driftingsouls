package net.driftingsouls.ds2.server.framework.authentication;

import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.user.authentication.AccountInVacationModeException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class DefaultAuthenticationManagerTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private DefaultAuthenticationManager authenticationManager;
	@Autowired
	private JavaSession javaSession;
	@Autowired
	private ConfigService configService;

	@Test
	@Transactional
	public void gegebenEinNormalerAccountUndDiePassendenLogindaten_login_sollteDenLoginDurchfuehren() throws AuthenticationException
	{
		// setup
		var user = new User(1, "foo", "foo", Common.md5("bar"), 0, "", "foo@localhost", configService);
		em.persist(user);

		// run
		BasicUser authenticated = authenticationManager.login("foo", "bar");

		// assert
		assertSame(user, authenticated);
		assertSame(user, javaSession.getUser());
	}

	@Test(expected = WrongPasswordException.class)
	@Transactional
	public void gegebenEinNormalerAccountAberDasFalschePasswort_login_sollteEineWrongPasswordExceptionWerfen() throws AuthenticationException
	{
		// setup
		var user = new User(1, "foo", "foo", Common.md5("bar"), 0, "", "foo@localhost", configService);
		em.persist(user);

		// run
		authenticationManager.login("foo", "barx");
	}

	@Test
	@Transactional
	public void gegebenEinAccountImVacationModus_login_sollteDieZugehoerigeExceptionWerfen()
	{
		// setup
		var user = new User(1, "foo", "foo", Common.md5("bar"), 0, "", "foo@localhost", configService);
		em.persist(user);
		user.setVacationCount(10);

		// run
		try
		{
			authenticationManager.login("foo", "bar");
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