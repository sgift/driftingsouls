package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.ConsignService;
import net.driftingsouls.ds2.server.services.FleetMgmtService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
public class ShipFleetTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private FleetMgmtService fleetMgmtService;
	@Autowired
	private ConsignService consignService;

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
		user1 = new User(1, "user1", "user1", "***", 0, "", "testUser@localhost", configService);
		em.persist(user1);
		user2 = new User(2, "user2", "user2", "***", 0, "", "testUser@localhost", configService);
		em.persist(user2);

		ShipType shipType = new ShipType();
		em.persist(shipType);
		Ship ship1 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship1);
		Ship ship2 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship2);
		Ship ship3 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship3);

		Ship ship4 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship4);
		Ship ship5 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship5);

		this.fleet1 = new ShipFleet();
		em.persist(fleet1);
		ship1.setFleet(this.fleet1);
		ship2.setFleet(this.fleet1);
		ship3.setFleet(this.fleet1);

		this.fleet2 = new ShipFleet();
		em.persist(fleet2);
		ship4.setFleet(this.fleet2);
		ship5.setFleet(this.fleet2);
	}

	/**
	 * Testet die Flottenuebergabe bei der ersten Flotte
	 */
	@Test
	@Transactional
	public void gegebenEineFlotteMitDreiSchiffen_consign_sollteAlleSchiffeAnDenNeuenBesitzerUebergeben() {
		// setup
		User currentOwner = this.user1;
		User targetOwner = this.user2;

		assertThat(fleetMgmtService.getOwner(this.fleet1), is(currentOwner));

		// run
		boolean ok = consignService.consign(this.fleet1, targetOwner);

		// assert
		assertThat(ok, is(true));
		assertThat(fleetMgmtService.getOwner(this.fleet1), is(targetOwner));

		List<Ship> ships = fleetMgmtService.getShips(this.fleet1);
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
	@Transactional
	public void gegebenEineFlotteMitZweiSchiffen_consignFleet_sollteAlleSchiffeAnDenNeuenBesitzerUebergeben() {
		// setup
		User currentOwner = this.user1;
		User targetOwner = this.user2;

		assertThat(fleetMgmtService.getOwner(this.fleet2), is(currentOwner));

		// run
		boolean ok = consignService.consign(this.fleet2, targetOwner);

		// assert
		assertThat(ok, is(true));
		assertThat(fleetMgmtService.getOwner(this.fleet2), is(targetOwner));

		List<Ship> ships = fleetMgmtService.getShips(this.fleet2);
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
	@Transactional
	public void gegebenEineFlotteMitDreiSchiffen_removeShip_sollteEinSchiffEntfernenUndDieFlotteBestehenLassen() {
		// setup
		Ship ship = fleetMgmtService.getShips(this.fleet1).get(0);

		// run
		fleetMgmtService.removeShip(ship.getFleet(), ship);

		// assert
		assertThat(ship.getFleet(), nullValue());

		ShipFleet fleet = this.fleet1;
		assertThat(em.contains(fleet), is(true));

		List<Ship> ships = fleetMgmtService.getShips(this.fleet1);
		assertThat(ships.size(), is(2));
		assertThat(ships.contains(ship), is(false));
	}

	/**
	 * Testet das Entfernen eines Schiffes aus einer Flotte
	 * dessen die Flotte anschliessend zu wenig Schiffe hat
	 */
	@Test
	@Transactional
	public void gegebenEineFlotteMitZweiSchiffen_removeShip_sollteAlleSchiffeEntfernenUndDieFlotteAufloesen() {
		// setup
		Ship ship = fleetMgmtService.getShips(this.fleet2).get(0);
		Ship ship2 = fleetMgmtService.getShips(this.fleet2).get(1);

		// run
		fleetMgmtService.removeShip(ship.getFleet(), ship);

		// assert
		assertThat(ship.getFleet(), nullValue());
		assertThat(em.contains(this.fleet2), is(false));
		assertThat(ship2.getFleet(), nullValue());
	}
}
