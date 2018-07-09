package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ShipFleetTest extends DBSingleTransactionTest
{
	private ShipFleet fleet1;
	private ShipFleet fleet2;
	private User user1;
	private User user2;

	/**
	 * Laedt die Flotten fuer Tests
	 *
	 */
	@Before
	public void loadFleets() {
		user1 = persist(new User("user1", "***", 0, "", new Cargo(), "testUser@localhost"));
		user2 = persist(new User("user2", "***", 0, "", new Cargo(), "testUser@localhost"));

		ShipType shipType = persist(new ShipType());
		Ship ship1 = persist(new Ship(user1, shipType, 1, 1, 1));
		Ship ship2 = persist(new Ship(user1, shipType, 1, 1, 1));
		Ship ship3 = persist(new Ship(user1, shipType, 1, 1, 1));

		Ship ship4 = persist(new Ship(user1, shipType, 1, 1, 1));
		Ship ship5 = persist(new Ship(user1, shipType, 1, 1, 1));

		this.fleet1 = persist(new ShipFleet());
		ship1.setFleet(this.fleet1);
		ship2.setFleet(this.fleet1);
		ship3.setFleet(this.fleet1);

		this.fleet2 = persist(new ShipFleet());
		ship4.setFleet(this.fleet2);
		ship5.setFleet(this.fleet2);
	}

	/**
	 * Testet die Flottenuebergabe bei der ersten Flotte
	 */
	@Test
	public void gegebenEineFlotteMitDreiSchiffen_consign_sollteAlleSchiffeAnDenNeuenBesitzerUebergeben() {
		// setup
		User currentOwner = this.user1;
		User targetOwner = this.user2;

		assertThat(this.fleet1.getOwner(), is(currentOwner));

		// run
		boolean ok = this.fleet1.consign(targetOwner);

		// assert
		assertThat(ok, is(true));
		assertThat(this.fleet1.getOwner(), is(targetOwner));

		List<Ship> ships = this.fleet1.getShips();
		assertThat(ships.size(), is(3));
		for (Ship ship : ships)
		{
			assertThat(ship.getOwner(), is(targetOwner));
			assertThat(ship.getFleet(), is(this.fleet1));
		}
	}

	/**
	 * Testet die Flottenuebergabe bei der zweiten Flotte
	 */
	@Test
	public void gegebenEineFlotteMitZweiSchiffen_consignFleet_sollteAlleSchiffeAnDenNeuenBesitzerUebergeben() {
		// setup
		User currentOwner = this.user1;
		User targetOwner = this.user2;

		assertThat(this.fleet2.getOwner(), is(currentOwner));

		// run
		boolean ok = this.fleet2.consign(targetOwner);

		// assert
		assertThat(ok, is(true));
		assertThat(this.fleet2.getOwner(), is(targetOwner));

		List<Ship> ships = this.fleet2.getShips();
		assertThat(ships.size(), is(2));
		for (Ship ship : ships)
		{
			assertThat(ship.getOwner(), is(targetOwner));
			assertThat(ship.getFleet(), is(this.fleet2));
		}
	}

	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte
	 */
	@Test
	public void gegebenEineFlotteMitDreiSchiffen_removeShip_sollteEinSchiffEntfernenUndDieFlotteBestehenLassen() {
		// setup
		Ship ship = this.fleet1.getShips().get(0);

		// run
		ship.getFleet().removeShip(ship);

		// assert
		assertThat(ship.getFleet(), nullValue());

		ShipFleet fleet = this.fleet1;
		assertThat(getEM().contains(fleet), is(true));

		List<Ship> ships = this.fleet1.getShips();
		assertThat(ships.size(), is(2));
		assertThat(ships.contains(ship), is(false));
	}

	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte
	 * dessen die Flotte anschliessend zu wenig Schiffe hat
	 */
	@Test
	public void gegebenEineFlotteMitZweiSchiffen_removeShip_sollteAlleSchiffeEntfernenUndDieFlotteAufloesen() {
		// setup
		Ship ship = this.fleet2.getShips().get(0);
		Ship ship2 = this.fleet2.getShips().get(1);

		// run
		ship.getFleet().removeShip(ship);

		// assert
		assertThat(ship.getFleet(), nullValue());
		assertThat(getEM().contains(this.fleet2), is(false));
		assertThat(ship2.getFleet(), nullValue());
	}
}
