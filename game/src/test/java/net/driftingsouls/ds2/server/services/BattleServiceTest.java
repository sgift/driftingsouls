package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
@WebAppConfiguration
public class BattleServiceTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private BattleService battleService;
	@Autowired
	private UserValueService userValueService;
	@Autowired
	private BBCodeParser bbCodeParser;

	private final List<Ship> ships1 = new ArrayList<>();
	private final List<Ship> ships2 = new ArrayList<>();
	private final List<Ship> ships3 = new ArrayList<>();
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
		user1 = new User(1, "user1", "user1", "***", 0, "", "testUser1@localhost", configService);
		em.persist(user1);
		user1.setFlag(UserFlag.NOOB, false);
		userValueService.setUserValue(user1, WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM, false);
		user2 = new User(2, "user2", "user2", "***", 0, "", "testUser2@localhost", configService);
		em.persist(user2);
		user2.setFlag(UserFlag.NOOB, false);
		userValueService.setUserValue(user2, WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM, false);
		user3 = new User(3, "user3", "user3", "***", 0, "", "testUser3@localhost", configService);
		em.persist(user3);
		user3.setFlag(UserFlag.NOOB, false);
		userValueService.setUserValue(user3, WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM, false);

		shipType =new ShipType();
		em.persist(shipType);

		var ship1 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship1);
		ships1.add(ship1);

		var ship2 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship2);
		ships1.add(ship2);

		var ship3 = new Ship(user1, shipType, 1, 1, 1);
		em.persist(ship3);
		ships1.add(ship3);

		var ship4 = new Ship(user2, shipType, 1, 1, 1);
		em.persist(ship4);
		ships2.add(ship4);

		var ship5 = new Ship(user2, shipType, 1, 1, 1);
		em.persist(ship5);
		ships2.add(ship5);

		var ship6 = new Ship(user3, shipType, 3, 1, 1);
		em.persist(ship6);
		ships3.add(ship6);

		var ship7 = new Ship(user3, shipType, 3, 1, 1);
		em.persist(ship7);
		ships3.add(ship7);
	}

	@Test
	@Transactional
	public void gegebenZweiFlotten_erstelle_sollteEineSchlachtErstellen() {

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
	@Transactional
	public void gegebenZweiSchiffe_erstelle_sollteEineSchlachtErstellen() {
		ships1.get(0).setLocation(new Location(2, 2, 2));
		ships2.get(0).setLocation(new Location(2, 2, 2));

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
	@Transactional
	public void gegebenZweiSchiffeInUnterschiedlichenSektoren_erstelle_sollteEineExceptionWerfen() {
		ships1.get(0).setLocation(new Location(2, 2, 2));
		ships2.get(0).setLocation(new Location(1, 2, 2));

		// run
		battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	@Transactional
	public void gegebenZweiFlottenUndEinenAngegriffenenSpielerImNoobModus_erstelle_sollteEineExceptionWerfen() {
		user2.setFlag(UserFlag.NOOB, true);

		// run
		battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test(expected = IllegalArgumentException.class)
	@Transactional
	public void gegebenZweiFlottenUndEinenAngreifendenSpielerImNoobModus_erstelle_sollteEineExceptionWerfen() {
		user1.setFlag(UserFlag.NOOB, true);

		// run
		battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);
	}

	@Test
	@Transactional
	public void gegebenZweiFlottenUndEinUnbeteiligerSpieler_erstelle_sollteEineSchlachtNurAusDenZweiFlottenErstellen() {
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
	@Transactional
	public void gegebenZweiFlottenUndWeitereSchiffeAnAnderenPositionen_erstelle_sollteEineSchlachtNurAusDenSchiffenAnDerPositionErstellen() {
		var ship1 = new Ship(user1, shipType, 1, 2, 2);
		em.persist(ship1);
		var ship2 = new Ship(user2, shipType, 1, 2, 2);
		em.persist(ship2);

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
	@Transactional
	public void gegebenDreiFlottenMitZweienInDerGleichenAllianz_erstelle_sollteEineSchlachtAusDenDreiFlottenErstellen() {
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		var plainname = Common._titleNoFormat(bbCodeParser, "test");
		Ally ally = new Ally("test", plainname, user2, 1);
		em.persist(ally);
		ally.addUser(user3);
		user2.setAlly(ally);
		user3.setAlly(ally);

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
	@Transactional
	public void gegebenDreiFlottenMitZweienInDerGleichenAllianzAberEinemImNoobModus_erstelle_sollteEineSchlachtAusZweiFlottenErstellen() {
		Location loc = ships3.get(0).getLocation();
		ships1.forEach(s -> s.setLocation(loc));
		ships2.forEach(s -> s.setLocation(loc));

		var plainname = Common._titleNoFormat(bbCodeParser, "test");
		Ally ally = new Ally("test", plainname, user2, 1);
		em.persist(ally);
		ally.addUser(user3);
		user2.setAlly(ally);
		user3.setAlly(ally);

		user3.setFlag(UserFlag.NOOB);

		// run
		Battle battle = battleService.erstelle(user1, ships1.get(0), ships2.get(0), true);

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
