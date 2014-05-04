package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Rasse;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.npcorders.OrderOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderShip;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableOffizier;
import net.driftingsouls.ds2.server.entities.npcorders.OrderableShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ViewMessage;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NpcControllerTest extends DBSingleTransactionTest
{
	private NpcController controller;

	@Before
	public void createController() {
		User user = new User("Test", "1234", 0, "", new Cargo(), "test@localhost");
		user.setFlag(UserFlag.ORDER_MENU);

		getContext().setActiveUser(user);
		controller = new NpcController(getContext());
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
		assertEquals("success", message.message.type);
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
		assertEquals("failure", message.message.type);
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
		assertEquals("failure", message.message.type);
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
		Rasse rasse = persist(new Rasse(0, "GCP", true));
		OrderableShip ship = persist(new OrderableShip(type, rasse, 5));

		getUser().setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship, 1);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertEquals("success", message.message.type);
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
		Rasse rasse = persist(new Rasse(0, "GCP", true));
		OrderableShip ship1 = persist(new OrderableShip(type1, rasse, 5));
		OrderableShip ship2 = persist(new OrderableShip(type2, rasse, 2));

		getUser().setNpcPunkte(10);

		Map<OrderableShip,Integer> shipMap = new HashMap<>();
		shipMap.put(ship1, 1);
		shipMap.put(ship2, 2);

		// run
		ViewMessage message = controller.orderShipsAction(false, false, false, shipMap);

		// assert
		assertEquals("success", message.message.type);
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
		assertEquals("failure", message.message.type);

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
		assertEquals("success", message.message.type);

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
		assertEquals("success", message.message.type);

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
		assertEquals("failure", message.message.type);
		assertEquals("1:2/3", getUser().getNpcOrderLocation());
	}
}