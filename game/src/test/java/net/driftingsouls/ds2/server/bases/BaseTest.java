package net.driftingsouls.ds2.server.bases;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BaseTest
{
	@Test
	public void kaputteAvailableSpawnableRess_setSpawnableRessAmount_sollteTrotzdemFunktionieren()
	{
		// setup
		Base base = new Base();
		base.setAvailableSpawnableRess("0");

		// run
		base.setSpawnableRessAmount(1, 100);

		// assert
		assertEquals("1,100", base.getAvailableSpawnableRess());
	}

	@Test
	public void noAvailableSpawnableRess_setSpawnableRessAmount_shouldSetTheAmount()
	{
		// setup
		Base base = new Base();
		base.setAvailableSpawnableRess(null);

		// run
		base.setSpawnableRessAmount(1, 100);

		// assert
		assertEquals("1,100", base.getAvailableSpawnableRess());
	}

	@Test
	public void existingAvailableSpawnableRess_setSpawnableRessAmount_shouldUpdateTheAmount()
	{
		// setup
		Base base = new Base();
		base.setAvailableSpawnableRess("1,50");

		// run
		base.setSpawnableRessAmount(1, 100);

		// assert
		assertEquals("1,100", base.getAvailableSpawnableRess());
	}

	@Test
	public void existingOtherAvailableSpawnableRess_setSpawnableRessAmount_shouldAddTheNewAmount()
	{
		// setup
		Base base = new Base();
		base.setAvailableSpawnableRess("2,50");

		// run
		base.setSpawnableRessAmount(1, 100);

		// assert
		assertEquals("2,50;1,100", base.getAvailableSpawnableRess());
	}
}