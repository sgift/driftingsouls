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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.AllyPosten;
import net.driftingsouls.ds2.server.entities.User;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Testcase fuer HandleAllyLowMember
 * @author Christopher Jung
 *
 */
public class HandleAllyLowMemberTest extends DriftingSoulsDBTestCase {
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		// Workaround zyklische Abhaengigkeit user <-> ally
		org.hibernate.Session db = this.context.getDB();
		User user = (User)db.get(User.class, 1);
		user.setAlly((Ally)db.get(Ally.class, 1));
		db.flush();
	}
	
	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(HandleAllyLowMemberTest.class.getResourceAsStream("HandleAllyLowMemberTest.xml"));
	}

	/**
	 * Testet die Task
	 * @throws Exception 
	 */
	@Test
	public void taskTickTimeout() throws Exception {
		org.hibernate.Session db = this.context.getDB();
		
		Task task = Taskmanager.getInstance().getTaskByID("12345");
		assertThat(task.getType(), is(Taskmanager.Types.ALLY_LOW_MEMBER));
		
		Taskmanager.getInstance().handleTask("12345", "tick_timeout");
		Ally ally = (Ally)db.get(Ally.class, 1);
		assertThat(ally, nullValue());
		
		AllyPosten posten = (AllyPosten)db.get(AllyPosten.class, 1);
		assertThat(posten, nullValue());
		
		for( int i=1; i <=3; i++ ) {
			User user = (User)db.get(User.class, i);
			assertThat(user, not(nullValue()));
			assertThat(user.getAlly(), nullValue());
			assertThat(user.getAllyPosten(), nullValue());
		}	
	}
}
