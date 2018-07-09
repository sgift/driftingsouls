package net.driftingsouls.ds2.server.ships;

import java.util.EnumSet;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShipTypeFlagTest
{
	@Test
	public void gegebenEineIdEinesFlags_byFlag_sollteDasZugehoerigeEnumZurueckgeben()
	{
		// setup
		String id = ShipTypeFlag.COLONIZER.getFlag();

		// run
		ShipTypeFlag shipTypeFlag = ShipTypeFlag.byFlag(id);

		// assert
		assertEquals(ShipTypeFlag.COLONIZER, shipTypeFlag);
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenEineUnbekannteId_byFlag_sollteEineExceptionWerfen()
	{
		// setup
		String id = "123213"+getClass().getCanonicalName();

		// run
		ShipTypeFlag.byFlag(id);
	}

	@Test
	public void gegebenEinStringVonFlagIds_parseFlags_sollteDieZugehoerigenEnumsAlsSetZurueckgeben()
	{
		// setup
		String str = ShipTypeFlag.COLONIZER.getFlag()+" "+ShipTypeFlag.JAEGER.getFlag()+" "+ShipTypeFlag.TRADEPOST.getFlag();

		// run
		EnumSet<ShipTypeFlag> shipTypeFlags = ShipTypeFlag.parseFlags(str);

		// assert
		assertEquals(3, shipTypeFlags.size());
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.COLONIZER));
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.JAEGER));
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.TRADEPOST));
	}

	@Test
	public void gegebenEinStringMitGenauEinerFlagId_parseFlags_sollteDasZugehoerigeEnumAlsSetZurueckgeben()
	{
		// setup
		String str = ShipTypeFlag.COLONIZER.getFlag();

		// run
		EnumSet<ShipTypeFlag> shipTypeFlags = ShipTypeFlag.parseFlags(str);

		// assert
		assertEquals(1, shipTypeFlags.size());
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.COLONIZER));
	}

	@Test
	public void gegebenEinStringVonFlagIdsMitZusaetzlichenLeerzeichen_parseFlags_sollteDieZugehoerigenEnumsAlsSetZurueckgeben()
	{
		// setup
		String str = " "+ShipTypeFlag.COLONIZER.getFlag()+"  "+ShipTypeFlag.JAEGER.getFlag()+"  "+ShipTypeFlag.TRADEPOST.getFlag()+" ";

		// run
		EnumSet<ShipTypeFlag> shipTypeFlags = ShipTypeFlag.parseFlags(str);

		// assert
		assertEquals(3, shipTypeFlags.size());
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.COLONIZER));
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.JAEGER));
		assertTrue(shipTypeFlags.contains(ShipTypeFlag.TRADEPOST));
	}

	@Test
	public void gegebenEinLeererString_parseFlags_sollteEinLeeresSetZurueckgeben()
	{
		// setup
		String str = "";

		// run
		EnumSet<ShipTypeFlag> shipTypeFlags = ShipTypeFlag.parseFlags(str);

		// assert
		assertEquals(0, shipTypeFlags.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenEinStringMitUngueltigenFlagIds_parseFlags_sollteEinenFehlerWerfen()
	{
		// setup
		String str = ShipTypeFlag.COLONIZER.getFlag()+" "+ShipTypeFlag.JAEGER.getFlag()+" 1233424asfasf";

		// run
		ShipTypeFlag.parseFlags(str);
	}
}
