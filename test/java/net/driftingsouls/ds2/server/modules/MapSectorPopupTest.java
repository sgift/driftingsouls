package net.driftingsouls.ds2.server.modules;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MapSectorPopupTest extends DBSingleTransactionTest
{
	private StarSystem sys;
	private User user;
	private ShipType shipType;

	@Before
	public void setUp()
	{
		sys = persist(new StarSystem());
		sys.setWidth(200);
		sys.setHeight(200);
		sys.setAccess(StarSystem.Access.NORMAL);
		sys.setStarmapVisible(true);

		user = persist(new User("testuser", "***", 0, "", new Cargo(), "test@localhost"));
		ContextMap.getContext().setActiveUser(user);

		shipType = persist(new ShipType(ShipClasses.AWACS));
		shipType.setSensorRange(2);
		shipType.setCrew(10);
	}

	@Test
	public void gegebenEinSichtbarerSprungpunkt_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = persist(new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt"));
		MapController mapController = new MapController();

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
	public void gegebenEinNichtSichtbarerSprungpunkt_sectorAction_sollteDiesenSprungpunktNichtZurueckgeben()
	{
		// setup
		JumpNode node = persist(new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt"));
		node.setHidden(true);
		MapController mapController = new MapController();

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, null, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(0, sectorViewModel.jumpnodes.size());
	}

	@Test
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffImSektor_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = persist(new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt"));
		node.setHidden(true);
		Ship ship = persist(new Ship(user, shipType, sys.getID(), 1, 1));
		ship.setCrew(shipType.getCrew());

		MapController mapController = new MapController();

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
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffInLrsReichweite_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = persist(new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt"));
		node.setHidden(true);
		Ship ship = persist(new Ship(user, shipType, sys.getID(), 2, 2));
		ship.setCrew(shipType.getCrew());

		MapController mapController = new MapController();

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
	public void gegebenEinNichtSichtbarerSprungpunktUndEinEigenesSchiffInAusserhalbDerLrsReichweite_sectorAction_sollteDiesenSprungpunktZurueckgeben()
	{
		// setup
		JumpNode node = persist(new JumpNode(new Location(sys.getID(), 1, 1), new Location(sys.getID(), 100,100), "Testsprungpunkt"));
		node.setHidden(true);
		Ship ship = persist(new Ship(user, shipType, sys.getID(), 50, 50));
		ship.setCrew(shipType.getCrew());

		MapController mapController = new MapController();

		// run
		MapController.SectorViewModel sectorViewModel = mapController.sectorAction(sys, 1, 1, ship, false);

		// assert
		assertNotNull(sectorViewModel);
		assertNotNull(sectorViewModel.jumpnodes);
		assertEquals(0, sectorViewModel.jumpnodes.size());
	}
}
