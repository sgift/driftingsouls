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

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.TestAppConfig;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.BaseType;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.services.ShipyardService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Testet die Werftimplementierung
 * @author Christopher Jung
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
	TestAppConfig.class
})
public class WerftObjectTest
{
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ConfigService configService;
	@Autowired
	private ShipyardService shipyardService;

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
		User user = new User(1, "testUser1", "testUser1", "***", 0, "", "test@localhost", configService);
		em.persist(user);
		werftType = new ShipType(ShipClasses.STATION);
		em.persist(werftType);
		werftType.setFlags(ShipTypeFlag.WERFTKOMPLEX.getFlag());
		werftType.setWerft(8);

		BaseType baseType = new BaseType("TestKlasse");
		em.persist(baseType);
		Base base = new Base(new Location(1, 1, 1), user, baseType);
		em.persist(base);

		this.werft = new BaseWerft(base);
		em.persist(werft);
		this.entry1 = new WerftQueueEntry(werft, werftType, 1, 1, shipyardService.getNextEmptyQueuePosition(werft));
		em.persist(entry1);
		this.werft.addQueueEntry(this.entry1);
		this.entry2 = new WerftQueueEntry(werft, werftType, 1, 1, shipyardService.getNextEmptyQueuePosition(werft));
		em.persist(entry2);
		this.werft.addQueueEntry(this.entry2);

		Ship ship1 = new Ship(user, werftType, 1, 1, 1);
		em.persist(ship1);
		Ship ship2 = new Ship(user, werftType, 1, 1, 1);
		em.persist(ship2);
		Ship ship3 = new Ship(user, werftType, 1, 1, 1);
		em.persist(ship3);

		this.werftKomplex = new WerftKomplex();
		em.persist(werftKomplex);

		ShipWerft sWerft1 = new ShipWerft(ship1);
		em.persist(sWerft1);
		sWerft1.addToKomplex(werftKomplex);
		ShipWerft sWerft2 = new ShipWerft(ship2);
		em.persist(sWerft2);
		sWerft2.addToKomplex(werftKomplex);
		ShipWerft sWerft3 = new ShipWerft(ship3);
		em.persist(sWerft3);
		sWerft3.addToKomplex(werftKomplex);

		this.werftKomplexPart = sWerft1;
		this.werftKomplexPart2 = sWerft2;
		this.werftKomplexPart3 = sWerft3;

		this.entry3 = new WerftQueueEntry(werftKomplex, werftType, 1, 1, shipyardService.getNextEmptyQueuePosition(werftKomplex));
		em.persist(entry3);
		this.werftKomplex.addQueueEntry(this.entry3);
		this.entry4 = new WerftQueueEntry(werftKomplex, werftType, 1, 1, shipyardService.getNextEmptyQueuePosition(werftKomplex));
		em.persist(entry4);
		this.werftKomplex.addQueueEntry(this.entry4);
	}


	/**
	 * Testet die Destroy-Operation
	 */
	@Test
	@Transactional
	public void gegebenEineWerftMitZweiZuBauendenSchiffen_destroy_sollteDieWerftUndBeideZuBauendenSchiffeEntfernen()
	{
		// setup

		// run
		shipyardService.destroyShipyard(this.werft);

		// assert
		assertThat(em.contains(this.werft), is(false));
		assertThat(em.contains(this.entry1), is(false));
		assertThat(em.contains(this.entry2), is(false));
	}

	/**
	 * Testet die Destroy-Operation bei Werftkomplexen
	 */
	@Test
	@Transactional
	public void gegebenEinWerftKomplexMitZweiZuBauendenSchiffen_destroy_sollteDenKomplexAufloesenUndAlleBauauftraegeVerwerfen()
	{
		// setup
		List<WerftObject> members = this.werftKomplex.getMembers();

		// run
		shipyardService.destroyShipyard(this.werftKomplex);

		// assert
		assertThat(em.contains(this.werftKomplex), is(false));
		assertThat(this.werftKomplex.getBuildQueue().contains(this.entry3), is(false));
		assertThat(this.werftKomplex.getBuildQueue().contains(this.entry4), is(false));
		assertThat(members.get(0).getBuildQueue().size(), is(0));
		assertThat(members.get(1).getBuildQueue().size(), is(0));
		assertThat(members.get(2).getBuildQueue().size(), is(0));
	}

	@Test
	@Transactional
	public void gegebenEinWerftKomplexAusDreiWerftenMitZweiZuBauendenSchiffenUndEineDarausZuLoeschendeWerft_destroy_sollteDieseEineWerftEntfernenUndDenKomplexBestehenLassen()
	{
		// setup

		// run
		shipyardService.destroyShipyard(this.werftKomplexPart);

		// assert
		assertThat(em.contains(this.werftKomplexPart), is(false));
		assertThat(this.werftKomplex.getBuildQueue().size(), is(2));
		assertThat(em.contains(this.entry3), is(true));
		assertThat(em.contains(this.entry4), is(true));
	}

	@Test
	@Transactional
	public void gegebenEinWerftKomplexAusZweiWerftenMitZweiZuBauendenSchiffenUndEineDarausZuLoeschendeWerft_destroy_sollteDieseEineWerftEntfernenUndDenKomplexBestehenLassen()
	{
		// setup
		shipyardService.destroyShipyard(this.werftKomplexPart);
		assertThat(this.werftKomplex.getBuildQueue().size(), is(2));

		// run
		shipyardService.destroyShipyard(this.werftKomplexPart2);

		// assert
		assertThat(em.contains(this.werftKomplexPart2), is(false));
		assertThat(em.contains(this.werftKomplex), is(false));
		assertThat(this.werftKomplex.getBuildQueue().size(), is(0));
		assertThat(this.werftKomplexPart2.getBuildQueue().size(), is(0));
		assertThat(this.werftKomplexPart3.getBuildQueue().size(), is(2));
	}

	@Test
	@Transactional
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
	@Transactional
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
	@Transactional
	public void gegebenEineWerftAusEinemWerftKomplex_getKomplex_sollteDiesenKomplexZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werftKomplexPart.getKomplex();
		// assert
		assertThat(komplex, is(this.werftKomplex));
	}

	@Test
	@Transactional
	public void gegebenEineBasisWerft_getKomplex_sollteNullZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werft.getKomplex();
		// assert
		assertThat(komplex, nullValue());
	}

	@Test
	@Transactional
	public void gegebenEinWerftKomplex_getKomplex_sollteNullZurueckgeben()
	{
		// setup
		// run
		WerftKomplex komplex = this.werftKomplex.getKomplex();
		// assert
		assertThat(komplex, nullValue());
	}

	@Test
	@Transactional
	public void gegebenEineWerftMitZuBauendenSchiffenUndEineGueltigePosition_getBuildQueueEntry_sollteDasPassendeZuBauendeSchiffZurueckgeben()
	{
		// setup

		// run
		WerftQueueEntry entry = shipyardService.getBuildQueueEntry(this.werft, 1);

		// assert
		assertThat(entry, not(nullValue()));
		assertThat(entry.getBuildShipType(), is(werftType));
	}

	@Test
	@Transactional
	public void gegebenEineWerftMitZuBauendenSchiffenUndEineUngueltigePosition_getBuildQueueEntry_sollteNullZurueckgeben()
	{
		// setup

		// run
		WerftQueueEntry entry = shipyardService.getBuildQueueEntry(this.werft, 3);

		// assert
		assertThat(entry, nullValue());
	}
}
