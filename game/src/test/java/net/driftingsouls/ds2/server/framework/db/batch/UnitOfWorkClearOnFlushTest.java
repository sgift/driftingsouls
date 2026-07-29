package net.driftingsouls.ds2.server.framework.db.batch;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pins down what {@link UnitOfWork#setClearOnFlush(boolean)} does to the work objects it is handed,
 * because every tick in this codebase builds on that contract.
 *
 * <p>{@code executeFor} clears the persistence context after each flush. Any entity that was loaded
 * <em>before</em> {@code executeFor} started - which is what a tick does when it queries its work
 * list up front - is therefore detached from the first flush boundary onwards. Two consequences
 * follow, and the ticks hit one or the other depending on what their {@code doWork} touches:
 *
 * <ul>
 *   <li>reading an uninitialized lazy association throws {@code LazyInitializationException}</li>
 *   <li>writing to the entity is silently dropped - a detached entity is not dirty-checked</li>
 * </ul>
 *
 * <p>Both are demonstrated here so the per-tick tests can stay focused on which of the two applies.
 */
public class UnitOfWorkClearOnFlushTest extends DBTest
{
	@Test
	public void executeFor_withClearOnFlush_detachesPreloadedWorkObjectsAfterTheFirstFlush()
	{
		mitTransaktion(() -> {
			for (int i = 0; i < 3; i++)
			{
				persist(new User("user" + i, "***", 0, "", new Cargo(), "user" + i + "@localhost"));
			}
		});

		List<User> users = getEM().createQuery("from User u where u.id > 0 order by u.id", User.class).getResultList();
		assertEquals(3, users.size());

		List<Boolean> attachedDuringWork = new ArrayList<>();

		new UnitOfWork<User>("detach probe", getEM())
		{
			@Override
			public void doWork(User user)
			{
				attachedDuringWork.add(getEM().contains(user));
			}
		}
		.setFlushSize(1)
		.setClearOnFlush(true)
		.executeFor(users);

		assertTrue("The first object is still attached to the context", attachedDuringWork.get(0));
		assertFalse("Object 2 must be detached after the first clear", attachedDuringWork.get(1));
		assertFalse("Object 3 must be detached after the first clear", attachedDuringWork.get(2));
	}

	@Test
	public void executeFor_withClearOnFlush_silentlyDropsWritesToDetachedWorkObjects()
	{
		mitTransaktion(() -> {
			for (int i = 0; i < 3; i++)
			{
				persist(new User("user" + i, "***", 0, "", new Cargo(), "user" + i + "@localhost"));
			}
		});

		List<User> users = getEM().createQuery("from User u where u.id > 0 order by u.id", User.class).getResultList();
		List<Integer> userIds = new ArrayList<>();
		users.forEach(user -> userIds.add(user.getId()));

		new UnitOfWork<User>("write probe", getEM())
		{
			@Override
			public void doWork(User user)
			{
				user.setNpcPunkte(42);
			}
		}
		.setFlushSize(1)
		.setClearOnFlush(true)
		.executeFor(users);

		getEM().clear();

		assertEquals("The first object was still attached, so the write must land",
				42, getEM().find(User.class, userIds.get(0)).getNpcPunkte());
		assertEquals("A write to a detached object must not land",
				0, getEM().find(User.class, userIds.get(1)).getNpcPunkte());
		assertEquals("A write to a detached object must not land",
				0, getEM().find(User.class, userIds.get(2)).getNpcPunkte());
	}

	/**
	 * A unit of work that clears the persistence context has to be driven by ids rather than by
	 * pre-loaded entities. The error path must cope with that: {@code EntityManager.contains}
	 * throws {@code IllegalArgumentException} for a non-entity, which would turn a single failed
	 * work object into an aborted run - and in a tick, into every later step being skipped.
	 */
	@Test
	public void executeFor_withNonEntityWorkObjects_keepsGoingAfterAFailedWorkObject()
	{
		List<Integer> processed = new ArrayList<>();

		new UnitOfWork<Integer>("non-entity work objects", getEM())
		{
			@Override
			public void doWork(Integer value)
			{
				processed.add(value);
				if (value == 2)
				{
					throw new IllegalStateException("boom");
				}
			}
		}
		.setFlushSize(1)
		.setClearOnFlush(true)
		.setErrorReporter((unitOfWork, failed, e) -> { /* no mail from a test */ })
		.executeFor(List.of(1, 2, 3));

		assertEquals("Every work object must be attempted, including the ones after the failure",
				List.of(1, 2, 3), processed);
	}

	@Test
	public void executeFor_withClearOnFlush_throwsOnLazyAssociationOfDetachedWorkObject()
	{
		mitTransaktion(() -> {
			BaseType baseType = persist(new BaseType("TestKlasse"));
			for (int i = 0; i < 3; i++)
			{
				User owner = persist(new User("owner" + i, "***", 0, "", new Cargo(), "owner" + i + "@localhost"));
				persist(new Base(new Location(1, 1, i + 1), owner, baseType));
			}
		});

		List<Base> bases = getEM().createQuery("from Base b order by b.id", Base.class).getResultList();
		assertEquals(3, bases.size());

		List<String> outcomes = new ArrayList<>();

		new UnitOfWork<Base>("lazy probe", getEM())
		{
			@Override
			public void doWork(Base base)
			{
				try
				{
					// Base.owner is a lazy ManyToOne, i.e. an uninitialized proxy on a freshly
					// queried Base. This is the access that blew up in production in AcademyTick.
					base.getOwner().getId();
					outcomes.add("ok");
				}
				catch (org.hibernate.LazyInitializationException e)
				{
					outcomes.add("lazy-init");
				}
			}
		}
		.setFlushSize(1)
		.setClearOnFlush(true)
		.executeFor(bases);

		if (!"ok".equals(outcomes.get(0)))
		{
			fail("The first object is still attached, so the access must succeed");
		}
		assertEquals("lazy-init", outcomes.get(1));
		assertEquals("lazy-init", outcomes.get(2));
	}
}
