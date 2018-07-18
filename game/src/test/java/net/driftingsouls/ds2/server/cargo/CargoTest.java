/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.cargo;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class CargoTest
{
	@Test
	public void testNewEmptyCargo()
	{
		Cargo cargo = new Cargo();
		assertThat(cargo.save(), is(""));
		assertThat(cargo.getItems().size(), is(0));
		assertThat(cargo.getItemArray().size(), is(0));
	}

	@Test
	public void testCargoFromString()
	{
		Cargo cargo = new Cargo(Cargo.Type.AUTO, "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20|50|0|0;29|1|0|0");
		assertThat(cargo.getResourceCount(new ItemID(20)), is(50L));
		assertThat(cargo.getResourceCount(new ItemID(29)), is(1L));
		assertThat(cargo.getItemArray().size(), is(2));

		Cargo cargo2 = new Cargo(Cargo.Type.AUTO, "25|51|0|0;29|1|0|0");
		assertThat(cargo2.getResourceCount(new ItemID(25)), is(51L));
		assertThat(cargo2.getResourceCount(new ItemID(29)), is(1L));
		assertThat(cargo2.getItemArray().size(), is(2));

		Cargo cargo3 = new Cargo(Cargo.Type.ITEMSTRING, "25|51|0|0;29|1|0|0");
		assertThat(cargo3.getResourceCount(new ItemID(25)), is(51L));
		assertThat(cargo3.getResourceCount(new ItemID(29)), is(1L));
		assertThat(cargo3.getItemArray().size(), is(2));

		Cargo cargo4 = new Cargo(Cargo.Type.AUTO, "");
		assertThat(cargo4.getResourceCount(new ItemID(25)), is(0L));
		assertThat(cargo4.getResourceCount(new ItemID(29)), is(0L));
		assertThat(cargo4.getItemArray().size(), is(0));
	}

	@Test
	public void testInvalidCargoFromString()
	{
		Cargo cargo = new Cargo(Cargo.Type.AUTO, "25|51||0;29|1|0|");
		assertThat(cargo.getResourceCount(new ItemID(25)), is(51L));
		assertThat(cargo.getResourceCount(new ItemID(29)), is(1L));
		assertThat(cargo.getItemArray().size(), is(2));

		Cargo cargo3 = new Cargo(Cargo.Type.AUTO, "25|51|;29|1|0");
		assertThat(cargo3.getResourceCount(new ItemID(25)), is(51L));
		assertThat(cargo3.getResourceCount(new ItemID(29)), is(1L));
		assertThat(cargo3.getItemArray().size(), is(2));
	}

	@Test
	public void testSave()
	{
		Cargo cargo = new Cargo(Cargo.Type.AUTO, "25|51|0|0;29|1|0|0");
		assertThat(cargo.save(), is("25|51|0|0;29|1|0|0"));
	}

	@Test
	public void testAddResource()
	{
		Cargo cargo = new Cargo();
		assertThat(cargo.save(), is(""));

		cargo.addResource(new ItemID(25), 10);
		assertThat(cargo.save(), is("25|10|0|0"));

		cargo.addResource(new ItemID(35), 1);
		assertThat(cargo.save(), is("25|10|0|0;35|1|0|0"));

		cargo.addResource(new ItemID(25), 3);
		assertThat(cargo.save(), is("25|13|0|0;35|1|0|0"));
	}

	@Test
	public void testSubtractResource()
	{
		Cargo cargo = new Cargo();
		assertThat(cargo.save(), is(""));

		cargo.addResource(new ItemID(25), 10);
		cargo.addResource(new ItemID(35), 1);

		assertThat(cargo.save(), is("25|10|0|0;35|1|0|0"));

		cargo.substractResource(new ItemID(25), 1);
		assertThat(cargo.save(), is("25|9|0|0;35|1|0|0"));

		cargo.substractResource(new ItemID(35), 1);
		assertThat(cargo.save(), is("25|9|0|0"));

		cargo.substractResource(new ItemID(45), 1);
		assertThat(cargo.save(), is("25|9|0|0;45|-1|0|0"));
	}

	@Test
	public void testHasResource()
	{
		Cargo cargo = new Cargo();
		assertThat(cargo.save(), is(""));

		cargo.addResource(new ItemID(25), 10);
		cargo.addResource(new ItemID(35), 1);

		assertTrue(cargo.hasResource(new ItemID(25)));
		assertTrue(cargo.hasResource(new ItemID(35)));
		assertFalse(cargo.hasResource(new ItemID(45)));

		assertTrue(cargo.hasResource(new ItemID(25), 10));
		assertFalse(cargo.hasResource(new ItemID(25), 11));
	}
}
