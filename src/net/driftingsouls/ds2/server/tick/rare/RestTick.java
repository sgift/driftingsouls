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
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.modules.Module;
import net.driftingsouls.ds2.server.cargo.modules.ModuleItemModule;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.Ships.ModuleEntry;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben
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
		Database db = getContext().getDatabase();
		
		this.log("Berechne Gesamtcargo:");
		Cargo cargo = new Cargo();
		Map<Integer,Cargo> usercargos = new HashMap<Integer,Cargo>();
		Map<Integer,Map<Integer,Set<String>>> useritemlocations = new HashMap<Integer, Map<Integer, Set<String>>>();
		
		this.log("\tLese Basen ein");
		SQLQuery base = db.query("SELECT id,owner,cargo FROM bases WHERE owner!=0");
		while( base.next() ) {
			Cargo bcargo = new Cargo( Cargo.Type.STRING, base.getString("cargo") );
			if( base.getInt("owner") > 0 ) {
				cargo.addCargo( bcargo );
			}
			
			if( !usercargos.containsKey(base.getInt("owner")) ) {
				usercargos.put(base.getInt("owner"), bcargo);
			}
			else {
				usercargos.get(base.getInt("owner")).addCargo( bcargo );
			}
			
			List<ItemCargoEntry> itemlist = bcargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(base.getInt("owner")) ) {
						useritemlocations.put(base.getInt("owner"), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(base.getInt("owner"));
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("b"+base.getInt("id"));
				}	
			}
		}
		base.free();
		
		this.log("\tLese Ships ein");
		SQLQuery ship = db.query("SELECT id,type,status,owner,cargo FROM ships WHERE id>0");
		while( ship.next() ) {
			Cargo scargo = new Cargo( Cargo.Type.STRING, ship.getString("cargo") );
			if( ship.getInt("owner") > 0 ) {
				cargo.addCargo( scargo );
			}
			
			if( !usercargos.containsKey(ship.getInt("owner")) ) {
				usercargos.put(ship.getInt("owner"), scargo);
			}
			else {
				usercargos.get(ship.getInt("owner")).addCargo( scargo );
			}
			
			List<ItemCargoEntry> itemlist = scargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(ship.getInt("owner")) ) {
						useritemlocations.put(ship.getInt("owner"), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(ship.getInt("owner"));
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("s"+ship.getInt("id"));
				}	
			}
				
			ModuleEntry[] modulelist = Ships.getModules(ship.getRow());
			
			for( int i=0; i < modulelist.length; i++ ) {
				ModuleEntry amodule = modulelist[i];
				
				Module shipmodule = Modules.getShipModule(amodule);
				if( shipmodule instanceof ModuleItemModule ) {
					ModuleItemModule itemmodule = (ModuleItemModule)shipmodule;
					if( ship.getInt("owner") > 0 ) {
						cargo.addResource(itemmodule.getItemID(), 1);
					}
					usercargos.get(ship.getInt("owner")).addResource(itemmodule.getItemID(), 1);
					if( !useritemlocations.containsKey(ship.getInt("owner")) ) {
						useritemlocations.put(ship.getInt("owner"), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(ship.getInt("owner"));
					if( !itemlocs.containsKey(itemmodule.getItemID().getItemID()) ) {
						itemlocs.put(itemmodule.getItemID().getItemID(), new HashSet<String>());
					}
					itemlocs.get(itemmodule.getItemID().getItemID()).add("s"+ship.getInt("id"));
				}
			}
		}
		ship.free();
		
		this.log("\tLese Zwischenlager ein");
		SQLQuery entry = db.query("SELECT posten,user1,user2,cargo1,cargo2 FROM gtu_zwischenlager");
		while( entry.next() ) {
			Cargo acargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo1") );
			if( entry.getInt("user1") > 0 ) {
				cargo.addCargo( acargo );
			}
				
			if( !usercargos.containsKey(entry.getInt("user1")) ) {
				usercargos.put(entry.getInt("user1"), acargo);
			}
			else {
				usercargos.get(entry.getInt("user1")).addCargo( acargo );
			}

			List<ItemCargoEntry> itemlist = acargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(entry.getInt("user1")) ) {
						useritemlocations.put(entry.getInt("user1"), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(entry.getInt("user1"));
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("g"+entry.getInt("posten"));
				}	
			}
			
			acargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo2") );
			if( entry.getInt("user2") > 0 ) {
				cargo.addCargo( acargo );
			}
			if( !usercargos.containsKey(entry.getInt("user2")) ) {
				usercargos.put(entry.getInt("user2"), acargo);
			}
			else {
				usercargos.get(entry.getInt("user2")).addCargo( acargo );
			}
			
			itemlist = acargo.getItems();
			for( int i=0; i < itemlist.size(); i++ ) {
				ItemCargoEntry aitem = itemlist.get(i);
				if( aitem.getItemEffect().getType() != ItemEffect.Type.AMMO ) {
					if( !useritemlocations.containsKey(entry.getInt("user2")) ) {
						useritemlocations.put(entry.getInt("user2"), new HashMap<Integer,Set<String>>());
					}
					Map<Integer,Set<String>> itemlocs = useritemlocations.get(entry.getInt("user2"));
					if( !itemlocs.containsKey(aitem.getItemID()) ) {
						itemlocs.put(aitem.getItemID(), new HashSet<String>());
					}
					itemlocs.get(aitem.getItemID()).add("g"+entry.getInt("posten"));
				}	
			}
		}
		entry.free();
		
		db.update("INSERT INTO stats_cargo (`tick`,`cargo`) VALUES ('",this.tick,"','",cargo.save(),"')");
		this.log("\t"+cargo.save());
		this.log("");
		
		this.log("Speichere User-Cargo-Stats");
		db.update("TRUNCATE TABLE stats_user_cargo");
		
		for( int owner : usercargos.keySet() ) {
			db.update("INSERT INTO stats_user_cargo (`user_id`,`cargo`) VALUES ('",owner,"','",usercargos.get(owner).save(),"')");
		}
		
		this.log("Speichere Module-Location-Stats");
		db.update("TRUNCATE TABLE stats_module_locations");
		
		for( int owner : useritemlocations.keySet() ) {
			for( int itemid : useritemlocations.get(owner).keySet() ) {
				Set<String> locations = useritemlocations.get(owner).get(itemid);
				
				List<String>locationlist = new ArrayList<String>();
				for( String loc : locations ) {
					locationlist.add(loc);
					
					// Bei einer durchschnittlichen Zeichenkettenlaenge von 8 passen nicht mehr 10 Orte rein
					if( locationlist.size() >= 10 ) {
						break;	
					}
				}
				
				db.update("INSERT INTO stats_module_locations (`user_id`,`item_id`,`locations`) VALUES ('",owner,"','",itemid,"','",Common.implode(";",locationlist),"')");
			}	
		}
		
		db.update("DELETE FROM sessions WHERE attach IS NOT NULL");
		this.log("Entferne Admin-Login-Session-IDs..."+db.affectedRows()+" entfernt");
	}

}
