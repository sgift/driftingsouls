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
package net.driftingsouls.ds2.server.werften;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Testet die Werftimplementierung
 * @author Christopher Jung
 *
 */
public class WerftObjectTest extends DriftingSoulsDBTestCase {
	private WerftObject werft;
	private WerftObject werftKomplex;
	private WerftObject werftKomplexPart;
	
	/**
	 * Laedt die Werft fuer Tests
	 *
	 */
	@Before
	public void loadWerft() {
		org.hibernate.Session db = context.getDB();
		
		this.werft = (WerftObject)db.get(WerftObject.class, 1);
		assertThat(this.werft, not(nullValue()));
		
		this.werftKomplex = (WerftObject)db.get(WerftObject.class, 2);
		assertThat(this.werftKomplex, not(nullValue()));
		
		this.werftKomplexPart = (WerftObject)db.get(WerftObject.class, 3);
		assertThat(this.werftKomplexPart, not(nullValue()));
	}
	
	public IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSet(WerftObjectTest.class.getResourceAsStream("WerftObjectTest.xml"));
	}
	
	/**
	 * Testet die Destroy-Operation
	 */
	@Test
	public void destroyWerft() {
		org.hibernate.Session db = context.getDB();
		
		this.werft.destroy();
		
		WerftObject werftAfterDelete = (WerftObject)db.get(WerftObject.class, 1);
		assertThat(werftAfterDelete, nullValue());
		
		WerftQueueEntry entry = (WerftQueueEntry)db.get(WerftQueueEntry.class, 1);
		assertThat(entry, nullValue());
	}
	
	/**
	 * Testet die Destroy-Operation bei Werftkomplexen
	 */
	@Test
	public void destroyWerftKomplex() {
		org.hibernate.Session db = context.getDB();
		
		this.werftKomplex.destroy();
		
		WerftObject werftAfterDelete = (WerftObject)db.get(WerftObject.class, 2);
		assertThat(werftAfterDelete, nullValue());
		
		WerftQueueEntry entry = (WerftQueueEntry)db.get(WerftQueueEntry.class, 2);
		assertThat(entry, nullValue());
		
		entry = (WerftQueueEntry)db.get(WerftQueueEntry.class, 3);
		assertThat(entry, nullValue());
	}
	
	/**
	 * Testet die Destroy-Operation bei Werftkomplexen ausgehend von einem Teil
	 */
	@Test
	public void destroyWerftKomplexPart() {
		org.hibernate.Session db = context.getDB();
		
		this.werftKomplexPart.destroy();
		
		WerftObject werftAfterDelete = (WerftObject)db.get(WerftObject.class, 3);
		assertThat(werftAfterDelete, nullValue());
		
		WerftQueueEntry entry = (WerftQueueEntry)db.get(WerftQueueEntry.class, 2);
		assertThat(entry, not(nullValue()));
		
		entry = (WerftQueueEntry)db.get(WerftQueueEntry.class, 3);
		assertThat(entry, not(nullValue()));
	}
	
	/**
	 * Testet {@link WerftObject#getBuildQueue()}
	 */
	@Test
	public void getBuildQuery() {
		WerftQueueEntry[] entries = this.werft.getBuildQueue();
		assertThat(entries, not(nullValue()));
		assertThat(entries.length, is(1));
		assertThat(entries[0].getBuildShipType().getTypeId(), is(1));
		
		entries = this.werftKomplex.getBuildQueue();
		assertThat(entries, not(nullValue()));
		assertThat(entries.length, is(2));
		assertThat(entries[0].getBuildShipType().getTypeId(), is(1));
		assertThat(entries[1].getBuildShipType().getTypeId(), is(1));
		
		entries = this.werftKomplexPart.getBuildQueue();
		assertThat(entries, not(nullValue()));
		assertThat(entries.length, is(0));
	}
	
	/**
	 * Testet {@link WerftObject#getKomplex()}
	 */
	@Test
	public void getKomplex() {
		assertThat(this.werft.getKomplex(), nullValue());
		assertThat(this.werftKomplex.getKomplex(), nullValue());
		assertThat(this.werftKomplexPart.getKomplex(), is(this.werftKomplex));
	}
	
	/**
	 * Testet {@link WerftObject#getBuildQueueEntry(int)}
	 */
	@Test
	public void getBuildQueueEntry() {
		WerftQueueEntry entry = this.werft.getBuildQueueEntry(0);
		assertThat(entry, not(nullValue()));
		assertThat(entry.getBuildShipType().getTypeId(), is(1));
		
		entry = this.werft.getBuildQueueEntry(1);
		assertThat(entry, nullValue());
		
		entry = this.werftKomplex.getBuildQueueEntry(1);
		assertThat(entry, not(nullValue()));
		assertThat(entry.getBuildShipType().getTypeId(), is(1));
		
		entry = this.werftKomplexPart.getBuildQueueEntry(1);
		assertThat(entry, nullValue());
	}
}
