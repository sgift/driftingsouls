package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Ware;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ShipTest extends DBSingleTransactionTest
{
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
		this.testWare = new ItemID((Item) getDB().merge(new Ware(1, "Deuteriumfass")));
		User user1 = persist(new User("testUser1", "***", 0, "", new Cargo(), "test@localhost"));
		this.user2 = persist(new User("testUser2", "***", 0, "", new Cargo(), "test@localhost"));
		this.user2.setFlag(UserFlag.SUPER_DOCK);

		this.user3 = persist(new User("testUser3", "***", 0, "", new Cargo(), "test@localhost"));

		ShipType tankerTyp = persist(new ShipType(ShipClasses.TANKER));
		tankerTyp.setADocks(2);
		tankerTyp.setJDocks(2);
		tankerTyp.setCargo(1500);

		ShipType containerTyp = persist(new ShipType(ShipClasses.CONTAINER));
		containerTyp.setCargo(1500);

		ShipType jaegerTyp = persist(new ShipType(ShipClasses.JAEGER));
		jaegerTyp.setFlags(ShipTypeFlag.JAEGER.getFlag());
		jaegerTyp.setCargo(1500);

		this.tanker = persist(new Ship(user1, tankerTyp, 1, 1, 1));
		Cargo cargo = new Cargo();
		cargo.addResource(this.testWare, 3);
		this.tanker.setCargo(cargo);

		this.container1 = persist(new Ship(user1, containerTyp, 1, 1, 1));
		Cargo cargo2 = new Cargo();
		cargo2.addResource(this.testWare, 2);
		this.container1.setCargo(cargo2);

		this.container2 = persist(new Ship(user1, containerTyp, 1, 1, 1));
		Cargo cargo3 = new Cargo();
		cargo3.addResource(this.testWare, 5);
		this.container2.setCargo(cargo3);

		this.jaeger1 = persist(new Ship(user1, jaegerTyp, 1, 1, 1));
		Cargo cargo4 = new Cargo();
		cargo4.addResource(this.testWare, 7);
		this.jaeger1.setCargo(cargo4);

		this.jaeger2 = persist(new Ship(user1, jaegerTyp, 1, 1, 1));
	}

	@Test
	public void gegebenEinSchiffOhneAngedockteContainer_dock_sollteDenContainerAndockenUndDenCargoZusammenlegen()
	{
		// setup
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.container2.isLanded() || this.container2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.dock(this.container1);

		// assert
		assertThat(dock, is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.isLanded() || this.container2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(false));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(5L));
	}

	@Test
	public void gegebenEinSchiffMitEinemAngedockteContainer_dock_sollteDenContainerAndockenUndDenCargoZusammenlegen()
	{
		// setup
		assertThat(this.tanker.dock(this.container1), is(false));

		// run
		boolean dock = this.tanker.dock(this.container2);

		// assert
		assertThat(dock, is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(10L));
	}

	@Test
	public void gegebenEinSchiffMitEinemAngedockteContainerUndGenauDiesenContainerAlsNeuesZiel_dock_sollteDenContainerNichtErneutAndocken()
	{
		// setup
		assertThat(this.tanker.dock(this.container1), is(false));

		// run
		boolean dock = this.tanker.dock(this.container1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.isLanded() || this.container2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(false));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(5L));
	}

	/**
	 * Testet das Andocken von Containern an ein Schiff
	 */
	@Test
	public void gegebenEinSchiffEinesSpielersMitSuperDockRechtenUndEinContainerEinesAnderenSpielers_dock_sollteDiesenContainerAndocken()
	{
		// setup
		this.tanker.setOwner(user2);

		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.dock(this.container1);

		// assert
		assertThat(dock, is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(5L));
	}

	/**
	 * Testet das Andocken von Containern an ein Schiff
	 */
	@Test
	public void gegebenEinSchiffEinesSpielersOhneSuperDockRechtenUndEinContainerEinesAnderenSpielers_dock_sollteDiesenContainerNichtAndocken()
	{
		// setup
		this.tanker.setOwner(user3);

		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.dock(this.container1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getCargo().isEmpty(), is(false));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
	}

	/**
	 * Testet das Andocken von mehreren Containern auf einmal an ein Schiff
	 */
	@Test
	public void gegebenEinSchiffUndMehrereGleichzeitigAnzudockendeContainer_dock_sollteDieseAlleAndockenUndDieCargosZusammenlegen()
	{
		// setup
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.container2.isLanded() || this.container2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.dock(this.container1, this.container2);

		// assert
		assertThat(dock, is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(10L));
	}

	/**
	 * Testet das Abdocken von Containern an ein Schiff
	 */
	@Test
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndEinemDavonAbzudockendenContainer_undock_sollteEinenContainerAbdocken()
	{
		// setup
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// run
		this.tanker.undock(this.container1);

		// assert
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
		assertThat(this.container1.getCargo().isEmpty(), is(true));
		assertThat(this.container2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(10L));
	}

	@Test
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndSehrVielCargoUndEinemDavonAbzudockendenContainer_undock_sollteEinenContainerAbdockenUndDenCargoAufteilen()
	{
		// setup
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		Cargo cargo = this.tanker.getCargo();
		cargo.setResource(this.testWare, 4000);
		this.tanker.setCargo(cargo);
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// run
		this.tanker.undock(this.container1);

		// assert
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
		assertThat(this.container1.getCargo().getResourceCount(this.testWare), is(1000L));
		assertThat(this.container2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3000L));
	}

	/**
	 * Testet das Abdocken aller Container an ein Schiff auf einmal
	 */
	@Test
	public void gegebenEinSchiffMitMehrerenGedocktenContainern_undock_sollteAlleContainerAbdocken()
	{
		// setup
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// run
		this.tanker.undock();

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

	/**
	 * Testet das Abdocken einer Liste von Container von ein Schiff
	 */
	@Test
	public void gegebenEinSchiffMitZweiAngedocktenContainernUndEineListeAbzudockenderContainer_undock_sollteDieseContainerAbdocken()
	{
		// setup
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// run
		this.tanker.undock(this.container1, this.container2);

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
	public void gegebenEinSchiffMitZweiGelandetenJaegernUndEinemZustartendenJaeger_start_sollteDiesenJaegerStarten()
	{
		// setup
		assertThat(this.tanker.land(this.jaeger1, this.jaeger2), is(false));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));

		// run
		this.tanker.start(this.jaeger1);

		// assert
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	@Test
	public void gegebenEinSchiffMitEinemGelandetenJaegerUndDiesemZuStartendenJaeger_start_sollteDiesenJaegerStarten()
	{
		// setup
		assertThat(this.tanker.land(this.jaeger2), is(false));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));

		// run
		this.tanker.start(this.jaeger2);

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
	public void gegebenEinSchiffMitZweiGelandetenJaegern_start_sollteAlleJaegerStarten()
	{
		// setup
		assertThat(this.tanker.land(this.jaeger1, this.jaeger2), is(false));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.tanker.getCargo().getResourceCount(this.testWare), is(3L));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));

		// run
		this.tanker.start();

		// assert

		// TODO: Refresh ist momentan notwendig, da die Objekte innerhalb der Session nicht aktualisiert werden (update-Statements sind problematisch)
		getDB().refresh(this.jaeger1);
		getDB().refresh(this.jaeger2);

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
	public void gegebenEinSchiffOhneGedockteContainer_getDockedShips_sollteEineLeereListeZurueckgeben()
	{
		// setup
		// run
		List<Ship> dockedShips = this.tanker.getDockedShips();

		// assert
		assertThat(dockedShips.size(), is(0));
	}

	@Test
	public void gegebenEinSchiffMitZweiGedocktenContainern_getDockedShips_sollteEineListeMitDiesenBeidenContainerZurueckgeben()
	{
		// setup
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip().getId(), is(this.tanker.getId()));

		// run
		List<Ship> dockedShips = this.tanker.getDockedShips();

		// assert
		assertThat(dockedShips.size(), is(2));
		assertThat(dockedShips.contains(this.container1), is(true));
		assertThat(dockedShips.contains(this.container2), is(true));
	}

	@Test
	public void gegebenEinSchiffMitEinemGedocktenContainer_getDockedShips_sollteEineListeMitDiesemContainerZurueckgeben()
	{
		// setup
		assertThat(this.tanker.dock(this.container1), is(false));
		assertThat(this.container1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.container2.getBaseShip(), is(nullValue()));

		// run
		List<Ship> dockedShips = this.tanker.getDockedShips();

		// assert
		assertThat(dockedShips.size(), is(1));
		assertThat(dockedShips.contains(this.container1), is(true));
		assertThat(dockedShips.contains(this.container2), is(false));
	}


	@Test
	public void gegebenEinSchiffOhneGelandeteJaeger_land_sollteDenJaegerLandenUndDenCargoNichtVeraendern()
	{
		// setup
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.land(this.jaeger1);

		// assert
		assertThat(dock, is(false));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.tanker.getCargo().isEmpty(), is(false));
	}

	@Test
	public void gegebenEinSchiffMitEinemGelandetenJaeger_land_sollteDenJaegerLandenUndDenCargoNichtVeraendern()
	{
		// setup
		assertThat(this.tanker.land(this.jaeger1), is(false));

		// run
		boolean dock = this.tanker.land(this.jaeger2);

		// assert
		assertThat(dock, is(false));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
	}

	@Test
	public void gegebenEinSchiffMitEinemGelandetenjaegerUndGenauDiesenJaegerAlsZiel_land_sollteDenJaegerNichtErneutLanden()
	{
		// setup
		assertThat(this.tanker.land(this.jaeger1), is(false));

		// run
		boolean dock = this.tanker.land(this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	public void gegebenEinSchiffEinesSpielersMitSuperDockRechtenUndEinJaegerEinesAnderenSpielers_land_sollteDiesenJaegerNichtLanden()
	{
		// setup
		this.tanker.setOwner(user2);

		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.land(this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	public void gegebenEinSchiffEinesSpielersOhneSuperDockRechtenUndEinJaegerEinesAnderenSpielers_land_sollteDiesenJaegerNichtLanden()
	{
		// setup
		this.tanker.setOwner(user3);

		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.land(this.jaeger1);

		// assert
		assertThat(dock, is(true));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
	public void gegebenEinSchiffUndMehrereGleichzeitigZuLandendeJaeger_land_sollteDieseAlleLandenAberDenCargoNichtVeraendern()
	{
		// setup
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		// run
		boolean dock = this.tanker.land(this.jaeger1, this.jaeger2);

		// assert
		assertThat(dock, is(false));
		assertThat(this.jaeger1.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.jaeger2.getBaseShip().getId(), is(this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getCargo().isEmpty(), is(false));
		assertThat(this.jaeger2.getCargo().isEmpty(), is(true));
		assertThat(this.jaeger1.getCargo().getResourceCount(this.testWare), is(7L));
	}

	@Test
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
		boolean dock = this.tanker.land(this.container1);

		// assert
		assertThat(dock, is(false));
		assertThat(this.jaeger1.isLanded() || this.jaeger1.isDocked(), is(false));
		assertThat(this.jaeger2.isLanded() || this.jaeger2.isDocked(), is(false));
		assertThat(this.container1.isLanded() || this.container1.isDocked(), is(false));
	}
}
