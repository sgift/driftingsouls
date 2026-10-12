package net.driftingsouls.ds2.server.ships;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShipTypeFlagTest
{
	@Test
	public void givenAFlagId_byFlag_shouldReturnTheMatchingEnum()
	{
		// setup
		String id = ShipTypeFlag.COLONIZER.getFlag();

		// run
		@SuppressWarnings({"OptionalGetWithoutIsPresent"})
		ShipTypeFlag shipTypeFlag = ShipTypeFlag.byFlag(id).get();

		// assert
		assertEquals(ShipTypeFlag.COLONIZER, shipTypeFlag);
	}

	@Test
	public void givenAStringOfFlagIds_parseFlags_shouldReturnTheMatchingEnumsAsSet()
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
	public void givenAStringWithExactlyOneFlagId_parseFlags_shouldReturnTheMatchingEnumAsSet()
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
	public void givenAStringOfFlagIdsWithExtraWhitespace_parseFlags_shouldReturnTheMatchingEnumsAsSet()
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
	public void givenAnEmptyString_parseFlags_shouldReturnAnEmptySet()
	{
		// setup
		String str = "";

		// run
		EnumSet<ShipTypeFlag> shipTypeFlags = ShipTypeFlag.parseFlags(str);

		// assert
		assertEquals(0, shipTypeFlags.size());
	}
}
