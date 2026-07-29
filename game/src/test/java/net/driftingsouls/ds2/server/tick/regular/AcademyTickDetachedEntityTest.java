package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.AcademyQueueEntry;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.Academy;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for the production {@code LazyInitializationException} in {@link AcademyTick}
 * ("could not initialize proxy - no Session" at {@code User.getUserValue}).
 *
 * <p>The tick runs its academies through a {@code UnitOfWork} with {@code setClearOnFlush(true)},
 * i.e. the persistence context is cleared after every 10 academies. The tick used to load the full
 * {@code Academy} list up front and hand those instances to the unit of work, so from the 11th
 * academy onwards it was working on detached entities: reaching {@code base.getOwner()} hit an
 * uninitialized proxy without a session, and — just as bad but silently — none of the mutations on
 * those detached entities were written back.
 *
 * <p>The scenario therefore needs more academies than the flush size of 10.
 */
public class AcademyTickDetachedEntityTest extends DBTest
{
	/** Flush size configured in {@link AcademyTick}; academies beyond it were the broken ones. */
	private static final int ACADEMY_COUNT = 12;

	@Test
	public void academyTick_withMoreAcademiesThanTheFlushSize_finishesTrainingOnEveryAcademy()
	{
		List<Integer> academyIds = new ArrayList<>();

		mitTransaktion(() -> {
			BaseType baseType = persist(new BaseType("TestKlasse"));

			for (int i = 0; i < ACADEMY_COUNT; i++)
			{
				User owner = persist(new User("owner" + i, "***", 0, "", new Cargo(), "owner" + i + "@localhost"));
				// Suppresses the "Ausbildung abgeschlossen" PM: the tick sends it from the user with
				// id -1, which does not exist in a test schema. The read of this very user value is
				// what used to blow up on the detached proxy, so it is still exercised.
				owner.setUserValue(WellKnownUserValue.GAMEPLAY_USER_OFFICER_BUILD_PM, false);

				Base base = persist(new Base(new Location(1, 1, i + 1), owner, baseType));

				Academy academy = persist(new Academy(base));
				academy.setTrain(true);
				base.setAcademy(academy);

				// Further training of an officer that does not exist: finishBuildProcess() removes
				// the queue entry and returns early, which keeps the test independent of the
				// name generator and the officer templates in Offiziere.LIST.
				AcademyQueueEntry entry = persist(new AcademyQueueEntry(academy, 999_000 + i, 1, 1));
				entry.setScheduled(true);
				academy.addQueueEntry(entry);

				academyIds.add(academy.getId());
			}
		});

		AcademyTick academyTick = (AcademyTick) getContext().getBean(AcademyTick.class, null);
		academyTick.execute();

		// Every academy - including those processed after the first persistence-context clear -
		// must have had its queue entry finished and training switched off. Before the fix the
		// academies from index 10 on were untouched (their work was rolled back by the
		// LazyInitializationException, and their changes were never flushed anyway).
		for (int academyId : academyIds)
		{
			Academy academy = getEM().find(Academy.class, academyId);
			getEM().refresh(academy);

			assertEquals("Bauschlange der Akademie " + academyId + " nicht abgearbeitet",
					0, academy.getQueueEntries().size());
			assertEquals("Akademie " + academyId + " trainiert weiterhin",
					false, academy.getTrain());
		}
	}
}
