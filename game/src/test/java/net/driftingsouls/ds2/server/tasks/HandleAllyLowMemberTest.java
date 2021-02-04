/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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

import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Testcase fuer HandleAllyLowMember
 * @author Christopher Jung
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
@WebAppConfiguration
public class HandleAllyLowMemberTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private BBCodeParser bbCodeParser;
	@Autowired
	private TaskManager taskManager;

	private User sender;
	private Ally ally;
	private AllyPosten posten;
	private User user1;
	private User user2;
	private Task task;

	@Before
	public void setUp() {
		this.sender = new User(1, "senderUser", "senderUser", "***", 0, "", "testSender@localhost", configService);
		em.persist(sender);
		ConfigValue configValue = configService.get(WellKnownConfigValue.ALLIANZAUFLOESUNG_PM_SENDER);
		configValue.setValue(String.valueOf(sender.getId()));

		this.user1 = new User(2, "testUser1", "testUser1", "***", 0, "", "test1@localhost", configService);
		em.persist(user1);
		var plainname = Common._titleNoFormat(bbCodeParser, "TestAlly");
		this.ally = new Ally("TestAlly", plainname, this.user1, 1);
		em.persist(ally);
		this.ally.addUser(user1);

		this.posten = new AllyPosten(this.ally, "Testposten");
		em.persist(posten);

		this.user2 = new User(3, "testUser2", "testUser2", "***", 0, "", "test2@localhost", configService);
		em.persist(user2);
		ally.addUser(user2);
		this.user2.setAllyPosten(posten);

		this.task = new Task(TaskManager.Types.ALLY_LOW_MEMBER);
		em.persist(task);
		this.task.setData1(String.valueOf(this.ally.getId()));
	}

	/**
	 * Testet die Task
     */
	@Test
	@Transactional
	public void gegebenEineAllianzUndEineAllyLowMemberTask_handleTask_sollteDieAllianzAufloesen() {
		// setup

		// run
		taskManager.handleTask(task.getTaskID(), "tick_timeout");

		// assert
		assertThat(em.contains(ally), is(false));
		assertThat(em.contains(posten), is(false));

		for( User user : Arrays.asList(user1, user2) )
		{
			assertThat(user, not(nullValue()));
			assertThat(user.getAlly(), nullValue());
			assertThat(user.getAllyPosten(), nullValue());
		}

		List<PM> pms = em.createQuery("from PM", PM.class).getResultList();
		assertThat(pms.size(), is(2));
		for (PM pm : pms)
		{
			assertThat(pm.getTitle(), is("Allianzauflösung"));
			assertThat(pm.getSender(), is(this.sender));
		}

	}
}
