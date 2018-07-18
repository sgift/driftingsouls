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

import net.driftingsouls.ds2.server.DBSingleTransactionTest;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Testet die Werftimplementierung
 * @author Christopher Jung
 *
 */
public class WerftObjectTest extends DBSingleTransactionTest
{
	private ShipType werftType;
	private BaseWerft werft;
	private WerftKomplex werftKomplex;
	private ShipWerft werftKomplexPart;
	private ShipWerft werftKomplexPart2;
	private ShipWerft werftKomplexPart3;
	private WerftQueueEntry entry1;
	private WerftQueueEntry entry2;
	private WerftQueueEntry entry3;
	private WerftQueueEntry entry4;

	/**
	 * Laedt die Werft fuer Tests
	 */
	@Before
	public void loadWerft()
	{
		User user = persist(new User("testUser1", "***", 0, "", new Cargo(), "test@localhost"));
		werftType = persist(new ShipType(ShipClasses.STATION));
		werftType.setFlags(ShipTypeFlag.WERFTKOMPLEX.getFlag());
		werftType.setWerft(8);

		BaseType baseType = persist(new BaseType("TestKlasse"));
		Base base = persist(new Base(new Location(1, 1, 1), user, baseType));

		this.werft = persist(new BaseWerft(base));
		this.entry1 = persist(new WerftQueueEntry(werft, werftType, 1, 1));
		this.werft.addQueueEntry(this.entry1);
		this.entry2 = persist(new WerftQueueEntry(werft, werftType, 1, 1));
		this.werft.addQueueEntry(this.entry2);

		Ship ship1 = persist(new Ship(user, werftType, 1, 1, 1));
		Ship ship2 = persist(new Ship(user, werftType, 1, 1, 1));
		Ship ship3 = persist(new Ship(user, werftType, 1, 1, 1));

		this.werftKomplex = persist(new WerftKomplex());

		ShipWerft sWerft1 = persist(new ShipWerft(ship1));
		sWerft1.addToKomplex(werftKomplex);
		ShipWerft sWerft2 = persist(new ShipWerft(ship2));
		sWerft2.addToKomplex(werftKomplex);
		ShipWerft sWerft3 = persist(new ShipWerft(ship3));
		sWerft3.addToKomplex(werftKomplex);

		this.werftKomplexPart = sWerft1;
		this.werftKomplexPart2 = sWerft2;
		this.werftKomplexPart3 = sWerft3;

		this.entry3 = persist(new WerftQueueEntry(werftKomplex, werftType, 1, 1));
		this.werftKomplex.addQueueEntry(this.entry3);
		this.entry4 = persist(new WerftQueueEntry(werftKomplex, werftType, 1, 1));
		this.werftKomplex.addQueueEntry(this.entry4);
	}


	/**
	 * Testet die Destroy-Operation
	 */
	@Test
	public void gegebenEineWerftMitZweiZuBauendenSchiffen_destroy_sollteDieWerftUndBeideZuBauendenSchiffeEntfernen()
	{
		// setup

		// run
		this.werft.destroy();

		// assert
		assertThat(getEM().contains(this.werft), is(false));
		assertThat(getEM().contains(this.entry1), is(false));
		assertThat(getEM().contains(this.entry2), is(false));
	}

	/**
	 * Testet die Destroy-Operation bei Werftkomplexen
	 */
	@Test
	public void gegebenEinWerftKomplexMitZweiZuBauendenSchiffen_destroy_sollteDenKomplexAufloesenUndAlleBauauftraegeVerwerfen()
	{
		// setup
		WerftObject[] members = this.werftKomplex.getMembers();

		// run
		this.werftKomplex.destroy();

		// assert
		assertThat(getEM().contains(this.werftKomplex), is(false));
		assertThat(this.werftKomplex.getBuildQueue().contains(this.entry3), is(false));
		assertThat(this.werftKomplex.getBuildQueue().contains(this.entry4), is(false));
		assertThat(members[0].getBuildQueue().size(), is(0));
		assertThat(members[1].getBuildQueue().size(), is(0));
		assertThat(members[2].getBuildQueue().size(), is(0));
	}

