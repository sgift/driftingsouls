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

import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ItemID;
import net.driftingsouls.ds2.server.cargo.modules.ModuleEntry;
import net.driftingsouls.ds2.server.cargo.modules.ModuleType;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.DSApplication;
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
		org.hibernate.Session db = getContext().getDB();

		if( getContext().getRequest().getParameter("help") != null ) {
			printHelp();
			return;
		}

		if( itemid == 0 ) {
			log("Sie muessen --itemid angeben");
			return;
		}

		addLogTarget("_ds2_items_"+itemid+"_to_"+replaceid+"_"+recount+".log", false);

		Item item = (Item)db.get(Item.class, itemid);
		Item replaceitem = (Item)db.get(Item.class, replaceid);
		log("DS2 Itemmanipulator");
		log("ItemID: "+itemid+"");
		if( item != null ) {
			log("["+item.getName()+"]");
		}
		log("ReplaceID: "+replaceid+"");
		if( replaceitem != null ) {
			log("["+replaceitem.getName()+"]");
		}

		log("RE-Count: "+Common.ln(recount)+" RE\n");

		Map<Integer,Long> userlist = new HashMap<Integer,Long>();

		try {
			log("* Processing Bases:");
			List<Base> bases = Common.cast(db.createQuery("FROM Base WHERE cargo LIKE \"%"+itemid+"%\"" ).list());
			for( Base base : bases ) {
				boolean changed = false;

				Cargo cargo = base.getCargo();

				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);

					if( aitem.getItemID() == itemid ) {
						changed = true;

						if( recount != 0 ) {
							awardRE(userlist, base.getOwner().getId(), recount*aitem.getCount());
						}

						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}
				}

				if( changed ) {
					log("\t- modifiziere Basis "+base.getId());
					base.setCargo(cargo);
				}
			}

			log("* Processing Ships:");

			List<Ship> ships = Common.cast(db.createQuery("from Ship where cargo like \"%"+itemid+"5\"" ).list());
			for( Ship ship : ships ) {
				boolean changed = false;

				Cargo cargo = ship.getCargo();

				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);

					if( aitem.getItemID() == itemid ) {
						changed = true;

						if( recount != 0 ) {
							awardRE(userlist, ship.getOwner().getId(), recount*aitem.getCount());
						}

						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}
				}

				if( changed ) {
					log("\t- modifiziere Schiff "+ship.getId());
					ship.setCargo(cargo);
				}
			}

			log("* Processing Ship-Modules:");
			List<?> moduleships = db.createQuery("from ShipModules inner join fetch ship where modules like ?")
				.setString(0, "%:"+itemid+"%")
				.list();

			for( Iterator<?> iter=moduleships.iterator(); iter.hasNext(); ) {
				ShipModules shipModules = (ShipModules)iter.next();
				Ship ship = shipModules.getShip();

				ModuleEntry[] modules = ship.getModules();
				for( int i=0; i < modules.length; i++ ) {
					ModuleEntry module = modules[i];
					if( (module.getModuleType() == ModuleType.ITEMMODULE) && (Integer.parseInt(module.getData()) == itemid) ) {
						ship.removeModule(module);
						if( recount != 0 ) {
							awardRE(userlist, ship.getOwner().getId(), recount);
						}
						if( replaceid != 0  ) {
							ship.addModule(module.getSlot(), module.getModuleType(), Integer.toString(replaceid));
						}
						log("\t- modifiziere "+ship.getId()+": slot "+module.getSlot());
					}
				}
			}

			log("* Processing gtu-zwischenlager:");
			List<GtuZwischenlager> allelager = Common.cast(db.createQuery("from GtuZwischenlager where cargo1 like \"%"+itemid+"%\" or cargo1need like \"%"+itemid+"%\" or cargo2 LIKE \"%"+itemid+"%\" OR cargo2need LIKE \"%"+itemid+"%\"").list());
			for( GtuZwischenlager lager : allelager ) {
				boolean changed = false;

				Cargo cargo = lager.getCargo1();

				List<ItemCargoEntry> itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);

					if( aitem.getItemID() == itemid ) {
						changed = true;

						if( recount != 0 ) {
							awardRE(userlist, lager.getUser1().getId(), recount*aitem.getCount());
						}

						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}
				}

				if( changed ) {
					log("\t- modifiziere "+lager.getId()+" cargo1");
					lager.setCargo1(cargo);
				}

				changed = false;

				cargo = lager.getCargo1Need();

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
					log("\t- modifiziere "+lager.getId()+" cargo1need");
					lager.setCargo1Need(cargo);
				}

				changed = false;

				cargo = lager.getCargo2();

				itemlist = cargo.getItems();
				for( int i=0; i < itemlist.size(); i++ ) {
					ItemCargoEntry aitem = itemlist.get(i);

					if( aitem.getItemID() == itemid ) {
						changed = true;

						if( recount != 0 ) {
							awardRE(userlist, lager.getUser2().getId(), recount*aitem.getCount());
						}

						cargo.setResource(aitem.getResourceID(),0);
						if( replaceid != 0 ) {
							cargo.setResource(new ItemID(replaceid, aitem.getMaxUses(), aitem.getQuestData()), aitem.getCount());
						}
					}
				}

				if( changed ) {
					log("\t- modifiziere "+lager.getId()+" cargo2");
					lager.setCargo2(cargo);
				}

				changed = false;

				cargo = lager.getCargo2Need();

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
					log("\t- modifiziere "+lager.getId()+" cargo2need");
					lager.setCargo2Need(cargo);
				}
			}
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
