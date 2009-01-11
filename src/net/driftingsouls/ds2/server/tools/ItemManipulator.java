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
package net.driftingsouls.ds2.server.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.modules.Modules;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.DSApplication;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;

/**
 * Kommandozeilentool zur Ersetzung eines Items mit einem anderen. Es erlaubt zudem
 * das gleichzeitige Zahlen von RE-Betraegen an die Besitzer.<br>
 * Achtung: Es werden keine Transaktionen verwendet!
 * @author Christopher Jung
 *
 */
public class ItemManipulator extends DSApplication {
	private int itemid;
	private int replaceid;
	private long recount;
	
	/**
	 * Konstruktor.
	 * @param args Die Kommandozeilenargumente
	 * @throws Exception
	 */
	public ItemManipulator(String[] args) throws Exception {
		super(args);
		
		itemid = getContext().getRequest().getParameterInt("itemid");
		replaceid = getContext().getRequest().getParameterInt("replaceid");
		recount = getContext().getRequest().getParameterInt("recount");
	}
	
	private void printHelp() {
		log("Item Manipulator");
		log("Ersetzt ein Item mit einem anderen und zahlt gleichzeitig (optional) einen Ausgleichsbetrag in RE");
		log("Achtung: Es werden keine Transaktionen verwendet! Bitte vorher DS sperren!");
		log("");
		log("java "+getClass().getName()+" --config $configpfad --itemid $itemid [--replaceid $replaceid] [--recount $recount] [--help]");
		log(" * --config Der Pfad zum DS2-Konfigurationsverzeichnis");
		log(" * --itemid Die ID des Items, welches ersetzt werden soll");
		log(" * [optional] --replaceid Die ID des Items, mit dem das alte Item ersetzt werden soll. Falls nicht angegeben, wird das alte Item einfach entfernt");
		log(" * [optional] --recount Die Hoehe der Ausgleichszahlung in RE. Default ist nichts");
		log(" * [optional] --help Zeigt diese Hilfe an");
	}
	
	private void awardRE(Map<Integer,Long> users, int userid, long re) {
		if( !users.containsKey(userid) ) {
			users.put(userid, re);
		}
		else {
			users.put(userid, users.get(userid)+re);
		}
	}
	
