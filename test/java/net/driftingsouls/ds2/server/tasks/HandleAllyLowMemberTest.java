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

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.entities.ally.AllyPosten;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Testcase fuer HandleAllyLowMember
 * @author Christopher Jung
 *
 */
public class HandleAllyLowMemberTest extends DBSingleTransactionTest
{
	private User sender;
	private Ally ally;
	private AllyPosten posten;
	private User user1;
	private User user2;
	private User user3;
	private Task task;

	@Before
	public void setUp() throws Exception {
		this.sender = persist(new User("senderUser", "***", 0, "", new Cargo(), "testSender@localhost"));
		ConfigValue configValue = new ConfigService().get(WellKnownConfigValue.ALLIANZAUFLOESUNG_PM_SENDER);
		configValue.setValue(String.valueOf(sender.getId()));

		this.user1 = persist(new User("testUser1", "***", 0, "", new Cargo(), "test1@localhost"));
		this.ally = persist(new Ally("TestAlly", this.user1));
		this.ally.addUser(user1);

		this.posten = persist(new AllyPosten(this.ally, "Testposten"));

		this.user2 = persist(new User("testUser2", "***", 0, "", new Cargo(), "test2@localhost"));
		ally.addUser(user2);
		this.user3 = persist(new User("testUser3", "***", 0, "", new Cargo(), "test3@localhost"));
		ally.addUser(user3);
		this.user3.setAllyPosten(posten);

		this.task = persist(new Task(Taskmanager.Types.ALLY_LOW_MEMBER));
		this.task.setData1(String.valueOf(this.ally.getId()));
	}

	/**
	 * Testet die Task
	 * @throws Exception 
	 */
	@Test
	public void gegebenEineAllianzUndEineAllyLowMemberTask_handleTask_sollteDieAllianzAufloesen() throws Exception {
		// setup

		// run
		Taskmanager.getInstance().handleTask(task.getTaskID(), "tick_timeout");

		// assert
		assertThat(getEM().contains(ally), is(false));
		assertThat(getEM().contains(posten), is(false));

		for( User user : Arrays.asList(user1, user2, user3) )
		{
			assertThat(user, not(nullValue()));
			assertThat(user.getAlly(), nullValue());
			assertThat(user.getAllyPosten(), nullValue());
		}

		List<PM> pms = Common.cast(getDB().createCriteria(PM.class).list());
		assertThat(pms.size(), is(3));
		for (PM pm : pms)
		{
			assertThat(pm.getTitle(), is("Allianzauflösung"));
			assertThat(pm.getSender(), is(this.sender));
		}

	}
}
