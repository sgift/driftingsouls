package net.driftingsouls.ds2.server.entities;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserFlagTest
{
	@Test
	public void gegebenEineIdEinesFlags_byFlag_sollteDasZugehoerigeEnumZurueckgeben()
	{
		// setup
		String id = UserFlag.HIDE.getFlag();

		// run
		@SuppressWarnings("ConstantConditions")
		UserFlag userFlag = UserFlag.byFlag(id).get();

		// assert
		assertEquals(UserFlag.HIDE, userFlag);
	}

	@Test
	public void gegebenEinStringVonFlagIds_parseFlags_sollteDieZugehoerigenEnumsAlsSetZurueckgeben()
	{
		// setup
		String str = UserFlag.HIDE.getFlag()+" "+UserFlag.NOOB.getFlag()+" "+UserFlag.ORDER_MENU.getFlag();

		// run
		EnumSet<UserFlag> userFlags = UserFlag.parseFlags(str);

		// assert
		assertEquals(3, userFlags.size());
		assertTrue(userFlags.contains(UserFlag.HIDE));
		assertTrue(userFlags.contains(UserFlag.NOOB));
		assertTrue(userFlags.contains(UserFlag.ORDER_MENU));
	}

	@Test
	public void gegebenEinStringMitGenauEinerFlagId_parseFlags_sollteDasZugehoerigeEnumAlsSetZurueckgeben()
	{
		// setup
		String str = UserFlag.HIDE.getFlag();

		// run
		EnumSet<UserFlag> userFlags = UserFlag.parseFlags(str);

		// assert
		assertEquals(1, userFlags.size());
		assertTrue(userFlags.contains(UserFlag.HIDE));
	}

	@Test
	public void gegebenEinStringVonFlagIdsMitZusaetzlichenLeerzeichen_parseFlags_sollteDieZugehoerigenEnumsAlsSetZurueckgeben()
	{
		// setup
		String str = " "+UserFlag.HIDE.getFlag()+"  "+UserFlag.NOOB.getFlag()+"  "+UserFlag.ORDER_MENU.getFlag()+" ";

		// run
		EnumSet<UserFlag> userFlags = UserFlag.parseFlags(str);

		// assert
		assertEquals(3, userFlags.size());
		assertTrue(userFlags.contains(UserFlag.HIDE));
		assertTrue(userFlags.contains(UserFlag.NOOB));
		assertTrue(userFlags.contains(UserFlag.ORDER_MENU));
	}

	@Test
	public void gegebenEinLeererString_parseFlags_sollteEinLeeresSetZurueckgeben()
	{
		// setup
		String str = "";

		// run
		EnumSet<UserFlag> userFlags = UserFlag.parseFlags(str);

		// assert
		assertEquals(0, userFlags.size());
	}
}
