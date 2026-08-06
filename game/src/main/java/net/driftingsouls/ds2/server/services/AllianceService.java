package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service fuer Allianzen.
 */
@Service
@RequestScope
public class AllianceService
{
	public void delete(@Nonnull Ally alliance)
	{
		var db = ContextMap.getContext().getEM();

		List<ComNetChannel> chnList = db.createQuery("from ComNetChannel where allyOwner=:owner", ComNetChannel.class)
				.setParameter("owner", alliance)
				.getResultList();
		for (ComNetChannel channel: chnList)
		{
			db.createQuery("delete from ComNetVisit where channel=:channel")
					.setParameter("channel", channel)
					.executeUpdate();

			db.createQuery("delete from ComNetEntry where channel=:channel")
					.setParameter("channel", channel)
					.executeUpdate();

			db.remove(channel);
		}

		int tick = ContextMap.getContext().get(ContextCommon.class).getTick();

		List<User> uids = db.createQuery("from User where ally=:ally", User.class)
				.setParameter("ally", alliance)
				.getResultList();
		for (User auser: uids)
		{
			auser.addHistory(Common.getIngameTime(tick) + ": Verlassen der Allianz " + alliance.getName() + " im Zuge der Aufl&ouml;sung dieser Allianz");
			auser.setAlly(null);
			if (auser.getAllyPosten() != null)
			{
				AllyPosten posten = auser.getAllyPosten();
				auser.setAllyPosten(null);
				db.remove(posten);
			}
			auser.setName(auser.getNickname());
		}

		db.createQuery("delete from AllyPosten where ally=:ally")
				.setParameter("ally", alliance)
				.executeUpdate();

		// Delete Ally from running Battles

		var battleQuery = db.createQuery("from Battle " +
				"where ally1 = :ally or ally2 = :ally", Battle.class)
				.setParameter("ally", alliance.getId());

		Set<Battle> battles = new LinkedHashSet<>(battleQuery.getResultList());
		for (Battle battle : battles)
		{
			if (battle.getAlly(0) == alliance.getId())
			{
				battle.setAlly(0, 0);
			}
			if (battle.getAlly(1) == alliance.getId())
			{
				battle.setAlly(1, 0);
			}
		}

		alliance.removeAllMembers();
		db.remove(alliance);
	}

	/**
	 * Entfernt einen Spieler aus der Allianz.
	 *
	 * @param alliance Die Allianz
	 * @param member Der Spieler
	 */
	public void removeMember(@Nonnull Ally alliance, User member, EntityManager db)
	{
		final Context context = ContextMap.getContext();

		alliance.removeMember(member);
		member.setAlly(null);
		member.setAllyPosten(null);
		member.setName(member.getNickname());

		db.createQuery("update Battle set ally1=0 where commander1= :user and ally1= :ally")
				.setParameter("user", member)
				.setParameter("ally", alliance)
				.executeUpdate();

		db.createQuery("update Battle set ally2=0 where commander2= :user and ally2= :ally")
				.setParameter("user", member)
				.setParameter("ally", alliance)
				.executeUpdate();

		int tick = context.get(ContextCommon.class).getTick();
		member.addHistory(Common.getIngameTime(tick) + ": Verlassen der Allianz " + alliance.getName());

		checkForTooFewMembers(alliance, db);
	}

	/**
	 * Prueft, ob die Allianz noch genug Mitglieder hat um ihr
	 * Fortbestehen zu sichern. Falls dies nicht mehr der Fall ist
	 * wird eine entsprechende Task gesetzt und die Mitglieder davon
	 * in kenntnis gesetzt.
	 *
	 * @param alliance Die Allianz
	 */
	public void checkForTooFewMembers(@Nonnull Ally alliance, EntityManager db)
	{
		// Ist der Praesident kein NPC (negative ID) ?
		if (alliance.getPresident().getId() > 0)
		{
			long count = alliance.getMemberCount();
			if (count < 3)
			{
				Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(alliance.getId()), "", "");

				final User nullUser = db.find(User.class, 0);

				List<User> supermembers = getAllianceLeadership(alliance);
				for (User supermember : supermembers)
				{
					PM.send(nullUser, supermember.getId(), "Drohende Allianzauflösung",
							"[Automatische Nachricht]\n" +
									"Achtung!\n" +
									"Durch den jüngsten Weggang eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder, um weiterhin zu bestehen. " +
									"Du hast nun 21 Ticks Zeit, diesen Zustand zu ändern. Andernfalls wird die Allianz aufgelöst.", db);
				}
			}
		}
	}

	/**
	 * Gibt zurueck, ob der angegebene Benutzer momentan an einer laufenden Allianzgruendung beteiligt ist.
	 * @param user Der User
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isUserInvolvedInAllianceFounding(@Nonnull User user)
	{
		Taskmanager taskmanager = Taskmanager.getInstance();

		Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_FOUND, "*", "*", "*");
        for (Task task : tasks) {
            int[] users = Common.explodeToInt(",", task.getData3());
            for (int user1 : users) {
                if (user1 == user.getId()) {
                    return true;
                }
            }
        }

        return false;
	}

	/**
	 * Gibt alle Mitglieder der Allianzfuehrung zurueck, d.h. User mit Posten in der Allianz und der
	 * Allianzpraesident.
	 * @param alliance Die Allianz
	 * @return Die Liste der User
	 */
	public List<User> getAllianceLeadership(@Nonnull Ally alliance)
	{
		Session db = ContextMap.getContext().getDB();
		List<?> userList = db
				.createQuery("select distinct u from User u join u.ally a " +
						"where a.id= :allyId and (u.allyposten is not null or a.president.id=u.id)")
				.setInteger("allyId", alliance.getId())
				.list();
		return Common.cast(userList);
	}
}
