package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.services.FraktionsGuiEintragService;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import static net.driftingsouls.ds2.server.ViewMessageAssert.*;

public class NpcControllerTest extends DBSingleTransactionTest
{
	private NpcController controller;

	@Before
	public void createController() {
		Rasse rasse = persist(new Rasse("SuperUsers", true));
		User user = persist(new User("Test", "1234", rasse.getId(), "", new Cargo(), "test@localhost"));
		user.setFlag(UserFlag.ORDER_MENU);
		rasse.setHead(user);

		getContext().setActiveUser(user);
		controller = new NpcController(new FraktionsGuiEintragService());
	}

	@Test
	public void gegebenEinZuBestellenderOffizier_orderAction_sollteEineEntsprechendeBestellungErzeugen()
	{
		// setup
		OrderableOffizier offi = persist(new OrderableOffizier("Testoffi", 1, 1, 42, 42, 42, 42, 42));

		getUser().setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(offi, 1);

		// assert
		assertSuccess(message);
		assertEquals(9, getUser().getNpcPunkte());

		OrderOffizier order = (OrderOffizier) getDB().createQuery("from OrderOffizier where user=:user")
				.setParameter("user", getUser())
				.uniqueResult();

		assertNotNull(order);
		assertEquals(offi.getId(), order.getType());
	}

	@Test
	public void gegebenEinZuUnbekannterZuBestellenderOffizier_orderAction_sollteEineFehlermeldungZurueckgeben()
	{
		// setup
		getUser().setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(null, 1);

		// assert
		assertFailure(message);
	}

	@Test
	public void gegebenEinZuBestellenderOffizierUndZuWenigeNpcPunkte_orderAction_sollteEineFehlermeldungZurueckgebenUndKeineBestellungEintragen()
	{
		// setup
		OrderableOffizier offi = persist(new OrderableOffizier("Testoffi", 20, 1, 42, 42, 42, 42, 42));

		getUser().setNpcPunkte(10);

		// run
		ViewMessage message = controller.orderAction(offi, 1);

		// assert
		assertFailure(message);
		assertEquals(10, getUser().getNpcPunkte());

		OrderOffizier order = (OrderOffizier) getDB().createQuery("from OrderOffizier where user=:user")
				.setParameter("user", getUser())
				.uniqueResult();

		assertNull(order);
	}

	@Test
	public void gegebenEinZuBestellendesSchiff_orderShipsAction_sollteEineEntsprechendeBestellungErzeugen()
	{
		// setup
		ShipType type = persist(new ShipType());
		Rasse rasse = persist(new Rasse("GCP", true));
		OrderableShip ship = persist(new OrderableShip(type, rasse, 5));

		getUser().setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship, 1);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertSuccess(message);
		assertEquals(5, getUser().getNpcPunkte());

		OrderShip order = (OrderShip) getDB().createQuery("from OrderShip where user=:user")
				.setParameter("user", getUser())
				.uniqueResult();

