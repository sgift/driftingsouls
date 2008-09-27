/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.ships;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.entities.User;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Testet die Flotten
 * @author Christopher Jung
 *
 */
public class ShipFleetTest extends DriftingSoulsDBTestCase {
	private ShipFleet fleet1;
	private ShipFleet fleet2;
	
	/**
	 * Laedt die Flotten fuer Tests
	 *
	 */
	@Before
	public void loadFleets() {
		org.hibernate.Session db = context.getDB();
		
		this.fleet1 = (ShipFleet)db.get(ShipFleet.class, 1);
		assertThat(this.fleet1, not(nullValue()));
		
		this.fleet2 = (ShipFleet)db.get(ShipFleet.class, 2);
		assertThat(this.fleet2, not(nullValue()));
	}
	
	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(ShipFleetTest.class.getResourceAsStream("ShipFleetTest.xml"));
	}
	
	/**
	 * Testet die Flottenuebergabe bei der ersten Flotte
	 */
	@Test
	public void consignFleet1() {
		org.hibernate.Session db = context.getDB();
		
		User currentOwner = (User)db.get(User.class, 1);
		User targetOwner = (User)db.get(User.class, 2);
		
		assertThat(this.fleet1.getOwner(), is(currentOwner));
		boolean ok = this.fleet1.consign(targetOwner);
		assertThat(ok, is(true));
		
		db.flush();
		
		assertThat(this.fleet1.getOwner(), is(targetOwner));
		
		for( int i=1; i <= 3; i++ ) {
			Ship ship = (Ship)db.get(Ship.class, i);
			assertThat(ship.getOwner(), is(targetOwner));
			assertThat(ship.getFleet(), is(this.fleet1));
		}
	}
	
	/**
	 * Testet die Flottenuebergabe bei der zweiten Flotte
	 */
	@Test
	public void consignFleet2() {
		org.hibernate.Session db = context.getDB();
		
		User currentOwner = (User)db.get(User.class, 1);
		User targetOwner = (User)db.get(User.class, 2);
		
		assertThat(this.fleet2.getOwner(), is(currentOwner));
		boolean ok = this.fleet2.consign(targetOwner);
		assertThat(ok, is(true));
		
		db.flush();
		
		assertThat(this.fleet2.getOwner(), is(targetOwner));
		
		for( int i=4; i <= 5; i++ ) {
			Ship ship = (Ship)db.get(Ship.class, i);
			assertThat(ship.getOwner(), is(targetOwner));
			assertThat(ship.getFleet(), is(this.fleet2));
		}
	}
	
	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte 
	 */
	@Test
	public void removeShipCleanSession() {
		org.hibernate.Session db = context.getDB();
		
		// Flotten aus dem Cache entfernen
		db.clear();
		
		Ship ship = (Ship)db.get(Ship.class, 1);
		ship.getFleet().removeShip(ship);
		
		db.flush();
		
		assertThat(ship.getFleet(), nullValue());
		
		ShipFleet fleet = (ShipFleet)db.get(ShipFleet.class, 1);
		assertThat(fleet, not(nullValue()));
		
		ship = (Ship)db.get(Ship.class, 2);
		assertThat(ship.getFleet(), equalTo(fleet));
	}
	
	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte 
	 */
	@Test
	public void removeShip() {
		org.hibernate.Session db = context.getDB();
		
		Ship ship = (Ship)db.get(Ship.class, 1);
		ship.getFleet().removeShip(ship);
		
		db.flush();
		
		assertThat(ship.getFleet(), nullValue());
		
		ShipFleet fleet = (ShipFleet)db.get(ShipFleet.class, 1);
		assertThat(fleet, not(nullValue()));
		
		ship = (Ship)db.get(Ship.class, 2);
		assertThat(ship.getFleet(), equalTo(fleet));
	}
	
	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte 
	 * dessen die Flotte anschliessend zu wenig Schiffe hat
	 */
	@Test
	public void removeShipAndDissolveFleet() {
		org.hibernate.Session db = context.getDB();
		
		Ship ship = (Ship)db.get(Ship.class, 4);
		ship.getFleet().removeShip(ship);
		
		db.flush();
		
		assertThat(ship.getFleet(), nullValue());
		
		ShipFleet fleet = (ShipFleet)db.get(ShipFleet.class, 2);
		assertThat(fleet, nullValue());
		
		ship = (Ship)db.get(Ship.class, 5);
		assertThat(ship.getFleet(), nullValue());
	}
}
