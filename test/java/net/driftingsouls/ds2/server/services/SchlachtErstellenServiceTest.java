package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class SchlachtErstellenServiceTest extends DBSingleTransactionTest
{
	private List<Ship> ships1 = new ArrayList<>();
	private List<Ship> ships2 = new ArrayList<>();
	private List<Ship> ships3 = new ArrayList<>();
	private User user1;
	private User user2;
	private User user3;
	private ShipType shipType;

	/**
	 * Laedt die Flotten fuer Tests
	 *
	 */
	@Before
	public void loadFleets() {
		user1 = persist(new User("user1", "***", 0, "", new Cargo(), "testUser1@localhost"));
		user1.setFlag(UserFlag.NOOB, false);
		user2 = persist(new User("user2", "***", 0, "", new Cargo(), "testUser2@localhost"));
		user2.setFlag(UserFlag.NOOB, false);
		user3 = persist(new User("user3", "***", 0, "", new Cargo(), "testUser3@localhost"));
		user3.setFlag(UserFlag.NOOB, false);

		shipType = persist(new ShipType());
		ships1.add(persist(new Ship(user1, shipType, 1, 1, 1)));
		ships1.add(persist(new Ship(user1, shipType, 1, 1, 1)));
		ships1.add(persist(new Ship(user1, shipType, 1, 1, 1)));

		ships2.add(persist(new Ship(user2, shipType, 1, 1, 1)));
		ships2.add(persist(new Ship(user2, shipType, 1, 1, 1)));

		ships3.add(persist(new Ship(user3, shipType, 3, 1, 1)));
		ships3.add(persist(new Ship(user3, shipType, 3, 1, 1)));
	}

	@Test
	public void gegebenZweiFlotten_erstelle_sollteEineSchlachtErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(0));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(2));
		assertThat(battle.getLocation(), is(new Location(1,1,1)));
		assertThat(battle.getOwnShip(), not(nullValue()));
		assertThat(battle.getEnemyShip(), not(nullValue()));
		assertThat(battle.getOwnShip().getShip(), is(ships1.get(0)));
		assertThat(battle.getEnemyShip().getShip(), is(ships2.get(0)));
	}

	@Test
	public void gegebenZweiSchiffe_erstelle_sollteEineSchlachtErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		ships1.get(0).setLocation(new Location(2, 2, 2));
		ships2.get(0).setLocation(new Location(2, 2, 2));

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(0));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(1));
		assertThat(battle.getEnemyShips().size(), is(1));
		assertThat(battle.getLocation(), is(new Location(2,2,2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenZweiSchiffeInUnterschiedlichenSektoren_erstelle_sollteEineExceptionWerfen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		ships1.get(0).setLocation(new Location(2, 2, 2));
		ships2.get(0).setLocation(new Location(1, 2, 2));

		// run
		schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenZweiFlottenUndEinenAngegriffenenSpielerImNoobModus_erstelle_sollteEineExceptionWerfen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		user2.setFlag(UserFlag.NOOB, true);

		// run
		schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void gegebenZweiFlottenUndEinenAngreifendenSpielerImNoobModus_erstelle_sollteEineExceptionWerfen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		user1.setFlag(UserFlag.NOOB, true);

		// run
		schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test
	public void gegebenZweiFlottenUndEinUnbeteiligerSpieler_erstelle_sollteEineSchlachtNurAusDenZweiFlottenErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(0));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(2));
		assertThat(battle.getLocation(), is(loc));
	}

	@Test
	public void gegebenZweiFlottenUndWeitereSchiffeAnAnderenPositionen_erstelle_sollteEineSchlachtNurAusDenSchiffenAnDerPositionErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		persist(new Ship(user1, shipType, 1, 2, 2));
		persist(new Ship(user2, shipType, 1, 2, 2));

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(0));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(2));
		assertThat(battle.getLocation(), is(new Location(1,1,1)));
	}

	@Test
	public void gegebenDreiFlottenMitZweienInDerGleichenAllianz_erstelle_sollteEineSchlachtAusDenDreiFlottenErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		Ally ally = persist(new Ally("test", user2));
		ally.addUser(user3);
		user2.setAlly(ally);
		user3.setAlly(ally);

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(ally.getId()));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(4));
		assertThat(battle.getLocation(), is(loc));
	}

	@Test
	public void gegebenDreiFlottenMitZweienInDerGleichenAllianzAberEinemImNoobModus_erstelle_sollteEineSchlachtAusZweiFlottenErstellen() {
		// setup
		SchlachtErstellenService schlachtErstellenService = new SchlachtErstellenService();
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		Ally ally = persist(new Ally("test", user2));
		ally.addUser(user3);
		user2.setAlly(ally);
		user3.setAlly(ally);

		user3.setFlag(UserFlag.NOOB);

		// run
		Battle battle = schlachtErstellenService.erstelle(user1, ships1.get(0), ships2.get(0), true);

		// assert
		assertThat(battle, not(nullValue()));
		assertThat(battle.getAlly(0), is(0));
		assertThat(battle.getAlly(1), is(ally.getId()));
		assertThat(battle.getCommander(0), is(user1));
		assertThat(battle.getCommander(1), is(user2));
		assertThat(battle.getOwnShips().size(), is(3));
		assertThat(battle.getEnemyShips().size(), is(2));
		assertThat(battle.getLocation(), is(loc));
	}
}