		assertNotNull(order);
		assertEquals(ship.getShipType(), order.getShipType());
		assertEquals("", order.getFlags());
	}

	@Test
	public void gegebenMehrereZuBestellendeSchiffe_orderShipsAction_sollteEntsprechendeBestellungenErzeugen()
	{
		// setup
		ShipType type1 = persist(new ShipType());
		ShipType type2 = persist(new ShipType());
		Rasse rasse = persist(new Rasse("GCP", true));
		OrderableShip ship1 = persist(new OrderableShip(type1, rasse, 5));
		OrderableShip ship2 = persist(new OrderableShip(type2, rasse, 2));

		getUser().setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship1, 1);
		shipMap.put(ship2, 2);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertSuccess(message);
		assertEquals(1, getUser().getNpcPunkte());

		List<OrderShip> order = Common.cast(getDB().createQuery("from OrderShip where user=:user")
				.setParameter("user", getUser())
				.list());

		assertEquals(3, order.size());
		assertEquals(1, order.stream().filter(o -> o.getShipType() == type1).count());
		assertEquals(2, order.stream().filter(o -> o.getShipType() == type2).count());
	}

	@Test
	public void gegebenKeinZuBestellendesSchiff_orderShipsAction_sollteEineFehlermeldungZurueckgeben()
	{
		// setup
		getUser().setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertFailure(message);

		OrderShip order = (OrderShip) getDB().createQuery("from OrderShip where user=:user")
				.setParameter("user", getUser())
				.uniqueResult();

		assertNull(order);
	}

	@Test
	public void gegebenKeinePosition_changeOrderLocationAction_sollteDieOrderPositionZuruecksetzen()
	{
		// setup
		getUser().setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction("");

		// assert
		assertSuccess(message);

		assertNull(getUser().getNpcOrderLocation());
	}

	@Test
	public void gegebenEinePositionMitEinerEigenenBasis_changeOrderLocationAction_sollteDieOrderPositionAendern()
	{
		// setup
		Location loc = new Location(2, 3, 4);
		BaseType type = persist(new BaseType("Testklasse"));
		getUser().getBases().add(persist(new Base(loc, getUser(), type)));
		getUser().setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction(loc.asString());

		// assert
		assertSuccess(message);

		assertEquals(loc.asString(), getUser().getNpcOrderLocation());
	}

	@Test
	public void gegebenEinePositionOhneEigeneBasis_changeOrderLocationAction_sollteEinenFehlerZurueckgeben()
	{
		// setup
		Location loc = new Location(2, 3, 4);
		getUser().setNpcOrderLocation("1:2/3");

		// run
		ViewMessage message = controller.changeOrderLocationAction(loc.asString());

		// assert
		assertFailure(message);
		assertEquals("1:2/3", getUser().getNpcOrderLocation());
	}

	@Test
	public void gegebenEinOrden_awardMedalAction_sollteDenOrdenDemBenutzerHinzufuegenUndEinePmVersenden()
	{
		// setup
		User zielUser = persist(new User("Test2", "1234", 0, "", new Cargo(), "test@localhost"));
		Medal medal = persist(new Medal("Testorden", "", ""));
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertSuccess(message);
		assertTrue(zielUser.getMedals().contains(medal));
		assertTrue(zielUser.getHistory().contains(reason));
		assertTrue(zielUser.getHistory().contains("[medal]"+medal.getId()+"[/medal]"));

		PM pm = (PM)getDB().createQuery("from PM where sender=:user and empfaenger=:zielUser")
				.setParameter("user", getUser())
				.setParameter("zielUser", zielUser)
				.uniqueResult();
		assertNotNull(pm);
		assertTrue(pm.getInhalt().contains(reason));
		assertTrue(pm.getInhalt().contains("[medal]"+medal.getId()+"[/medal]"));
	}

	@Test
	public void gegebenEinNurFuerAdminsVerwendbarerOrden_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		User zielUser = persist(new User("Test2", "1234", 0, "", new Cargo(), "test@localhost"));
		Medal medal = persist(new Medal("Testorden", "", ""));
		medal.setAdminOnly(true);
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertFailure(message);
		assertFalse(zielUser.getMedals().contains(medal));
	}

	@Test
	public void gegebenEinNichtBekannterBenutzer_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		Medal medal = persist(new Medal("Testorden", "", ""));
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction("1234", medal, reason);

		// assert
		assertFailure(message);
	}

	@Test
	public void gegebenEinNpcDerNichtKopfSeinerRasseIst_awardMedalAction_sollteEinenFehlerMelden()
	{
		// setup
		Rasse rasse = (Rasse)getDB().get(Rasse.class,getUser().getRace());
		rasse.setHead(null);
		User zielUser = persist(new User("Test2", "1234", 0, "", new Cargo(), "test@localhost"));
		Medal medal = persist(new Medal("Testorden", "", ""));
		String reason = "Ein Grund 14242332343222434546";

		// run
		controller.validateAndPrepare();
		ViewMessage message = controller.awardMedalAction(Integer.toString(zielUser.getId()), medal, reason);

		// assert
		assertFailure(message);
	}
}