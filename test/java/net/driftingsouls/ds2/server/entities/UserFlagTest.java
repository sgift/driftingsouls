package net.driftingsouls.ds2.server.entities;

import java.util.EnumSet;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserFlagTest
{
	@Test
	public void gegebenEineIdEinesFlags_byFlag_sollteDasZugehoerigeEnumZurueckgeben()
	{
		// setup
		String id = UserFlag.HIDE.getFlag();

		// run
		UserFlag userFlag = UserFlag.byFlag(id);

		// assert
		assertEquals(UserFlag.HIDE, userFlag);
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenEineUnbekannteId_byFlag_sollteEineExceptionWerfen()
	{
		// setup
		String id = "123213"+getClass().getCanonicalName();

		// run
		UserFlag.byFlag(id);
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

	@Test(expected = IllegalArgumentException.class)
	public void gegebenEinStringMitUngueltigenFlagIds_parseFlags_sollteEinenFehlerWerfen()
	{
		// setup
		String str = UserFlag.HIDE.getFlag()+" "+UserFlag.NOOB.getFlag()+" 1233424asfasf";

		// run
		UserFlag.parseFlags(str);
	}
}
