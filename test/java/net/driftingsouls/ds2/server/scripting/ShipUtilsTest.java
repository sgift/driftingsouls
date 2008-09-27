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
package net.driftingsouls.ds2.server.scripting;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.ships.Ship;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Testcase fuer ShipUtils
 * @author Christopher Jung
 *
 */
public class ShipUtilsTest extends DriftingSoulsDBTestCase {
	private Ship ship;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();

		this.ship = (Ship)this.context.getDB().get(Ship.class, 1);
	}

	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(ShipUtilsTest.class.getResourceAsStream("ShipUtilsTest.xml"));
	}

	/**
	 * Testet den einfachen Flug zu einem nahen Ziel
	 * @throws Exception 
	 */
	@Test
	public void simpleMovement() throws Exception {
		boolean result = ShipUtils.move(this.ship, new Location(1, 5, 1), Integer.MAX_VALUE);
		
		assertTrue("Schiff zu langsam", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 5, 1)));
		assertThat(this.ship.getEnergy(), is(146));
	}
	
	/**
	 * Testet den einfachen Flug zu einem fernen Ziel
	 * @throws Exception 
	 */
	@Test
	public void longMovement() throws Exception {
		boolean result = ShipUtils.move(this.ship, new Location(1, 150, 1), Integer.MAX_VALUE);
		
		assertFalse("Schiff zu schnell", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 101, 1)));
		assertThat(this.ship.getEnergy(), is(50));
		
		this.ship.setHeat(0);
		this.ship.setEnergy(150);
		
		result = ShipUtils.move(this.ship, new Location(1, 150, 1), Integer.MAX_VALUE);
		assertTrue("Schiff zu langsam", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 150, 1)));
		assertThat(this.ship.getEnergy(), is(101));
	}
	
	/**
	 * Testet den Flug mit einer max-Anzahl an Feldern
	 * @throws Exception 
	 */
	@Test
	public void limitedMovement() throws Exception {
		boolean result = ShipUtils.move(this.ship, new Location(1, 150, 1), 50);
		
		assertFalse("Schiff zu schnell", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 51, 1)));
		assertThat(this.ship.getEnergy(), is(100));
	}
	
	/**
	 * Testet den einfachen Flug durch einen Emp-Nebel zu einem nahen Ziel
	 * @throws Exception 
	 */
	@Test
	public void empMovement() throws Exception {
		boolean result = ShipUtils.move(this.ship, new Location(1, 1, 10), Integer.MAX_VALUE);
		
		assertTrue("Schiff zu langsam", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 1, 10)));
		assertThat(this.ship.getEnergy(), is(141));
	}
	
	/**
	 * Testet den Flug durch einen Emp-Nebel mit einer max-Anzahl an Feldern
	 * @throws Exception 
	 */
	@Test @Ignore("maxcount fixen")
	// TODO maxcount wird beim Flug durch EMP-Nebel falsch interpretiert
	public void empLimitedMovement() throws Exception {
		boolean result = ShipUtils.move(this.ship, new Location(1, 1, 150), 50);
		
		assertFalse("Schiff zu schnell", result);
		assertThat(this.ship.getLocation(), is(new Location(1, 1, 51)));
		assertThat(this.ship.getEnergy(), is(100));
	}
}