	/**
	 * Startet die Ausfuehrung.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void execute() throws IOException, InterruptedException {
		log("WARNUNG: Dieses Tool funktioniert evt. nicht mehr!");
		
		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}
		
		if( itemid == 0 ) {
			log("Sie muessen --itemid angeben");
			return;
		}
		
		addLogTarget("_ds2_items_"+itemid+"_to_"+replaceid+"_"+recount+".log", false);
		
		log("DS2 Itemmanipulator");
		log("ItemID: "+itemid+"");
		if( Items.get().item(itemid) != null ) {
			log("["+Items.get().item(itemid).getName()+"]");
		}
		log("ReplaceID: "+replaceid+"");
		if( Items.get().item(replaceid) != null ) {
			log("["+Items.get().item(replaceid).getName()+"]");
		}

		log("RE-Count: "+Common.ln(recount)+" RE\n");
		
		Map<Integer,Long> userlist = new HashMap<Integer,Long>();

		Database database = getDatabase();
		org.hibernate.Session db = getDB();
		
		try {
			log("* Processing Bases:");
			SQLQuery base = database.query("SELECT id,cargo,owner FROM bases WHERE cargo LIKE \"%"+itemid+"%\"" );
			while( base.next() ) {
				boolean changed = false;
				
				Cargo cargo = new Cargo( Cargo.Type.STRING, base.getString("cargo") );
				
				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
						
						if( recount != 0 ) {
							awardRE(userlist, base.getInt("owner"), recount*aitem.getCount());
						}
						
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
				
				if( changed ) {
					log("\t- modifiziere Basis "+base.getInt("id"));
					database.update("UPDATE bases SET cargo='"+cargo.save()+"' WHERE id="+base.getInt("id"));	
				}
			}
			base.free();
	
			log("* Processing Ships:");
			SQLQuery shipQuery = database.query("SELECT id,cargo,owner FROM ships WHERE cargo LIKE \"%"+itemid+"%\"" );
			while( shipQuery.next() ) {
				boolean changed = false;
				
				Cargo cargo = new Cargo( Cargo.Type.STRING, shipQuery.getString("cargo") );
				
				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
						
						if( recount != 0 ) {
							awardRE(userlist, shipQuery.getInt("owner"), recount*aitem.getCount());
						}
						
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
				
				if( changed ) {
					log("\t- modifiziere Schiff "+shipQuery.getInt("id"));
					database.update("UPDATE ships SET cargo='"+cargo.save()+"' WHERE id="+shipQuery.getInt("id"));	
				}
			}
			shipQuery.free();
	
			log("* Processing Ship-Modules:");
			List<?> ships = db.createQuery("from ShipModules inner join fetch ship where modules like ?")
				.setString(0, "%:"+itemid+"%")
				.list();
			
			for( Iterator<?> iter=ships.iterator(); iter.hasNext(); ) {
				ShipModules shipModules = (ShipModules)iter.next();
				Ship ship = shipModules.getShip();
				
				Ship.ModuleEntry[] modules = ship.getModules();
				for( int i=0; i < modules.length; i++ ) {
					Ship.ModuleEntry module = modules[i];
					if( (module.moduleType == Modules.MODULE_ITEMMODULE) && (Integer.parseInt(module.data) == itemid) ) {
						ship.removeModule(module.slot, module.moduleType, module.data);
						if( recount != 0 ) {
							awardRE(userlist, shipQuery.getInt("owner"), recount);
						}
						if( replaceid != 0  ) {
							ship.addModule(module.slot, module.moduleType, Integer.toString(replaceid));
						}
						log("\t- modifiziere "+shipQuery.getInt("id")+": slot "+module.slot);
					}
				}
			}
	
			log("* Processing gtu-zwischenlager:");
			SQLQuery entry = database.query("SELECT id,user1,user2,cargo1,cargo1need,cargo2,cargo2need " +
					"FROM gtu_zwischenlager " +
					"WHERE cargo1 LIKE \"%"+itemid+"%\" OR cargo1need LIKE \"%"+itemid+"%\" OR " +
						"cargo2 LIKE \"%"+itemid+"%\" OR cargo2need LIKE \"%"+itemid+"%\"" );
			while( entry.next() ) {
				boolean changed = false;
				
				Cargo cargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo1") );
				
				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
						
						if( recount != 0 ) {
							awardRE(userlist, entry.getInt("user1"), recount*aitem.getCount());
						}
						
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
							
				if( changed ) {
					log("\t- modifiziere "+entry.getInt("id")+" cargo1");
					database.update("UPDATE gtu_zwischenlager SET cargo1='"+cargo.save()+"' WHERE id="+entry.getInt("id"));	
				}
				
				changed = false;
				
				cargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo1need") );
				
				itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
											
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
				
				if( changed ) {
					log("\t- modifiziere "+entry.getInt("id")+" cargo1need");
					database.update("UPDATE gtu_zwischenlager SET cargo1need='"+cargo.save()+"' WHERE id="+entry.getInt("id"));	
				}
				
				changed = false;
				
				cargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo2") );
				
				itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
						
						if( recount != 0 ) {
							awardRE(userlist, entry.getInt("user2"), recount*aitem.getCount());
						}
						
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
				
				if( changed ) {
					log("\t- modifiziere "+entry.getInt("id")+" cargo2");
					database.update("UPDATE gtu_zwischenlager SET cargo2='"+cargo.save()+"' WHERE id="+entry.getInt("id"));	
				}
				
				changed = false;
				
				cargo = new Cargo( Cargo.Type.STRING, entry.getString("cargo2need") );
				
				itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);
					
					if( aitem.getItemID() == itemid ) {
						changed = true;
						
						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}	
				}
				
				if( changed ) {
					log("\t- modifiziere "+entry.getInt("id")+" cargo2need");
					database.update("UPDATE gtu_zwischenlager SET cargo2need='"+cargo.save()+"' WHERE id="+entry.getInt("id"));	
				}
			}
			entry.free();
		}
		catch( Throwable t ) {
			log("Fehler beim Verarbeiten der Cargos: "+t);
			t.printStackTrace();
		}

		log("\n");

		for( int userid : userlist.keySet() ) {
			long re = userlist.get(userid);
			if( re != 0 ) {
				User auser = (User)getContext().getDB().get(User.class, userid);
				
				log("UserID "+userid+": +"+Common.ln(re)+" RE");
				auser.transferMoneyFrom(0, re, "Entsch&auml;digung Item "+itemid, true);
			} 	
		}
	}

	/**
	 * Main.
	 * @param args Die Argumente
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ItemManipulator rs = new ItemManipulator(args);
		rs.execute();
		rs.dispose();
	}

}
