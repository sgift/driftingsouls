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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.StatCargo;
import net.driftingsouls.ds2.server.entities.StatItemLocations;
import net.driftingsouls.ds2.server.entities.StatUnitCargo;
import net.driftingsouls.ds2.server.entities.StatUserCargo;
import net.driftingsouls.ds2.server.entities.StatUserUnitCargo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.units.UnitCargo;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 * @author Christopher Jung
 *
 */
public class RestTick extends TickController {
	private int tick;
	
	@Override
	protected void prepare() {
		this.tick = ContextMap.getContext().get(ContextCommon.class).getTick();
	}
	
	@Override
	protected void tick() {
		org.hibernate.Session db = getContext().getDB();
		db.setFlushMode(FlushMode.MANUAL);
		
		this.log("Berechne Gesamtcargo:");
		Cargo cargo = new Cargo();
		UnitCargo unitcargo = new UnitCargo();
		Map<User,Cargo> usercargos = new HashMap<User,Cargo>();
		Map<User,Map<Integer,Set<String>>> useritemlocations = new HashMap<User, Map<Integer, Set<String>>>();
		Map<User,UnitCargo> userunitcargos = new HashMap<User,UnitCargo>();
		
		this.log("\tLese Basen ein");
		ScrollableResults bases = db.createQuery("from Base as b inner join fetch b.owner where b.owner!=0")
			.setCacheMode(CacheMode.IGNORE)
			.setFetchSize(50)
			.scroll(ScrollMode.FORWARD_ONLY);
		while( bases.next() ) {
			Base base = (Base)bases.get(0);
			
			Cargo bcargo = base.getCargo();
			if( base.getOwner().getId() > 0 ) {
				cargo.addCargo( bcargo );
			}
						
			if( !usercargos.containsKey(base.getOwner()) ) {
				usercargos.put(base.getOwner(), new Cargo(bcargo));
			}
			else {
				usercargos.get(base.getOwner()).addCargo( bcargo );
			}
			
			if( !base.getUnits().isEmpty())
			{
				unitcargo.addCargo(base.getUnits());

				if( !userunitcargos.containsKey(base.getOwner()) ) {
					userunitcargos.put(base.getOwner(), new UnitCargo(base.getUnits()));
				}
				else {
					userunitcargos.get(base.getOwner()).addCargo( base.getUnits() );
				}
				
			}
			
			List<ItemCargoEntry> itemlist = bcargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(base.getOwner()) ) {
						useritemlocations.put(base.getOwner(), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(base.getOwner());
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("b"+base.getId());
				}	
			}
			
			db.evict(base);
		}
		
		long shipCount = (Long)db.createQuery("select count(*) from Ship where id>0")
			.iterate()
			.next();
		
		int counter = 0;
		
