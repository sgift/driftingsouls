package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.DBTest;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for the <em>silent</em> half of the detached-entity defect described in
 * {@link net.driftingsouls.ds2.server.framework.db.batch.UnitOfWorkClearOnFlushTest}.
 *
 * <p>Unlike {@code AcademyTick}, the NPC point payout in {@link NPCOrderTick} never reads a lazy
 * association, so it never threw anything and never showed up in a stack trace. It only writes to
 * the pre-loaded work object - and a detached entity is not dirty-checked, so from the flush
 * boundary onwards those writes went nowhere. Players past that boundary simply stopped being paid.
 */
public class NPCOrderTickDetachedEntityTest extends DBTest
{
	/** Default flush size of the payout unit of work; users beyond it were the broken ones. */
	private static final int USER_COUNT = 55;

	@Test
	public void npcOrderTick_withMoreUsersThanTheFlushSize_awardsPointsToEveryUser()
	{
		List<Integer> userIds = new ArrayList<>();

		mitTransaktion(() -> {
			for (int i = 0; i < USER_COUNT; i++)
			{
				User user = persist(new User("npc" + i, "***", 0, "", new Cargo(), "npc" + i + "@localhost"));
				user.setFlag(UserFlag.ORDER_MENU, true);
				user.setNpcPunkte(0);
				userIds.add(user.getId());
			}
		});

		NPCOrderTick npcOrderTick = (NPCOrderTick) getContext().getBean(NPCOrderTick.class, null);
		npcOrderTick.execute();

		getEM().clear();

		for (int i = 0; i < userIds.size(); i++)
		{
			assertEquals("User at index " + i + " (id " + userIds.get(i) + ") was not awarded a point",
					1, getEM().find(User.class, userIds.get(i)).getNpcPunkte());
		}
	}
}
