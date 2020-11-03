package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.items.Ware;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.ShipService;
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
public class ShipTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private ShipService shipService;

	private Ship tanker;
	private Ship container1;
	private Ship container2;
	private Ship jaeger1;
	private Ship jaeger2;
	private User user3;
	private User user2;
	private ItemID testWare;

	/**
	 * Laedt einige Schiffe
	 */
	@Before
	public void loadShips()
	{
		this.testWare = new ItemID(em.merge(new Ware(1, "Deuteriumfass")));
		User user1 = em.merge(new User(1, "testUser1", "testUser1", "***", 0, "","test@localhost", configService));
		this.user2 = em.merge(new User(2, "testUser2", "testUser2", "***", 0, "","test@localhost", configService));
		this.user2.setFlag(UserFlag.SUPER_DOCK);
		this.user3 = em.merge(new User(3, "testUser3", "testUser3", "***", 0, "","test@localhost", configService));

		ShipType tankerTyp = em.merge(new ShipType(ShipClasses.TANKER));
		tankerTyp.setADocks(2);
		tankerTyp.setJDocks(2);
		tankerTyp.setCargo(1500);

		ShipType containerTyp = em.merge(new ShipType(ShipClasses.CONTAINER));
		containerTyp.setCargo(1500);

		ShipType jaegerTyp = em.merge(new ShipType(ShipClasses.JAEGER));
		jaegerTyp.setFlags(ShipTypeFlag.JAEGER.getFlag());
		jaegerTyp.setCargo(1500);


		this.tanker = new Ship(user1, tankerTyp, 1, 1, 1);
		em.persist(tanker);
		Cargo cargo = new Cargo();
		cargo.addResource(this.testWare, 3);
		this.tanker.setCargo(cargo);


		this.container1 = new Ship(user1, containerTyp, 1, 1, 1);
		em.persist(container1);
		Cargo cargo2 = new Cargo();
		cargo2.addResource(this.testWare, 2);
		this.container1.setCargo(cargo2);

		this.container2 = new Ship(user1, containerTyp, 1, 1, 1);
		em.persist(container2);
		Cargo cargo3 = new Cargo();
		cargo3.addResource(this.testWare, 5);
		this.container2.setCargo(cargo3);

		this.jaeger1 = new Ship(user1, jaegerTyp, 1, 1, 1);
		em.persist(jaeger1);
		Cargo cargo4 = new Cargo();
		cargo4.addResource(this.testWare, 7);
		this.jaeger1.setCargo(cargo4);

		this.jaeger2 = new Ship(user1, jaegerTyp, 1, 1, 1);
		em.persist(jaeger2);
	}

	@Test
	@Transactional
	public void gegebenEinSchiffOhneAngedockteContainer_dock_sollteDenContainerAndockenUndDenCargoZusammenlegen()
	{
		// setup
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(container2.isLanded() || container2.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.dock(tanker, container1);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(container2.isLanded() || container2.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(3000L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(false));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(5L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemAngedockteContainer_dock_sollteDenContainerAndockenUndDenCargoZusammenlegen()
	{
		// setup
		assertThat(shipService.dock(tanker, container1), is(false));

		// run
		boolean dock = shipService.dock(tanker, container2);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(4500L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(0L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(10L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemAngedockteContainerUndGenauDiesenContainerAlsNeuesZiel_dock_sollteDenContainerNichtErneutAndocken()
	{
		// setup
		assertThat(shipService.dock(tanker, container1), is(false));

		// run
		boolean dock = shipService.dock(tanker, container1);

		// assert
		assertThat(dock, is(true));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(container2.isLanded() || container2.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(3000L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(false));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(5L));
	}

	/**
	 * Testet das Andocken von Containern an ein Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffEinesSpielersMitSuperDockRechtenUndEinContainerEinesAnderenSpielers_dock_sollteDiesenContainerAndocken()
	{
		// setup
		this.tanker.setOwner(user2);

		assertThat(container1.isLanded() ||container1.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.dock(tanker, container1);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(3000L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(5L));
	}

	/**
	 * Testet das Andocken von Containern an ein Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffEinesSpielersOhneSuperDockRechtenUndEinContainerEinesAnderenSpielers_dock_sollteDiesenContainerNichtAndocken()
	{
		// setup
		tanker.setOwner(user3);

		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.dock(tanker, container1);

		// assert
		assertThat(dock, is(true));
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getCargo().isEmpty(), is(false));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(3L));
	}

	/**
	 * Testet das Andocken von mehreren Containern auf einmal an ein Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffUndMehrereGleichzeitigAnzudockendeContainer_dock_sollteDieseAlleAndockenUndDieCargosZusammenlegen()
	{

		// setup
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(container2.isLanded() || container2.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.dock(tanker, container1, container2);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(4500L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(0L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(10L));
	}

	/**
	 * Testet das Abdocken von Containern an ein Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndEinemDavonAbzudockendenContainer_undock_sollteEinenContainerAbdocken()
	{
		// setup
		assertThat(shipService.dock(tanker, container1, container2), is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(4500L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(0L));

		// run
		shipService.undock(tanker, container1);

		// assert
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(3000L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container2.getTypeData().getCargo(), is(0L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(10L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndSehrVielCargoUndEinemDavonAbzudockendenContainer_undock_sollteEinenContainerAbdockenUndDenCargoAufteilen()
	{
		// setup
		assertThat(shipService.dock(tanker, container1, container2), is(false));
		Cargo cargo = this.tanker.getCargo();
		cargo.setResource(testWare, 4000);
		tanker.setCargo(cargo);
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(4500L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(0L));

		// run
		shipService.undock(tanker, container1);

		// assert
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(3000L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container2.getTypeData().getCargo(), is(0L));
		assertThat(container1.getCargo().getResourceCount(testWare), is(1000L));
		assertThat(container2.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(3000L));
	}

	/**
	 * Testet das Abdocken aller Container an ein Schiff auf einmal
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffMitMehrerenGedocktenContainern_undock_sollteAlleContainerAbdocken()
	{

		// setup
		assertThat(shipService.dock(tanker, container1, container2), is(false));
		assertThat(shipService.getBaseShip(container1).getId(), is(tanker.getId()));
		assertThat(shipService.getBaseShip(container2).getId(), is(tanker.getId()));
		assertThat(tanker.getTypeData().getCargo(), is(4500L));
		assertThat(container1.getTypeData().getCargo(), is(0L));
		assertThat(container2.getTypeData().getCargo(), is(0L));

		// run
		shipService.undock(tanker);

		// assert
		assertThat(container1.isLanded() || container1.isDocked(), is(false));
		assertThat(container2.isLanded() || container2.isDocked(), is(false));
		assertThat(tanker.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getTypeData().getCargo(), is(1500L));
		assertThat(container2.getTypeData().getCargo(), is(1500L));
		assertThat(container1.getCargo().isEmpty(), is(true));
		assertThat(container2.getCargo().isEmpty(), is(true));
		assertThat(tanker.getCargo().getResourceCount(testWare), is(10L));
	}

	/**
	 * Testet das Abdocken einer Liste von Container von ein Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndEineListeAbzudockenderContainer_undock_sollteDieseContainerAbdocken()
	{
		// setup
		assertThat(shipService.dock(this.tanker, this.container1, this.container2), is(false));
		assertThat(shipService.getBaseShip(this.container1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.container2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// run
		shipService.undock(this.tanker, this.container1, this.container2);

		// assert
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.container2.isLanded() || this.container2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(10L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiGelandetenJaegernUndEinemZustartendenJaeger_start_sollteDiesenJaegerStarten()
	{
		// setup
		assertThat(shipService.land(this.tanker, this.jaeger1, this.jaeger2), is(false));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.jaeger2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));

		// run
		shipService.start(this.tanker, this.jaeger1);

		// assert
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(shipService.getBaseShip(this.jaeger2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemGelandetenJaegerUndDiesemZuStartendenJaeger_start_sollteDiesenJaegerStarten()
	{
		// setup
		assertThat(shipService.land(this.tanker, this.jaeger2), is(false));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(shipService.getBaseShip(this.jaeger2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));

		// run
		shipService.start(this.tanker, this.jaeger2);

		// ssert
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	/**
	 * Testet das Starten aller Jaegern von einem Schiff
	 */
	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiGelandetenJaegern_start_sollteAlleJaegerStarten()
	{
		// setup
		assertThat(shipService.land(this.tanker, this.jaeger1, this.jaeger2), is(false));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(true));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(true));

		// run
		shipService.start(this.tanker);

		// assert

		// TODO: Refresh ist momentan notwendig, da die Objekte innerhalb der Session nicht aktualisiert werden (update-Statements sind problematisch)
		this.jaeger1 = em.merge(this.jaeger1);
		this.jaeger2 = em.merge(this.jaeger2);

		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffOhneGedockteContainer_getDockedShips_sollteEineLeereListeZurueckgeben()
	{
		// setup
		// run
		List<Ship> dockedShips = shipService.getDockedShips(this.tanker);

		// assert
		assertThat(dockedShips.size(), is(0));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitZweiGedocktenContainern_getDockedShips_sollteEineListeMitDiesenBeidenContainerZurueckgeben()
	{
		// setup
		assertThat(shipService.dock(this.tanker, this.container1, this.container2), is(false));
		assertThat(shipService.getBaseShip(this.container1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.container2).getId(), is(this.tanker.getId()));

		// run
		List<Ship> dockedShips = shipService.getDockedShips(this.tanker);

		// assert
		assertThat(dockedShips.size(), is(2));
		assertThat(dockedShips.contains(this.container1), is(true));
		assertThat(dockedShips.contains(this.container2), is(true));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemGedocktenContainer_getDockedShips_sollteEineListeMitDiesemContainerZurueckgeben()
	{
		// setup
		assertThat(shipService.dock(this.tanker, this.container1), is(false));
		assertThat(shipService.getBaseShip(this.container1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.container2), is(nullValue()));

		// run
		List<Ship> dockedShips = shipService.getDockedShips(this.tanker);

		// assert
		assertThat(dockedShips.size(), is(1));
		assertThat(dockedShips.contains(this.container1), is(true));
		assertThat(dockedShips.contains(this.container2), is(false));
	}


	@Test
	@Transactional
	public void gegebenEinSchiffOhneGelandeteJaeger_land_sollteDenJaegerLandenUndDenCargoNichtVeraendern()
	{
		// setup
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger1);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().isEmpty(), is(false));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemGelandetenJaeger_land_sollteDenJaegerLandenUndDenCargoNichtVeraendern()
	{
		// setup
		assertThat(shipService.land(this.tanker, this.jaeger1), is(false));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger2);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.jaeger2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffMitEinemGelandetenjaegerUndGenauDiesenJaegerAlsZiel_land_sollteDenJaegerNichtErneutLanden()
	{
		// setup
		assertThat(shipService.land(this.tanker, this.jaeger1), is(false));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffEinesSpielersMitSuperDockRechtenUndEinJaegerEinesAnderenSpielers_land_sollteDiesenJaegerNichtLanden()
	{
		// setup
		this.tanker.setOwner(user2);

		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffEinesSpielersOhneSuperDockRechtenUndEinJaegerEinesAnderenSpielers_land_sollteDiesenJaegerNichtLanden()
	{
		// setup
		this.tanker.setOwner(user3);

		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffUndMehrereGleichzeitigZuLandendeJaeger_land_sollteDieseAlleLandenAberDenCargoNichtVeraendern()
	{
		// setup
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.land(this.tanker, this.jaeger1, this.jaeger2);

		// assert
		assertThat(dock, is(false));
		assertThat(shipService.getBaseShip(this.jaeger1).getId(), is(this.tanker.getId()));
		assertThat(shipService.getBaseShip(this.jaeger2).getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	@Transactional
	public void gegebenEinSchiffUndEinZuLandenderContainer_land_sollteDiesenNichtLanden()
	{
		// setup
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = shipService.land(this.tanker, this.container1);

		// assert
		assertThat(dock, is(false));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
	}
}