		this.log("\tLese "+shipCount+" Schiffe ein");
		while( counter < shipCount ) {
			List<?> ships = db.createQuery("from Ship as s left join fetch s.modules where s.id>0")
				.setCacheMode(CacheMode.IGNORE)
				.setFirstResult(counter)
				.setMaxResults(50)
				.list();
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
				Ship ship = (Ship)iter.next();
				
				counter++;
				
				if( counter % 10000 == 0 ) {
					this.log("\t\t* "+ship.getId());
				}
				
				Cargo scargo = ship.getCargo();
				if( ship.getOwner().getId() > 0 ) {
					cargo.addCargo( scargo );
				}
				
				if( !usercargos.containsKey(ship.getOwner()) ) {
					usercargos.put(ship.getOwner(), new Cargo(scargo));
				}
				else {
					usercargos.get(ship.getOwner()).addCargo( scargo );
				}
				
				if( !ship.getUnits().isEmpty())
				{
					unitcargo.addCargo(ship.getUnits());

					if( !userunitcargos.containsKey(ship.getOwner()) ) {
						userunitcargos.put(ship.getOwner(), new UnitCargo(ship.getUnits()));
					}
					else {
						userunitcargos.get(ship.getOwner()).addCargo( ship.getUnits() );
					}
					
				}
				
				List<ItemCargoEntry> itemlist = scargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
						if( !useritemlocations.containsKey(ship.getOwner()) ) {
							useritemlocations.put(ship.getOwner(), new HashMap<Integer,Set<String>>());
						}
						Map<Integer,Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
						if( !itemlocs.containsKey(aitem.getItemID()) ) {
							itemlocs.put(aitem.getItemID(), new HashSet<String>());
						}
						itemlocs.get(aitem.getItemID()).add("s"+ship.getId());
					}	
				}
				
				Ship.ModuleEntry[] modulelist = ship.getModules();
				
				for( int i=0; i < modulelist.length; i++ ) {
					Ship.ModuleEntry amodule = modulelist[i];
					
					Module shipmodule = Modules.getShipModule(amodule);
					if( shipmodule instanceof ModuleItemModule ) {
						ModuleItemModule itemmodule = (ModuleItemModule)shipmodule;
						if( ship.getOwner().getId() > 0 ) {
							cargo.addResource(itemmodule.getItemID(), 1);
						}
						usercargos.get(ship.getOwner()).addResource(itemmodule.getItemID(), 1);
						if( !useritemlocations.containsKey(ship.getOwner()) ) {
							useritemlocations.put(ship.getOwner(), new HashMap<Integer,Set<String>>());
						}
						Map<Integer,Set<String>> itemlocs = useritemlocations.get(ship.getOwner());
						if( !itemlocs.containsKey(itemmodule.getItemID().getItemID()) ) {
							itemlocs.put(itemmodule.getItemID().getItemID(), new HashSet<String>());
						}
						itemlocs.get(itemmodule.getItemID().getItemID()).add("s"+ship.getId());
					}
				}
				db.evict(ship);
				HibernateFacade.evictAll(db, ShipModules.class);
			}
		}

		this.log("\tLese Zwischenlager ein");
		ScrollableResults entrylist = db.createQuery("from GtuZwischenlager")
			.setCacheMode(CacheMode.GET)
			.scroll(ScrollMode.FORWARD_ONLY);
		while( entrylist.next() ) {
			GtuZwischenlager entry = (GtuZwischenlager)entrylist.get(0);
			
			Cargo acargo = entry.getCargo1();
			if( entry.getUser1().getId() > 0 ) {
				cargo.addCargo( acargo );
			}
				
			if( !usercargos.containsKey(entry.getUser1()) ) {
				usercargos.put(entry.getUser1(), new Cargo(acargo));
			}
			else {
				usercargos.get(entry.getUser1()).addCargo( acargo );
			}

			List<ItemCargoEntry> itemlist = acargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(entry.getUser1()) ) {
						useritemlocations.put(entry.getUser1(), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(entry.getUser1());
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("g"+entry.getPosten().getId());
				}
			}
			
			acargo = entry.getCargo2();
			if( entry.getUser2().getId() > 0 ) {
				cargo.addCargo( acargo );
			}
			if( !usercargos.containsKey(entry.getUser2()) ) {
				usercargos.put(entry.getUser2(), new Cargo(acargo));
			}
			else {
				usercargos.get(entry.getUser2()).addCargo( acargo );
			}
			
			itemlist = acargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(entry.getUser2()) ) {
						useritemlocations.put(entry.getUser2(), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(entry.getUser2());
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("g"+entry.getPosten().getId());
				}	
			}
		}
		
		StatCargo stat = new StatCargo(this.tick, cargo);
		StatUnitCargo unitstat = new StatUnitCargo(this.tick, unitcargo);
		db.persist(stat);
		db.persist(unitstat);
		
		db.flush();
		getContext().commit();
		db.evict(stat);
		db.evict(unitstat);

		this.log("\t"+cargo.save());
		this.log("\t"+unitcargo.save());
		
		this.log("Speichere User-Cargo-Stats");
		db.createQuery("delete from StatUserCargo").executeUpdate();
		
		for( Map.Entry<User, Cargo> entry: usercargos.entrySet() ) {
			User owner = entry.getKey();
			Cargo userCargo = entry.getValue();
			StatUserCargo userstat = new StatUserCargo(owner, userCargo);
			db.persist(userstat);
		}
		
		this.log("Speichere User-UnitCargo-Stats");
		db.createQuery("delete from StatUserUnitCargo").executeUpdate();
		
		for( Map.Entry<User, UnitCargo> entry : userunitcargos.entrySet() )
		{
			User owner = entry.getKey();
			UnitCargo userunitcargo = entry.getValue();
			StatUserUnitCargo userunitstat = new StatUserUnitCargo(owner, userunitcargo);
			db.persist(userunitstat);
		}
		
		db.flush();
		getContext().commit();
		HibernateFacade.evictAll(db, StatUserCargo.class);
		
		this.log("Speichere Module-Location-Stats");
		db.createQuery("delete from StatItemLocations").executeUpdate();
		
		for( Map.Entry<User, Map<Integer, Set<String>>> entry: useritemlocations.entrySet() ) {
			User owner = entry.getKey();
			for( Map.Entry<Integer, Set<String>> innerEntry: entry.getValue().entrySet() ) {
				Set<String> locations = innerEntry.getValue();
				int itemid = innerEntry.getKey();
				
				List<String>locationlist = new ArrayList<String>();
				for( String loc : locations ) {
					locationlist.add(loc);
					
					// Bei einer durchschnittlichen Zeichenkettenlaenge von 8 passen nicht mehr 10 Orte rein
					if( locationlist.size() >= 10 ) {
						break;	
					}
				}
				
				StatItemLocations itemstat = new StatItemLocations(owner, itemid, Common.implode(";",locationlist));
				db.persist(itemstat);
			}
		}
		
		db.flush();
		getContext().commit();
		db.clear();
		
		db.setFlushMode(FlushMode.AUTO);
	}

}
