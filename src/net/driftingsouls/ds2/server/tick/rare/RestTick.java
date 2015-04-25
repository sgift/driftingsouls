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
package net.driftingsouls.ds2.server.tick.rare;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.statistik.StatAktiveSpieler;
import net.driftingsouls.ds2.server.entities.statistik.StatCargo;
import net.driftingsouls.ds2.server.entities.statistik.StatItemLocations;
import net.driftingsouls.ds2.server.entities.statistik.StatUserCargo;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.tick.TickController;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 * @author Christopher Jung
 *
 */
@Service("rareRestTick")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestTick extends TickController {
	private int tick;

	@Override
	protected void prepare()
	{
		this.tick = ContextMap.getContext().get(ContextCommon.class).getTick();
	}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getContext().getDB();

		Transaction transaction = db.beginTransaction();
		try
		{
			this.log("Berechne Gesamtcargo:");
			Cargo cargo = new Cargo();
			Map<User,Cargo> usercargos = new HashMap<>();
			Map<User,Map<Integer,Set<String>>> useritemlocations = new HashMap<>();

			ermittleCargoStatistiken(db, cargo, usercargos, useritemlocations);

			StatCargo stat = new StatCargo(this.tick, cargo);
			db.persist(stat);
			transaction.commit();
			transaction = db.beginTransaction();

			this.log("\t"+cargo.save());
			this.log("Speichere User-Cargo-Stats");
			speicherUserCargoStatistik(db, usercargos);

			transaction.commit();
			transaction = db.beginTransaction();

			this.log("Speichere Module-Location-Stats");
			speicherItemLocations(db, useritemlocations);

			transaction.commit();
			transaction = db.beginTransaction();

			this.log("Erfasse Spieleraktivitaet");
			erstelleSpielerStatistik(db);

			transaction.commit();
		}
		catch(Exception e)
		{
			transaction.rollback();
		}
	}

	private void erstelleSpielerStatistik(Session db)
	{
		StatAktiveSpieler lastStatSpieler = (StatAktiveSpieler)db
				.createQuery("from StatAktiveSpieler order by tick DESC")
				.setMaxResults(1)
				.uniqueResult();
		StatAktiveSpieler statSpieler = new StatAktiveSpieler();
		statSpieler.setTick(this.tick);

		List<User> spielerList = Common.cast(db.createQuery("from User u").list());
		for( User user : spielerList )
		{
			statSpieler.erfasseSpieler(user);
			if( lastStatSpieler != null )
			{
				if( lastStatSpieler.getMaxUserId() < user.getId() )
				{
					statSpieler.erfasseRegistrierung(user);
				}
			}
		}
		db.persist(statSpieler);
	}

	private void ermittleCargoStatistiken(Session db, Cargo cargo, Map<User, Cargo> usercargos, Map<User, Map<Integer, Set<String>>> useritemlocations)
	{
		int counter = 0;
		long baseCount = (Long) db.createQuery("select count(*) from Base where owner!=0").iterate().next();

		this.log("\tLese " + baseCount + " Basen ein");

		while (counter < baseCount)
		{
			List<?> bases = db.createQuery("from Base as b inner join fetch b.owner where b.owner!=0")
					.setCacheMode(CacheMode.IGNORE)
					.setFirstResult(counter)
					.setFetchSize(50)
					.list();
			for (Object base1 : bases)
			{
				Base base = (Base) base1;

				counter++;

				Cargo bcargo = base.getCargo();
				if (base.getOwner().getId() > 0)
				{
					cargo.addCargo(bcargo);
				}

				if (!usercargos.containsKey(base.getOwner()))
				{
					usercargos.put(base.getOwner(), new Cargo(bcargo));
				}
				else
				{
					usercargos.get(base.getOwner()).addCargo(bcargo);
				}


				List<ItemCargoEntry> itemlist = bcargo.getItems();
				for (ItemCargoEntry aitem : itemlist)
				{
					if (aitem.getItem() instanceof Munition)
					{
						continue;
					}

					if (!useritemlocations.containsKey(base.getOwner()))
					{
						useritemlocations.put(base.getOwner(), new HashMap<>());
					}
					Map<Integer, Set<String>> itemlocs = useritemlocations.get(base.getOwner());
					if (!itemlocs.containsKey(aitem.getItemID()))
					{
						itemlocs.put(aitem.getItemID(), new HashSet<>());
					}
					itemlocs.get(aitem.getItemID()).add("b" + base.getId());
				}

				db.evict(base);
			}
		}
		long shipCount = (Long) db.createQuery("select count(*) from Ship where id>0")
				.iterate()
				.next();

		counter = 0;

		this.log("\tLese " + shipCount + " Schiffe ein");
		while (counter < shipCount)
		{
			List<?> ships = db.createQuery("from Ship as s left join fetch s.modules where s.id>0")
					.setCacheMode(CacheMode.IGNORE)
					.setFirstResult(counter)
					.setMaxResults(50)
					.list();
			for (Object ship1 : ships)
			{
				Ship ship = (Ship) ship1;

				counter++;

				if (counter % 10000 == 0)
				{
					this.log("\t\t* " + ship.getId());
				}

				Cargo scargo = ship.getCargo();
				if (ship.getOwner().getId() > 0)
				{
					cargo.addCargo(scargo);
				}

				if (!usercargos.containsKey(ship.getOwner()))
				{
					usercargos.put(ship.getOwner(), new Cargo(scargo));
				}
				else
				{
					usercargos.get(ship.getOwner()).addCargo(scargo);
				}

				List<ItemCargoEntry> itemlist = scargo.getItems();
				for (ItemCargoEntry aitem : itemlist)
				{
					if (aitem.getItem() instanceof Munition)
					{
						continue;
					}
					if (!useritemlocations.containsKey(ship.getOwner()))
					{
						useritemlocations.put(ship.getOwner(), new HashMap<>());
					}
					Map<Integer, Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
					if (!itemlocs.containsKey(aitem.getItemID()))
					{
						itemlocs.put(aitem.getItemID(), new HashSet<>());
					}
					itemlocs.get(aitem.getItemID()).add("s" + ship.getId());
				}

				ModuleEntry[] modulelist = ship.getModules();

				for (ModuleEntry amodule : modulelist)
				{
					Module shipmodule = amodule.createModule();
					if (shipmodule instanceof ModuleItemModule)
					{
						ModuleItemModule itemmodule = (ModuleItemModule) shipmodule;
						if (ship.getOwner().getId() > 0)
						{
							cargo.addResource(itemmodule.getItemID(), 1);
						}
						usercargos.get(ship.getOwner()).addResource(itemmodule.getItemID(), 1);
						if (!useritemlocations.containsKey(ship.getOwner()))
						{
							useritemlocations.put(ship.getOwner(), new HashMap<>());
						}
						Map<Integer, Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
						if (!itemlocs.containsKey(itemmodule.getItemID().getItemID()))
						{
							itemlocs.put(itemmodule.getItemID().getItemID(), new HashSet<>());
						}
						itemlocs.get(itemmodule.getItemID().getItemID()).add("s" + ship.getId());
					}
				}
				db.evict(ship);
				db.evict(ShipModules.class);
			}
		}

		this.log("\tLese Zwischenlager ein");
		ScrollableResults entrylist = db.createQuery("from GtuZwischenlager")
				.setCacheMode(CacheMode.GET)
				.scroll(ScrollMode.FORWARD_ONLY);
		while (entrylist.next())
		{
			GtuZwischenlager entry = (GtuZwischenlager) entrylist.get(0);

			Cargo acargo = entry.getCargo1();
			if (entry.getUser1().getId() > 0)
			{
				cargo.addCargo(acargo);
			}

			if (!usercargos.containsKey(entry.getUser1()))
			{
				usercargos.put(entry.getUser1(), new Cargo(acargo));
			}
			else
			{
				usercargos.get(entry.getUser1()).addCargo(acargo);
			}

			List<ItemCargoEntry> itemlist = acargo.getItems();
			for (ItemCargoEntry aitem : itemlist)
			{
				if (aitem.getItem() instanceof Munition)
				{
					continue;
				}
				if (!useritemlocations.containsKey(entry.getUser1()))
				{
					useritemlocations.put(entry.getUser1(), new HashMap<>());
				}
				Map<Integer, Set<String>> itemlocs = useritemlocations.get(entry.getUser1());
				if (!itemlocs.containsKey(aitem.getItemID()))
				{
					itemlocs.put(aitem.getItemID(), new HashSet<>());
				}
				itemlocs.get(aitem.getItemID()).add("g" + entry.getPosten().getId());
			}

			acargo = entry.getCargo2();
			if (entry.getUser2().getId() > 0)
			{
				cargo.addCargo(acargo);
			}
			if (!usercargos.containsKey(entry.getUser2()))
			{
				usercargos.put(entry.getUser2(), new Cargo(acargo));
			}
			else
			{
				usercargos.get(entry.getUser2()).addCargo(acargo);
			}

			itemlist = acargo.getItems();
			for (ItemCargoEntry aitem : itemlist)
			{
				if (aitem.getItem() instanceof Munition)
				{
					continue;
				}
				if (!useritemlocations.containsKey(entry.getUser2()))
				{
					useritemlocations.put(entry.getUser2(), new HashMap<>());
				}
				Map<Integer, Set<String>> itemlocs = useritemlocations.get(entry.getUser2());
				if (!itemlocs.containsKey(aitem.getItemID()))
				{
					itemlocs.put(aitem.getItemID(), new HashSet<>());
				}
				itemlocs.get(aitem.getItemID()).add("g" + entry.getPosten().getId());
			}
		}
	}

	private void speicherUserCargoStatistik(Session db, Map<User, Cargo> usercargos)
	{
		db.createQuery("delete from StatUserCargo").executeUpdate();

		for( Map.Entry<User, Cargo> entry: usercargos.entrySet() ) {
			User owner = entry.getKey();
			Cargo userCargo = entry.getValue();
			StatUserCargo userstat = new StatUserCargo(owner, userCargo);
			this.log(owner.getId()+":"+userCargo.save());
			db.persist(userstat);
		}
	}

	private void speicherItemLocations(Session db, Map<User, Map<Integer, Set<String>>> useritemlocations)
	{
		db.createQuery("delete from StatItemLocations").executeUpdate();

		for( Map.Entry<User, Map<Integer, Set<String>>> entry: useritemlocations.entrySet() ) {
			User owner = entry.getKey();
			for( Map.Entry<Integer, Set<String>> innerEntry: entry.getValue().entrySet() ) {
				Set<String> locations = innerEntry.getValue();
				int itemid = innerEntry.getKey();

				List<String> locationlist = new ArrayList<>();
				for( String loc : locations ) {
					locationlist.add(loc);

					// Bei einer durchschnittlichen Zeichenkettenlaenge von 8 passen nicht mehr 10 Orte rein
					if( locationlist.size() >= 10 ) {
						break;
					}
				}

				StatItemLocations itemstat = new StatItemLocations(owner, itemid, Common.implode(";", locationlist));
				db.persist(itemstat);
			}
		}
	}

}
