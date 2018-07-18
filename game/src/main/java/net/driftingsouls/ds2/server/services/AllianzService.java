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
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service fuer Allianzen.
 */
@Service
public class AllianzService
{
	public void loeschen(@Nonnull Ally allianz)
	{
		org.hibernate.Session db = ContextMap.getContext().getDB();

		List<?> chnList = db.createQuery("from ComNetChannel where allyOwner=:owner")
				.setParameter("owner", allianz)
				.list();
		for (Object aChnList : chnList)
		{
			ComNetChannel channel = (ComNetChannel) aChnList;

			db.createQuery("delete from ComNetVisit where channel=:channel")
					.setEntity("channel", channel)
					.executeUpdate();

			db.createQuery("delete from ComNetEntry where channel=:channel")
					.setEntity("channel", channel)
					.executeUpdate();

			db.delete(channel);
		}

		int tick = ContextMap.getContext().get(ContextCommon.class).getTick();

		List<?> uids = db.createQuery("from User where ally=:ally")
				.setEntity("ally", allianz)
				.list();
		for (Object uid : uids)
		{
			User auser = (User) uid;

			auser.addHistory(Common.getIngameTime(tick) + ": Verlassen der Allianz " + allianz.getName() + " im Zuge der Aufl&ouml;sung dieser Allianz");
			auser.setAlly(null);
			if (auser.getAllyPosten() != null)
			{
				AllyPosten posten = auser.getAllyPosten();
				auser.setAllyPosten(null);
				db.delete(posten);
			}
			auser.setName(auser.getNickname());
		}

		db.createQuery("delete from AllyPosten where ally=:ally")
				.setEntity("ally", allianz)
				.executeUpdate();

		// Delete Ally from running Battles
		Set<Battle> battles = new LinkedHashSet<>();

		Query battleQuery = db.createQuery("from Battle " +
				"where ally1 = :ally or ally2 = :ally")
				.setParameter("ally", allianz.getId());

		battles.addAll(Common.cast(battleQuery.list(), Battle.class));

		for (Battle battle : battles)
		{
			if (battle.getAlly(0) == allianz.getId())
			{
				battle.setAlly(0, 0);
			}
			if (battle.getAlly(1) == allianz.getId())
			{
				battle.setAlly(1, 0);
			}
		}

		allianz.removeAllMembers();
		db.delete(allianz);
	}

	/**
	 * Entfernt einen Spieler aus der Allianz.
	 *
	 * @param allianz Die Allianz
	 * @param mitglied Der Spieler
	 */
	public void entferneMitglied(@Nonnull Ally allianz, User mitglied)
	{

		final Context context = ContextMap.getContext();
		final org.hibernate.Session db = context.getDB();

		allianz.removeMember(mitglied);
		mitglied.setAlly(null);
		mitglied.setAllyPosten(null);
		mitglied.setName(mitglied.getNickname());

		db.createQuery("update Battle set ally1=0 where commander1= :user and ally1= :ally")
				.setEntity("user", mitglied)
				.setParameter("ally", allianz.getId())
				.executeUpdate();

		db.createQuery("update Battle set ally2=0 where commander2= :user and ally2= :ally")
				.setEntity("user", mitglied)
				.setParameter("ally", allianz.getId())
				.executeUpdate();

		int tick = context.get(ContextCommon.class).getTick();
		mitglied.addHistory(Common.getIngameTime(tick) + ": Verlassen der Allianz " + allianz.getName());

		pruefeAufZuWenigMitglieder(allianz);
	}

	/**
	 * Prueft, ob die Allianz noch genug Mitglieder hat um ihr
	 * Fortbestehen zu sichern. Falls dies nicht mehr der Fall ist
	 * wird eine entsprechende Task gesetzt und die Mitglieder davon
	 * in kenntnis gesetzt.
	 *
	 * @param allianz Die Allianz
	 */
	public void pruefeAufZuWenigMitglieder(@Nonnull Ally allianz)
	{
		final org.hibernate.Session db = ContextMap.getContext().getDB();

		// Ist der Praesident kein NPC (negative ID) ?
		if (allianz.getPresident().getId() > 0)
		{
			long count = allianz.getMemberCount();
			if (count < 3)
			{
				Taskmanager.getInstance().addTask(Taskmanager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(allianz.getId()), "", "");

				final User nullUser = (User) db.get(User.class, 0);

				List<User> supermembers = getAllianzfuehrung(allianz);
				for (User supermember : supermembers)
				{
					PM.send(nullUser, supermember.getId(), "Drohende Allianzauflösung",
							"[Automatische Nachricht]\n" +
									"Achtung!\n" +
									"Durch den jüngsten Weggang eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder um weiterhin zu bestehen. " +
									"Du hast nun 21 Ticks Zeit diesen Zustand zu ändern. Andernfalls wird die Allianz aufgelößt.");
				}
			}
		}
	}

	/**
	 * Gibt zurueck, ob der angegebene Benutzer momentan an einer laufenden Allianzgruendung beteiligt ist.
	 * @param user Der User
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isUserAnAllianzgruendungBeteiligt(@Nonnull User user)
	{
		Taskmanager taskmanager = Taskmanager.getInstance();

		Task[] tasks = taskmanager.getTasksByData(Taskmanager.Types.ALLY_FOUND, "*", "*", "*");
		if (tasks.length > 0)
		{
			for (Task task : tasks)
			{
				int[] users = Common.explodeToInt(",", task.getData3());
				for (int user1 : users)
				{
					if (user1 == user.getId())
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Gibt alle Mitglieder der Allianzfuehrung zurueck, d.h. User mit Posten in der Allianz und der
	 * Allianzpraesident.
	 * @param allianz Die Allianz
	 * @return Die Liste der User
	 */
	public List<User> getAllianzfuehrung(@Nonnull Ally allianz)
	{
		Session db = ContextMap.getContext().getDB();
		List<?> userList = db
				.createQuery("select distinct u from User u join u.ally a " +
						"where a.id= :allyId and (u.allyposten is not null or a.president.id=u.id)")
				.setInteger("allyId", allianz.getId())
				.list();
		return Common.cast(userList);
	}
}
