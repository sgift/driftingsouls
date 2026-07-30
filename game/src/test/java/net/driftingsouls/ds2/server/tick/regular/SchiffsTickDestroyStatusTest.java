package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.DBTest;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Regression test for {@code SchiffsTick.doDestroyStatus}: every ship carrying the {@code destroy}
 * status token must be gone by the end of the tick, not just the first one in the list.
 *
 * <p>The pass runs with {@code setFlushSize(1).setClearOnFlush(true)}, so the persistence context is
 * cleared after each single ship. When the ships were queried up front, everything after the first
 * was detached and {@code Ship.destroy()} could not complete on it - it reads the lazy ship type and
 * ends in {@code EntityManager.remove}, neither of which works detached. The failures were caught
 * per object by {@code UnitOfWork}, so the tick reported success while removing exactly one ship per
 * run. Ships marked long ago therefore stayed in service and were destroyed one at a time, ticks or
 * weeks after whatever marked them.
 *
 * <p>The ships are placed in system 0, which {@code tickUser} skips, so this exercises the
 * destruction pass without dragging the whole per-ship tick into the fixture.
 */
public class SchiffsTickDestroyStatusTest extends DBTest
{
	private static final int SHIP_COUNT = 5;

	@Test
	public void schiffsTick_withSeveralShipsFlaggedForDestruction_destroysAllOfThem()
	{
		List<Integer> shipIds = new ArrayList<>();

		mitTransaktion(() -> {
			User owner = persist(new User("owner", "***", 0, "", new Cargo(), "owner@localhost"));

			for (int i = 0; i < SHIP_COUNT; i++)
			{
				// A type per ship, deliberately. Sharing one type hides the defect: destroying the
				// first ship initialises that single shared proxy, and every later ship then finds
				// it already initialised and survives the cleared context. Real fleets are mixed.
				ShipType shipType = persist(new ShipType(ShipClasses.TRANSPORTER));

				Ship ship = persist(new Ship(owner, shipType, 0, 0, 0));
				ship.setStatus("destroy");
				shipIds.add(ship.getId());
			}
		});

		assertEquals(SHIP_COUNT, shipIds.size());

		// Also essential: without this the tick's query returns the very Ship instances built
		// above, straight out of the first-level cache, with the real ShipType in the field.
		// Production loads them cold, so Ship.shiptype - a LAZY ManyToOne - is an uninitialized
		// proxy, and that is what stops working once the context is cleared mid-run.
		getEM().clear();

		SchiffsTick schiffsTick = (SchiffsTick) getContext().getBean(SchiffsTick.class, null);
		// prepare()/tick() instead of execute(): execute() swallows every exception into
		// Common.mailThrowable, which would let an aborted tick pass as a silent no-op.
		schiffsTick.prepare();
		schiffsTick.tick();

		getEM().clear();

		for (int shipId : shipIds)
		{
			assertNull("Ship " + shipId + " was flagged for destruction but is still there",
					getEM().find(Ship.class, shipId));
		}
	}
}
