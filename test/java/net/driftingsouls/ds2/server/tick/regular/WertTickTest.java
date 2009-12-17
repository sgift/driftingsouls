/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.tick.regular;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Test;

/**
 * Testet den Werfttick
 * @author Christopher Jung
 *
 */
public class WertTickTest extends DriftingSoulsDBTestCase
{	
	public IDataSet getDataSet() throws Exception
	{
		return new FlatXmlDataSet(WertTickTest.class.getResourceAsStream("WerftTickTest.xml"));
	}
	
	/**
	 * Testet die Tickausfuehrung
	 */
	@Test
	public void executeTick()
	{
		org.hibernate.Session sess = context.getDB();
		
		long count = (Long)sess.createQuery("select count(*) from Ship").iterate().next();
		assertThat(count, is(4L));
		
		count = (Long)sess.createQuery("select count(*) from WerftQueueEntry").iterate().next();
		assertThat(count, is(4L));
		
		count = (Long)sess.createQuery("select count(*) from User").iterate().next();
		assertThat(count, is(3L));
		
		count = (Long)sess.createQuery("select count(*) from WerftObject").iterate().next();
		assertThat(count, is(6L));
		
		count = (Long)sess.createQuery("select count(*) from Base").iterate().next();
		assertThat(count, is(1L));
		
		new WerftTick().execute();
		
		count = (Long)sess.createQuery("select count(*) from WerftQueueEntry").iterate().next();
		assertThat(count, is(0L));
		
		count = (Long)sess.createQuery("select count(*) from Ship").iterate().next();
		assertThat(count, is(8L));
	}
}
