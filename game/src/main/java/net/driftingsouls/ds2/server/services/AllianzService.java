package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.entities.ComNetChannel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserRank;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.entities.ally.AllyRangDescriptor;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Service fuer Allianzen.
 */
@Service
public class AllianzService
{
	@PersistenceContext
	private EntityManager em;

	private final ConfigService configService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final MedalService medalService;
	private final TaskManager taskManager;

	public AllianzService(ConfigService configService, PmService pmService, BBCodeParser bbCodeParser, MedalService medalService, TaskManager taskManager) {
		this.configService = configService;
		this.pmService = pmService;
		this.bbCodeParser = bbCodeParser;
		this.medalService = medalService;
		this.taskManager = taskManager;
	}

	public void loeschen(@NonNull Ally allianz)
	{
		List<ComNetChannel> chnList = em.createQuery("from ComNetChannel where allyOwner=:owner", ComNetChannel.class)
				.setParameter("owner", allianz)
				.getResultList();
		for (ComNetChannel channel: chnList)
		{
			em.createQuery("delete from ComNetVisit where channel=:channel")
					.setParameter("channel", channel)
					.executeUpdate();

			em.createQuery("delete from ComNetEntry where channel=:channel")
					.setParameter("channel", channel)
					.executeUpdate();

			em.remove(channel);
		}

		int tick = configService.getValue(WellKnownConfigValue.TICKS);

		List<User> uids = em.createQuery("from User where ally=:ally", User.class)
				.setParameter("ally", allianz)
				.getResultList();
		for (User auser: uids)
		{
			auser.addHistory(Common.getIngameTime(tick) + ": Verlassen der Allianz " + allianz.getName() + " im Zuge der Aufl&ouml;sung dieser Allianz");
			auser.setAlly(null);
			if (auser.getAllyPosten() != null)
			{
				AllyPosten posten = auser.getAllyPosten();
				auser.setAllyPosten(null);
				em.remove(posten);
			}
			auser.setName(auser.getNickname());
			auser.setPlainname(bbCodeParser.parse(auser.getNickname(), new String[] {"all"}));
		}

		em.createQuery("delete from AllyPosten where ally=:ally")
				.setParameter("ally", allianz)
				.executeUpdate();

		// Delete Ally from running Battles

		var battleQuery = em.createQuery("from Battle " +
				"where ally1 = :ally or ally2 = :ally", Battle.class)
				.setParameter("ally", allianz.getId());

		for (Battle battle : battleQuery.getResultList())
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
		em.remove(allianz);
	}

	/**
	 * Entfernt einen Spieler aus der Allianz.
	 *
	 * @param allianz Die Allianz
	 * @param mitglied Der Spieler
	 */
	public void entferneMitglied(@NonNull Ally allianz, User mitglied)
	{

		allianz.removeMember(mitglied);
		mitglied.setAlly(null);
		mitglied.setAllyPosten(null);
		mitglied.setName(mitglied.getNickname());
		mitglied.setPlainname(bbCodeParser.parse(mitglied.getNickname(), new String[] {"all"}));

		em.createQuery("update Battle set ally1=0 where commander1= :user and ally1= :ally")
				.setParameter("user", mitglied)
				.setParameter("ally", allianz.getId())
				.executeUpdate();

		em.createQuery("update Battle set ally2=0 where commander2= :user and ally2= :ally")
				.setParameter("user", mitglied)
				.setParameter("ally", allianz.getId())
				.executeUpdate();

		int tick = configService.getValue(WellKnownConfigValue.TICKS);
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
	public void pruefeAufZuWenigMitglieder(@NonNull Ally allianz)
	{
		// Ist der Praesident kein NPC (negative ID) ?
		if (allianz.getPresident().getId() > 0)
		{
			long count = allianz.getMemberCount();
			if (count < 3)
			{
				taskManager.addTask(TaskManager.Types.ALLY_LOW_MEMBER, 21, Integer.toString(allianz.getId()), "", "");

				final User nullUser = em.find(User.class, 0);

				List<User> supermembers = getAllianzfuehrung(allianz);
				for (User supermember : supermembers)
				{
					pmService.send(nullUser, supermember.getId(), "Drohende Allianzauflösung",
							"[Automatische Nachricht]\n" +
									"Achtung!\n" +
									"Durch den jüngsten Weggang eines Allianzmitglieds hat deine Allianz zu wenig Mitglieder, um weiterhin zu bestehen. " +
									"Du hast nun 21 Ticks Zeit, diesen Zustand zu ändern. Andernfalls wird die Allianz aufgelöst.");
				}
			}
		}
	}

	/**
	 * Gibt zurueck, ob der angegebene Benutzer momentan an einer laufenden Allianzgruendung beteiligt ist.
	 * @param user Der User
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isUserAnAllianzgruendungBeteiligt(@NonNull User user)
	{
		Task[] tasks = taskManager.getTasksByData(TaskManager.Types.ALLY_FOUND, "*", "*", "*");
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
	public List<User> getAllianzfuehrung(@NonNull Ally allianz)
	{
		return em.createQuery("select distinct u from User u join u.ally a " +
						"where a.id= :allyId and (u.allyposten is not null or a.president.id=u.id)", User.class)
				.setParameter("allyId", allianz.getId())
				.getResultList();
	}

	/**
	 * Gibt die Liste aller bekannten Raenge dieser Allianz zurueck. Dies umfasst sowohl
	 * die spezifischen Raenge dieser Allianz als auch alle allgemeinen Raenge ({@link MedalService#raenge()}).
	 * @return Die nach Rangnummer sortierte Liste der Rangbezeichnungen
	 */
	public SortedSet<Rang> getFullRangNameList(Ally ally)
	{
		SortedSet<Rang> result = new TreeSet<>();
		for( AllyRangDescriptor rang : ally.getRangDescriptors() )
		{
			result.add(new Rang(rang.getRang(), rang.getName(), getAllyRangDescriptorImage(rang)));
		}

		result.addAll(medalService.raenge().values());

		return result;
	}

	/**
	 * Gibt das Bild (Pfad) des Ranges zurueck.
	 * @return Der Bildpfad
	 */
	public String getAllyRangDescriptorImage(AllyRangDescriptor allyRangDescriptor)
	{
		if( allyRangDescriptor.getCustomImg() != null )
		{
			return "data/dynamicContent/"+allyRangDescriptor.getCustomImg();
		}
		return medalService.rang(allyRangDescriptor.getRang()).getImage();
	}

	/**
	 * Gibt den Anzeigenamen fuer die angegebene Rangnummer zurueck.
	 * Sofern die Allianz ueber eine eigene Bezeichnung verfuegt wird diese zurueckgegeben.
	 * Andernfalls wird die globale Bezeichnung verwendet.
	 * @param rangNr Die Rangnummer
	 * @return Der Anzeigename
	 */
	public String getRangName(Ally ally, int rangNr)
	{
		for( AllyRangDescriptor rang : ally.getRangDescriptors() )
		{
			if( rang.getRang() == rangNr )
			{
				return rang.getName();
			}
		}

		return medalService.rang(rangNr).getName();
	}

	/**
	 * Gibt den Anzeigenamen des Rangs zurueck.
	 *
	 * @return Der Anzeigename
	 */
	public String getRankName(UserRank rank)
	{
		String rangName = medalService.rang(rank.getRank()).getName();
		if( rank.getUserRankKey().getRankGiver().getAlly() != null )
		{
			rangName = getRangName(rank.getUserRankKey().getRankGiver().getAlly(), rank.getRank());
		}
		return rangName;
	}
}
