package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.*;

import static net.driftingsouls.ds2.server.ViewMessageAssert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
public class NpcControllerTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private NpcController controller;
	@Autowired
	private JavaSession javaSession;
	@Autowired
	private UserService userService;

	private User user;

	@Before
	@Transactional
	public void createController() {
		Rasse rasse = new Rasse("SuperUsers", true);
		em.persist(rasse);
		User user = new User(1, "Test", "Test", "1234", rasse.getId(), "", "test@localhost", configService);
		em.persist(user);
		user.setFlag(UserFlag.ORDER_MENU);
		rasse.setHead(user);
		this.user = user;
		javaSession.setUser(user);
	}

	@Test
	@Transactional
	public void gegebenEinZuBestellenderOffizier_orderAction_sollteEineEntsprechendeBestellungErzeugen()
	{
		// setup
		OrderableOffizier offi = new OrderableOffizier("Testoffi", 1, 1, 42, 42, 42, 42, 42);
		em.persist(offi);

		user.setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(offi, 1);

		// assert
		assertSuccess(message);
		assertEquals(9, user.getNpcPunkte());

		OrderOffizier order = em.createQuery("from OrderOffizier where user=:user", OrderOffizier.class)
				.setParameter("user", user)
				.getSingleResult();

		assertNotNull(order);
		assertEquals(offi.getId(), order.getType());
	}

	@Test
	@Transactional
	public void gegebenEinZuUnbekannterZuBestellenderOffizier_orderAction_sollteEineFehlermeldungZurueckgeben()
	{
		// setup
		user.setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(null, 1);

		// assert
		assertFailure(message);
	}

	@Test
	@Transactional
	public void gegebenEinZuBestellenderOffizierUndZuWenigeNpcPunkte_orderAction_sollteEineFehlermeldungZurueckgebenUndKeineBestellungEintragen()
	{
		// setup
		OrderableOffizier offi = new OrderableOffizier("Testoffi", 20, 1, 42, 42, 42, 42, 42);
		em.persist(offi);

		user.setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(offi, 1);

		// assert
		assertFailure(message);
		assertEquals(10, user.getNpcPunkte());

		Optional<OrderOffizier> order = em.createQuery("from OrderOffizier where user=:user", OrderOffizier.class)
				.setParameter("user", user)
				.getResultStream().findFirst();

		assertTrue(order.isEmpty());
	}

	@Test
	@Transactional
	public void gegebenEinZuBestellendesSchiff_orderShipsAction_sollteEineEntsprechendeBestellungErzeugen()
	{
		// setup
		ShipType type = new ShipType();
		em.persist(type);
		Rasse rasse = new Rasse("GCP", true);
		em.persist(rasse);
		OrderableShip ship = new OrderableShip(type, rasse, 5);
		em.persist(ship);

		user.setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship, 1);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertSuccess(message);
		assertEquals(5, user.getNpcPunkte());

		OrderShip order = em.createQuery("from OrderShip where user=:user", OrderShip.class)
				.setParameter("user",user)
				.getSingleResult();

		assertNotNull(order);
		assertEquals(ship.getShipType(), order.getShipType());
		assertEquals("", order.getFlags());
	}

	@Test
	@Transactional
	public void gegebenMehrereZuBestellendeSchiffe_orderShipsAction_sollteEntsprechendeBestellungenErzeugen()
	{
		// setup
		ShipType type1 = new ShipType();
		em.persist(type1);
		ShipType type2 = new ShipType();
		em.persist(type2);
		Rasse rasse = new Rasse("GCP", true);
		em.persist(rasse);
		OrderableShip ship1 = new OrderableShip(type1, rasse, 5);
		em.persist(ship1);
		OrderableShip ship2 = new OrderableShip(type2, rasse, 2);
		em.persist(ship2);

		user.setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship1, 1);
		shipMap.put(ship2, 2);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertSuccess(message);
		assertEquals(1, user.getNpcPunkte());

		List<OrderShip> order = em.createQuery("from OrderShip where user=:user", OrderShip.class)
				.setParameter("user", user)
				.getResultList();

		assertEquals(3, order.size());
		assertEquals(1, order.stream().filter(o -> o.getShipType() == type1).count());
		assertEquals(2, order.stream().filter(o -> o.getShipType() == type2).count());
	}

	@Test
	@Transactional
	public void gegebenKeinZuBestellendesSchiff_orderShipsAction_sollteEineFehlermeldungZurueckgeben()
	{
		// setup
		user.setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertFailure(message);

		Optional<OrderShip> order = em.createQuery("from OrderShip where user=:user", OrderShip.class)
				.setParameter("user", user)
				.getResultStream().findFirst();

		assertTrue(order.isEmpty());
	}

	@Test
	@Transactional
	public void gegebenKeinePosition_changeOrderLocationAction_sollteDieOrderPositionZuruecksetzen()
	{
		// setup
		user.setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction("");

		// assert
		assertSuccess(message);

		assertNull(user.getNpcOrderLocation());
	}

	@Test
	@Transactional
	public void gegebenEinePositionMitEinerEigenenBasis_changeOrderLocationAction_sollteDieOrderPositionAendern()
	{
		// setup
		Location loc = new Location(2, 3, 4);
		BaseType type = new BaseType("Testklasse");
		em.persist(type);
		var base = new Base(loc, user, type);
		user.getBases().add(base);
		user.setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction(loc.asString());

		// assert
		assertSuccess(message);

		assertEquals(loc.asString(), user.getNpcOrderLocation());
	}

	@Test
	@Transactional
	public void gegebenEinePositionOhneEigeneBasis_changeOrderLocationAction_sollteEinenFehlerZurueckgeben()
	{
		// setup
		Location loc = new Location(2, 3, 4);
		user.setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction(loc.asString());

		// assert
		assertFailure(message);
		assertEquals("1:2/3", user.getNpcOrderLocation());
	}

	@Test
	@Transactional
	public void gegebenEinOrden_awardMedalAction_sollteDenOrdenDemBenutzerHinzufuegenUndEinePmVersenden()
	{
		// setup
		User zielUser = new User(4, "Test4", "Test4", "1234", 0, "", "test@localhost", configService);
		em.persist(zielUser);
		Medal medal = new Medal("Testorden", "", "");
		em.persist(medal);
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertSuccess(message);
		assertTrue(userService.getMedals(zielUser).contains(medal));
		assertTrue(zielUser.getHistory().contains(reason));
		assertTrue(zielUser.getHistory().contains("[medal]"+medal.getId()+"[/medal]"));

		PM pm = em.createQuery("from PM where sender=:user and empfaenger=:zielUser", PM.class)
				.setParameter("user", user)
				.setParameter("zielUser", zielUser)
				.getSingleResult();
		assertNotNull(pm);
		assertTrue(pm.getInhalt().contains(reason));
		assertTrue(pm.getInhalt().contains("[medal]"+medal.getId()+"[/medal]"));
	}

	@Test
	@Transactional
	public void gegebenEinNurFuerAdminsVerwendbarerOrden_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		User zielUser = new User(3, "Test3", "Test3", "1234", 0, "", "test@localhost", configService);
		em.persist(zielUser);
		Medal medal = new Medal("Testorden", "", "");
		em.persist(medal);
		medal.setAdminOnly(true);
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertFailure(message);
		assertFalse(userService.getMedals(zielUser).contains(medal));
	}

	@Test
	@Transactional
	public void gegebenEinNichtBekannterBenutzer_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		Medal medal = new Medal("Testorden", "", "");
		em.persist(medal);
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction("1234", medal, reason);

		// assert
		assertFailure(message);
	}

	@Test
	@Transactional
	public void gegebenEinNpcDerNichtKopfSeinerRasseIst_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		Rasse rasse = em.find(Rasse.class, user.getRace());
		rasse.setHead(null);
		User zielUser = new User(2, "Test2", "Test2", "1234", 0, "", "test@localhost", configService);
		em.persist(zielUser);
		Medal medal = new Medal("Testorden", "", "");
		em.persist(medal);
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertFailure(message);
	}
}