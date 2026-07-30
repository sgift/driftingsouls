package net.driftingsouls.ds2.server.werften;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipBaubar;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Regression test for {@code WerftObject.getRepairCosts}, which looked up the {@link ShipBaubar}
 * row for a ship by binding the ship's <em>type id</em> to a parameter compared against
 * {@code ShipBaubar.type} - a {@link ShipType} association.
 *
 * <p>This worked under the old Hibernate {@code Session} API, whose {@code setInteger} bound the
 * value against the foreign key column without checking. The move to
 * {@code EntityManager.setParameter} in 07f23f3e1 made the binding type-checked, so every call
 * failed with {@code IllegalArgumentException: Parameter value [9] did not match expected type}.
 * The shipyard repair view calls this for each repairable ship, so the whole page errored out.
 */
public class WerftObjectRepairCostsTest extends DBTest
{
	@Test
	public void getRepairCosts_forADamagedShip_looksUpTheBuildableEntryByShipType()
	{
		int[] werftId = new int[1];
		int[] shipId = new int[1];

		mitTransaktion(() -> {
			User owner = persist(new User("owner", "***", 0, "", new Cargo(), "owner@localhost"));
			BaseType baseType = persist(new BaseType("TestKlasse"));
			Base base = persist(new Base(new Location(1, 1, 1), owner, baseType));

			ShipType shipType = persist(new ShipType(ShipClasses.TRANSPORTER));
			shipType.setHull(1000);
			shipType.setCost(10);

			// The row the query has to find. Without it the method takes its fallback branch and
			// never reaches the binding under test.
			ShipBaubar buildable = new ShipBaubar(shipType);
			buildable.setCosts(new Cargo());
			buildable.setEKosten(100);
			persist(buildable);

			Ship ship = persist(new Ship(owner, shipType, 1, 1, 1));
			ship.setHull(500);

			BaseWerft werft = persist(new BaseWerft(base));

			werftId[0] = werft.getWerftID();
			shipId[0] = ship.getId();
		});

		WerftObject werft = getEM().find(WerftObject.class, werftId[0]);
		Ship ship = getEM().find(Ship.class, shipId[0]);

		assertNotNull("Repair costs could not be computed", werft.getRepairCosts(ship));
	}
}
