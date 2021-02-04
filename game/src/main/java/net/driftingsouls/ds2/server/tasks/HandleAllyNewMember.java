/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tasks;

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.UserService;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * TASK_ALLY_NEW_MEMBER
 * 		Einer Allianz beitreten (Aufnahmeantrag).
 *
 * 	- data1 -> die ID der Allianz
 *  - data2 -> die ID des Spielers, der den Antrag gestellt hat
 *  - data3 -> unbenutzt
 *
 *  @author Christopher Jung
 */
@Service
public class HandleAllyNewMember implements TaskHandler {
	@PersistenceContext
	private EntityManager em;

	private final UserService userService;
	private final PmService pmService;
	private final ConfigService configService;
	private final TaskManager taskManager;

	public HandleAllyNewMember(UserService userService, PmService pmService, ConfigService configService, TaskManager taskManager) {
		this.userService = userService;
		this.pmService = pmService;
		this.configService = configService;
		this.taskManager = taskManager;
	}

	@Override
	public void handleEvent(Task task, String event) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();

		int playerID = Integer.parseInt(task.getData2());

		switch (event)
		{
			case "pm_yes":
				Ally ally = em.find(Ally.class, Integer.valueOf(task.getData1()));

				User player = em.find(User.class, playerID);
				String newname = ally.getAllyTag();
				newname = newname.replace("[name]", player.getNickname());
				ally.addUser(player);
				player.setName(newname);

				int tick = configService.getValue(WellKnownConfigValue.TICKS);
				player.addHistory(Common.getIngameTime(tick) + ": Beitritt zur Allianz " + ally.getName());

				int membercount = 1;

				// Beziehungen auf "Freund" setzen
				List<User> members = ally.getMembers();
				for (User allymember : members)
				{
					if (allymember.getId() == player.getId())
					{
						continue;
					}
					userService.setRelation(allymember, player, User.Relation.FRIEND);
					userService.setRelation(player, allymember, User.Relation.FRIEND);

					membercount++;
				}

				pmService.send(user, player.getId(), "Aufnahmeantrag", "[Automatische Nachricht]\nDu wurdest in die Allianz >" + ally.getName() + "< aufgenommen\n\nHerzlichen Gr&uuml;ckwunsch!");

				// Check, ob wir eine TM_TASK_LOW_MEMBER entfernen muessen
				if (membercount == 2)
				{
					Task[] tasks = taskManager.getTasksByData(TaskManager.Types.ALLY_LOW_MEMBER, Integer.toString(ally.getId()), "*", "*");
					for (Task task1 : tasks)
					{
						taskManager.removeTask(task1.getTaskID());
					}
				}
				break;
			case "pm_no":
			{
				User source = em.find(User.class, 0);
				pmService.send(source, playerID, "Aufnahmeantrag", "[Automatische Nachricht]\nDein Antrag wurde leider abgelehnt. Es steht dir nun frei ob du einen neuen Antrag nach absprache mit der Allianz stellen willst oder ob du dich an eine andere Allianz wendest.");
				break;
			}
			case "tick_timeout":
			{
				User source = em.find(User.class, 0);
				pmService.send(source, playerID, "Aufnahmeantrag", "[Automatische Nachricht]\nDein Antrag wurde leider nicht innerhalb der vorgegebenen Zeit bearbeitet und daher entfernt. Du hast jedoch jederzeit die M&ouml;glichkeit einen neuen Antrag zu stellen.");
				break;
			}
		}

		taskManager.removeTask( task.getTaskID() );
	}

}
