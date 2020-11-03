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
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.services.UserService;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * TASK_ALLY_FOUND
 * 		Einer Allianz gruenden.
 * 
 * 	- data1 -> der Name der Allianz
 *  - data2 -> die Anzahl der noch fehlenden Unterstuetzungen (vgl. TASK_ALLY_FOUND_CONFIRM)
 *  - data3 -> die Spieler, die in die neu gegruendete Allianz sollen, jeweils durch ein , getrennt (Pos: 0 -> Praesident/Gruender)  
 *  @author Christopher Jung
 */
@Service
public class HandleAllyFound implements TaskHandler {

	@PersistenceContext
	private EntityManager em;

	private final UserService userService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final TaskManager taskManager;

	public HandleAllyFound(UserService userService, PmService pmService, BBCodeParser bbCodeParser, TaskManager taskManager) {
		this.userService = userService;
		this.pmService = pmService;
		this.bbCodeParser = bbCodeParser;
		this.taskManager = taskManager;
	}

	@Override
	public void handleEvent(Task task, String event) {	
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		switch (event)
		{
			case "__conf_recv":
				int confcount = Integer.parseInt(task.getData1());
				if (confcount == 1)
				{
					String allyname = task.getData2();

					Integer[] allymemberIds = Common.explodeToInteger(",", task.getData3());
					User[] allymember = new User[allymemberIds.length];
					for (int i = 0; i < allymemberIds.length; i++)
					{
						allymember[i] = em.find(User.class, allymemberIds[i]);
					}

					int ticks = new ConfigService().getValue(WellKnownConfigValue.TICKS);

					var plainname = Common._titleNoFormat(bbCodeParser, allyname);
					Ally ally = new Ally(allyname, plainname, allymember[0], ticks);
					int allyid = (Integer) db.save(ally);

					Common.copyFile(Configuration.getAbsolutePath() + "data/logos/ally/0.gif", Configuration.getAbsolutePath() + "data/logos/ally" + allyid + ".gif");

					for (User anAllymember : allymember)
					{
						User source = em.find(User.class, 0);

						pmService.send(source, anAllymember.getId(), "Allianzgr&uuml;ndung", "Die Allianz " + allyname + " wurde erfolgreich gegr&uuml;ndet.\n\nHerzlichen Gl&uuml;ckwunsch!");

						ally.addUser(anAllymember);
						anAllymember.setAllyPosten(null);
						anAllymember.addHistory(Common.getIngameTime(ticks) + ": Gr&uuml;ndung der Allianz " + allyname);

						// Beziehungen auf "Freund" setzen
						for (User anAllymember1 : allymember)
						{
							if (anAllymember1 == anAllymember)
							{
								continue;
							}

							userService.setRelation(anAllymember1, anAllymember, User.Relation.FRIEND);
							userService.setRelation(anAllymember, anAllymember1, User.Relation.FRIEND);
						}
					}

					taskManager.removeTask(task.getTaskID());
				}
				else
				{
					confcount--;
					taskManager.modifyTask(task.getTaskID(), Integer.toString(confcount), task.getData2(), task.getData3());
				}
				break;
			case "__conf_dism":
			{
				Integer[] allymember = Common.explodeToInteger(",", task.getData3());
				User source = em.find(User.class, 0);

				pmService.send(source, allymember[0], "Allianzgr&uuml;ndung", "Die Allianzgr&uuml;ndung ist fehlgeschlagen, da ein Spieler seine Unterst&uuml;tzung verweigert hat.");
				taskManager.removeTask(task.getTaskID());

				Task[] tasklist = taskManager.getTasksByData(TaskManager.Types.ALLY_FOUND_CONFIRM, task.getTaskID(), "*", "*");
				for (Task aTasklist : tasklist)
				{
					taskManager.removeTask(aTasklist.getTaskID());
				}

				break;
			}
			case "tick_timeout":
			{
				Integer[] allymember = Common.explodeToInteger(",", task.getData3());

				taskManager.removeTask(task.getTaskID());
				User source = em.find(User.class, 0);

				for (Integer anAllymember : allymember)
				{
					pmService.send(source, anAllymember, "Allianzgr&uuml;ndung", "Die Allianzgr&uuml;ndung ist fehlgeschlagen, da nicht alle angegebenen Spieler in der notwendigen Zeit ihre Unterst&uuml;tzung signalisiert haben.");
				}
				break;
			}
		}
	}

}
