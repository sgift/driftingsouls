package net.driftingsouls.ds2.server.ships;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShipTypeFlagTest
{
	@Test
	public void gegebenEineIdEinesFlags_byFlag_sollteDasZugehoerigeEnumZurueckgeben()
	{
		// setup
		String id = ShipTypeFlag.COLONIZER.getFlag();

		// run
		@SuppressWarnings("ConstantConditions")
		ShipTypeFlag shipTypeFlag = ShipTypeFlag.byFlag(id).get();

		// assert
		assertEquals(ShipTypeFlag.COLONIZER, shipTypeFlag);
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
}