	@Test
	public void gegebenEinWerftKomplexAusDreiWerftenMitZweiZuBauendenSchiffenUndEineDarausZuLoeschendeWerft_destroy_sollteDieseEineWerftEntfernenUndDenKomplexBestehenLassen()
	{
		// setup

		// run
		this.werftKomplexPart.destroy();

		// assert
		assertThat(getEM().contains(this.werftKomplexPart), is(false));
		assertThat(this.werftKomplex.getBuildQueue().size(), is(2));
		assertThat(getEM().contains(this.entry3), is(true));
		assertThat(getEM().contains(this.entry4), is(true));
	}

	@Test
	public void gegebenEinWerftKomplexAusZweiWerftenMitZweiZuBauendenSchiffenUndEineDarausZuLoeschendeWerft_destroy_sollteDieseEineWerftEntfernenUndDenKomplexBestehenLassen()
	{
		// setup
		this.werftKomplexPart.destroy();
		assertThat(this.werftKomplex.getBuildQueue().size(), is(2));

		// run
		this.werftKomplexPart2.destroy();

		// assert
		assertThat(getEM().contains(this.werftKomplexPart2), is(false));
		assertThat(getEM().contains(this.werftKomplex), is(false));
		assertThat(this.werftKomplex.getBuildQueue().size(), is(0));
		assertThat(this.werftKomplexPart2.getBuildQueue().size(), is(0));
		assertThat(this.werftKomplexPart3.getBuildQueue().size(), is(2));
	}

	@Test
	public void gegebenEineWerftMitZweiBauauftraegen_getBuildQueue_sollteDieseSortiertNachPositionZurueckgeben()
	{
		// setup
		this.werft.getBuildQueue().forEach(qe -> qe.setPosition(3 - qe.getPosition()));

		// run
		List<WerftQueueEntry> entries = this.werft.getBuildQueue();

		// assert
		assertThat(entries, not(nullValue()));
		assertThat(entries.size(), is(2));
		assertThat(entries.get(0).getPosition(), is(1));
		assertThat(entries.get(1).getPosition(), is(2));
	}

	@Test
	public void gegebenEineWerftOhneBauauftraege_getBuildQueue_sollteEineLeereListeZurueckgeben()
	{
		// setup
		// run
		List<WerftQueueEntry> entries = this.werftKomplexPart.getBuildQueue();

		// assert
		assertThat(entries, not(nullValue()));
		assertThat(entries.size(), is(0));
	}

	@Test
	public void gegebenEineWerftAusEinemWerftKomplex_getKomplex_sollteDiesenKomplexZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werftKomplexPart.getKomplex();
		// assert
		assertThat(komplex, is(this.werftKomplex));
	}

	@Test
	public void gegebenEineBasisWerft_getKomplex_sollteNullZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werft.getKomplex();
		// assert
		assertThat(komplex, nullValue());
	}

	@Test
	public void gegebenEinWerftKomplex_getKomplex_sollteNullZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werftKomplex.getKomplex();
		// assert
		assertThat(komplex, nullValue());
	}

	@SuppressWarnings("ConstantConditions")
	@Test
	public void gegebenEineWerftMitZuBauendenSchiffenUndEineGueltigePosition_getBuildQueueEntry_sollteDasPassendeZuBauendeSchiffZurueckgeben()
	{
		// setup

		// run
		WerftQueueEntry entry = this.werft.getBuildQueueEntry(1);

		// assert
		assertThat(entry, not(nullValue()));
		assertThat(entry.getBuildShipType(), is(werftType));
	}

	@Test
	public void gegebenEineWerftMitZuBauendenSchiffenUndEineUngueltigePosition_getBuildQueueEntry_sollteNullZurueckgeben()
	{
		// setup

		// run
		WerftQueueEntry entry = this.werft.getBuildQueueEntry(3);

		// assert
		assertThat(entry, nullValue());
	}
}
