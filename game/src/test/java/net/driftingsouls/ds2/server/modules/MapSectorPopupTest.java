package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
@WebAppConfiguration
public class MapSectorPopupTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private JavaSession javaSession;
	@Autowired
	private MapController mapController;

	private StarSystem sys;
	private User user;
	private ShipType shipType;

	@Before
	public void setUp()
	{
		sys = new StarSystem();
		em.persist(sys);
		sys.setWidth(200);
		sys.setHeight(200);
		sys.setAccess(StarSystem.Access.NORMAL);
		sys.setStarmapVisible(true);

		user = new User(1, "testuser", "testuser", "***", 0, "", "test@localhost", configService);
		em.persist(user);
		javaSession.setUser(user);

		shipType = new ShipType(ShipClasses.AWACS);
		em.persist(shipType);
		shipType.setSensorRange(2);
		shipType.setCrew(10);
	}

	@Test
	@Transactional
	public void gegebenEinSichtbarerSprungpunkt_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt");
		em.persist(node);

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, null, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(1, sectorViewModel.jumpnodes.size());
		MapController.SectorViewModel.JumpNodeViewModel jumpNodeViewModel = sectorViewModel.jumpnodes.get(0);
		assertFalse(jumpNodeViewModel.blocked);
		assertEquals(node.getName(), jumpNodeViewModel.name);
		assertEquals(node.getId(), jumpNodeViewModel.id);
	}

	@Test
	@Transactional
	public void gegebenEinNichtSichtbarerSprungpunkt_sectorAction_sollteDiesenSprungpunktNichtZurueckgeben()
	{
		// setup
		JumpNode node = new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt");
		em.persist(node);
		node.setHidden(true);

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, null, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(0, sectorViewModel.jumpnodes.size());
	}

	@Test
	@Transactional
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffImSektor_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt");
		em.persist(node);
		node.setHidden(true);
		Ship ship = new Ship(user, shipType, sys.getID(), 1, 1);
		em.persist(ship);
		ship.setCrew(shipType.getCrew());

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, ship, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(1, sectorViewModel.jumpnodes.size());
		MapController.SectorViewModel.JumpNodeViewModel jumpNodeViewModel = sectorViewModel.jumpnodes.get(0);
		assertFalse(jumpNodeViewModel.blocked);
		assertEquals(node.getName(), jumpNodeViewModel.name);
		assertEquals(node.getId(), jumpNodeViewModel.id);
	}

	@Test
	@Transactional
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffInLrsReichweite_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt");
		em.persist(node);
		node.setHidden(true);
		Ship ship = new Ship(user, shipType, sys.getID(), 2, 2);
		em.persist(ship);
		ship.setCrew(shipType.getCrew());

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, ship, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(1, sectorViewModel.jumpnodes.size());
		MapController.SectorViewModel.JumpNodeViewModel jumpNodeViewModel = sectorViewModel.jumpnodes.get(0);
		assertFalse(jumpNodeViewModel.blocked);
		assertEquals(node.getName(), jumpNodeViewModel.name);
		assertEquals(node.getId(), jumpNodeViewModel.id);
	}

	@Test
	@Transactional
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffInAusserhalbDerLrsReichweite_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt");
		em.persist(node);
		node.setHidden(true);
		Ship ship = new Ship(user, shipType, sys.getID(), 50, 50);
		em.persist(ship);
		ship.setCrew(shipType.getCrew());

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, ship, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(0, sectorViewModel.jumpnodes.size());
	}
}
