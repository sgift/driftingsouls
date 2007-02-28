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
package net.driftingsouls.ds2.server.modules.ks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.IEAmmo;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.PreparedQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Berechnet das Waffenfeuer im KS
 * @author Christopher Jung
 *
 */
public class KSAttackAction extends BasicKSAction {
	private Weapon weapon;
	private SQLResultRow ownShip;
	private SQLResultRow ownShipBackup;
	private SQLResultRow enemyShip;
	private SQLResultRow localweapon;
	private String attmode;
	private int apmulti;
	private int attcount;
	private int apcost;
	private Random rand = new Random();
	
	/**
	 * Konstruktor
	 *
	 */
	public KSAttackAction() {
		Context context = ContextMap.getContext();
		this.weapon = Weapons.get().weapon(context.getRequest().getParameterString("weapon"));
		
		if( this.weapon == null ) {
			context.addError("FATAL ERROR: Unbekannte Waffe &gt;"+context.getRequest().getParameterString("weapon")+"&lt; gefunden");
			return;
		}
		
		this.requireOwnShipReady(true);
		
		this.ownShip = null;
		this.enemyShip = null;
		this.localweapon = null;
		
		this.attmode = context.getRequest().getParameterString("attmode");
		if( !this.attmode.equals("single") && !this.attmode.equals("alphastrike") && !this.attmode.equals("strafe") &&
			!this.attmode.equals("alphastrike_max") && !this.attmode.equals("strafe_max") ) {
			this.attmode = "single";	
		}
		
		this.apmulti = 1;
		if( this.attmode.equals("alphastrike") || this.attmode.equals("strafe") ) {
			this.apmulti = 5;
		}
		
		this.attcount = context.getRequest().getParameterInt("attcount");
		if( (this.attcount <= 0) || (this.attcount > 3) ) {
			this.attcount = 3;
		} 
				
		this.requireAP(this.weapon.getAPCost()*this.apmulti);
	}
	
	private int attCountForShip( SQLResultRow ownShip, SQLResultRow ownShipType, int attcount ) {
		int count = 0;
		if( attcount == 3 ) {
			count = ownShipType.getInt("shipcount");
		}
		else if( attcount == 2 ) {
			count = (int)Math.ceil( ownShipType.getInt("shipcount")*0.5d );
		}
		else {
			count = 1;
		}
		if( count > ownShip.getInt("count") ) {
			count = ownShip.getInt("count");
		}
		return count;
	}
	
	private int destroyShipOnly(int id, Battle battle, SQLResultRow eShip, boolean generateLoot, boolean generateStats) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		User eUser = context.createUserObject(eShip.getInt("owner"));
		UserFlagschiffLocation loc = eUser.getFlagschiff();
		
		//
		// Schiff als zerstoert makieren
		//
		SQLResultRow ownShip = battle.getOwnShip();
		User oUser = context.createUserObject(ownShip.getInt("owner"));
		
		int masterid = 0;
		if( generateStats ) {
			PreparedQuery pq = db.prepare("INSERT INTO ships_lost (type,name,time,owner,ally,destowner,destally,battle,battlelog) ",
						"VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			pq.update(eShip.getInt("type"), eShip.getString("name"), Common.time(), eShip.getInt("owner"), eUser.getAlly(), ownShip.getInt("owner"), oUser.getAlly(), battle.getID(), Configuration.getSetting("LOXPATH")+"battles/battle_id"+battle.getID()+".log");
	
			masterid = pq.insertID();
			pq.close();
		}
		int remove = 1; // Anzahl der zerstoerten Schiffe
	
		eShip.put("action", eShip.getInt("action") | Battle.BS_DESTROYED);
		db.update("UPDATE battles_ships SET action=action | ",Battle.BS_DESTROYED," WHERE shipid=",eShip.getInt("id"));
	
		// ggf. den Flagschiffstatus zuruecksetzen
		if( (loc != null) && (loc.getType() == UserFlagschiffLocation.Type.SHIP) && 
			(loc.getID() == eShip.getInt("id")) ) {
			eUser.setFlagschiff(0);
		}
	
		if( generateLoot ) {
			// Loot generieren
			Ships.generateLoot( eShip.getInt("id"), ownShip.getInt("owner") );
		}
		
		//
		// Ueberpruefen, ob weitere (angedockte) Schiffe zerstoert wurden
		//
	
		List<SQLResultRow> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow s = enemyShips.get(i);
			
			if( s.getString("docked").equals(eShip.getString("id")) || s.getString("docked").equals("l "+eShip.getInt("id")) ) {
				if( generateStats ) {				
					db.prepare("INSERT INTO ships_lost (type,name,time,owner,ally,destowner,destally,docked,battle,battlelog) ",
								"VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
						.update(s.getInt("type"), s.getString("name"), Common.time(), s.getInt("owner"), eUser.getAlly(), 
								ownShip.getInt("owner"), oUser.getAlly(), Integer.toString(masterid), battle.getID(), 
								Configuration.getSetting("LOXPATH")+"battles/battle_id"+battle.getID()+".log");
				}
	
				remove++;
				db.update("UPDATE battles_ships SET action=action | ",Battle.BS_DESTROYED," WHERE shipid=",s.getInt("id"));
	
				// ggf. den Flagschiffstatus zuruecksetzen
				if( (loc != null) && (loc.getType() == UserFlagschiffLocation.Type.SHIP) && 
					(loc.getID() == s.getInt("id")) ) {
					eUser.setFlagschiff(0);
				}
				
				if( generateLoot ) {
					// Loot generieren
					Ships.generateLoot( s.getInt("id"), ownShip.getInt("owner") );
				}
			}
		}
	
		return remove;
	}
	
	private void destroyShip(int id, Battle battle, SQLResultRow eShip, boolean selectnew) {	
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		int remove = this.destroyShipOnly(id, battle, eShip, true, true);
	
		SQLResultRow ownShip = battle.getOwnShip();
	
		// Wurde mehr als ein Schiff zerstoert?
		if( remove > 1 ) {
			battle.logenemy( (remove-1)+" gedockte/gelandete Schiffe wurden bei der Explosion zerst&ouml;rt\n" );
			battle.logme( (remove-1)+" gedockte/gelandete Schiffe wurden bei der Explosion zerst&ouml;rt\n" );
		}
	
		//
		// Verluste verbuchen (zerstoerte/verlorene Schiffe)
		//
	
		if( battle.getAlly(battle.getOwnSide()) != 0 ) {
			db.update("UPDATE ally SET destroyedShips=destroyedShips+",remove," WHERE id=",battle.getAlly(battle.getOwnSide()));
		} 
		db.update("UPDATE users SET destroyedShips=destroyedShips+",remove," WHERE id=",ownShip.getInt("owner"));
	
		if( battle.getAlly(battle.getEnemySide()) != 0 ) {
			db.update("UPDATE ally SET lostShips=lostShips+",remove," WHERE id=",battle.getAlly(battle.getEnemySide()));
		}
		db.update("UPDATE users SET lostShips=lostShips+",remove," WHERE id=",eShip.getInt("owner"));
	
		//
		// Ein neues Ziel auswaehlen
		//
		if( selectnew ) {
			battle.setEnemyShipIndex(battle.getNewTargetIndex());
			eShip.clear();
			eShip.putAll(battle.getEnemyShip());
		}
	}
	
	private int getTrefferWS( Battle battle, int defTrefferWS, SQLResultRow eShip, SQLResultRow eShipType, int defensivskill, int navskill ) {
		SQLResultRow ownShipType = Ships.getShipType(this.ownShip);
		
		if( (eShip.getInt("crew") == 0) && (eShipType.getInt("crew") > 0) ) {
			return 100;
		}
		if( (defTrefferWS <= 0) && (eShipType.getInt("cost") > 0) && (eShip.getInt("engine") > 0) ) {
			return 0;
		}
	
		int eSize = eShipType.getInt("size");
		int oSize = ownShipType.getInt("size");
		
		if( eSize < oSize ) {
			eSize = (int)Math.round(Math.pow(1.5d,eSize));
			if( eSize > oSize ) {
				eSize = oSize;
			}
		}
	
		// Das Objekt kann sich nicht bewegen - also 100% trefferws
		int trefferWS = 100;
		
		// Das Objekt hat einen Antrieb - also TrefferWS anpassen
		if( ( eShipType.getInt("cost") > 0 ) && ( eShip.getInt("engine") > 0 ) ) {
			trefferWS = defTrefferWS + (int)Math.round((eSize-oSize)*2-defensivskill/15d+navskill/15d)*5;
		} 
		
	
		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}
	
		// Nun die TrefferWS anteilig senken, wenn Crew/Sensoren nicht auf 100 sind
		trefferWS *= (this.ownShip.getInt("sensors")/100);
		if( (ownShipType.getInt("crew") > 0) && (this.ownShip.getInt("crew") < ownShipType.getInt("crew")) ) {
			trefferWS *= this.ownShip.getInt("crew")/ownShipType.getInt("crew");
		}
		
		// Und nun die TrefferWS anteilig steigern, wenn die Gegnerische Crew/Antrie nicht auf 100 sind
		int restws = 100-trefferWS;
		trefferWS += restws*((100-eShip.getInt("engine"))/100);
		if( eShip.getInt("crew") < eShipType.getInt("crew") ) {
			trefferWS += restws*((eShipType.getInt("crew")-eShip.getInt("crew"))/eShipType.getInt("crew"));
		}
		
		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}
	
		return trefferWS;
	}
	
	private int getSmallTrefferWS( Battle battle, int defTrefferWS, SQLResultRow eShip, SQLResultRow eShipType, int defensivskill, int navskill ) {
		SQLResultRow ownShipType = Ships.getShipType(this.ownShip);
		
		if( (eShip.getInt("crew") == 0) && (eShipType.getInt("crew") > 0) ) {
			return 100;
		}
		if( (defTrefferWS <= 0) && (eShipType.getInt("cost") > 0) && (eShip.getInt("engine") > 0) ) {
			return 0;
		}
		
		int eSize = eShipType.getInt("size");
		int oSize = ownShipType.getInt("size");
	
		if( oSize > 3 ) {
			oSize = 3;
		}
	
		// Das Objekt kann sich nicht bewegen - also 100% trefferws
		int trefferWS = 100;
		
		// Das Objekt hat einen Antrieb - also TrefferWS anpassen
		if( ( eShipType.getInt("cost") > 0 ) && ( eShip.getInt("engine") > 0 ) ) {
			trefferWS = defTrefferWS + (int)Math.round((eSize-oSize)*2-defensivskill/15d+navskill/15d)*5;
		} 
	
		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}
	
		// Nun die TrefferWS anteilig senken, wenn Crew/Sensoren nicht auf 100 sind
		trefferWS *= (this.ownShip.getInt("sensors")/100);
		if( (ownShipType.getInt("crew") > 0) && (this.ownShip.getInt("crew") < ownShipType.getInt("crew")) ) {
			trefferWS *= this.ownShip.getInt("crew")/ownShipType.getInt("crew");
		}
		
		// Und nun die TrefferWS anteilig steigern, wenn die Gegnerische Crew/Antrie nicht auf 100 sind
		int restws = 100-trefferWS;
		trefferWS += restws*((100-eShip.getInt("engine"))/100);
		if( eShip.getInt("crew") < eShipType.getInt("crew") ) {
			trefferWS += restws*((eShipType.getInt("crew")-eShip.getInt("crew"))/eShipType.getInt("crew"));
		}
		
		if( trefferWS < 0 ) {
			trefferWS = 0;
		}
		if( trefferWS > 100 ) {
			trefferWS = 100;
		}
		
		return trefferWS;
	}
	
	private int getOffensivSkill(SQLResultRow ownShipType, Offizier offizier ) {
		if( offizier != null ) {
			return (int)Math.round((offizier.getAbility(Offizier.Ability.WAF)+offizier.getAbility(Offizier.Ability.COM))/2d);
		}
		
		return -ownShipType.getInt("crew");
	}
	
	private int getNavSkill( Battle battle, SQLResultRow ownShipType, Offizier offizier ) {
		int navskill = ownShipType.getInt("size")*3;
		
		if( offizier != null ) {
			navskill = offizier.getAbility(Offizier.Ability.NAV);
		} 
		
		navskill *= (battle.getOwnShip().getInt("engine")/100);
	
		return navskill;
	}
	
	private int getDefensivSkill( SQLResultRow enemyShipType, Offizier eOffizier ) {
		if( eOffizier != null ) {
			return (int)Math.round((eOffizier.getAbility(Offizier.Ability.NAV)+eOffizier.getAbility(Offizier.Ability.COM))/2d);
		} 

		return -enemyShipType.getInt("crew");
	}
	
	private boolean calcDamage( Battle battle, SQLResultRow eShip, SQLResultRow eShipType, int hit, int absSchaden, int schaden, int[] subdmgs, String prefix ) {
		boolean ship_intact = true;
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
	
		SQLResultRow battle_ship = db.first("SELECT * FROM battles_ships WHERE shipid=",eShip.getInt("id"));
		SQLResultRow battleShipBackup = (SQLResultRow)battle_ship.clone();
	
		if( (prefix != null) && prefix.length() > 0 ) {
			battle.logme("\n"+prefix+":\n");	
			battle.logenemy("\n"+prefix+":\n");	
		}
	
		if( (hit != 0) && (battle_ship.getInt("shields") > 0) ) {
			if( battle_ship.getInt("shields") >= absSchaden*hit ) {
				battle_ship.put("shields", battle_ship.getInt("shields") - absSchaden*hit);
				if( battle_ship.getInt("shields") == 0 ) {
					battle.logme( "+ Schilde ausgefallen\n" );
					battle.logenemy( "+ Schilde ausgefallen\n" );
				}
				else {
					battle.logme( "+ Schaden (Schilde): "+Common.ln(hit*absSchaden)+"\n" );
					battle.logenemy( "+ Schilde: "+Common.ln(hit*absSchaden)+" Schaden\n" );
				}
				hit = 0;
			}
			else {
				hit -= Math.ceil(battle_ship.getInt("shields")/absSchaden);
				battle_ship.put("shields", 0);
				battle.logme( "+ Schilde ausgefallen\n" );
				battle.logenemy( "+ Schilde ausgefallen\n" );
			}
		}
		if( hit != 0 ) {
			int hulldamage = hit*schaden;

			if( Ships.hasShipTypeFlag(eShipType, Ships.SF_ZERSTOERERPANZERUNG) ) {
				int dmgThisTurn = eShip.getInt("hull")-battle_ship.getInt("hull")+hulldamage;
				if( dmgThisTurn / (double)eShipType.getInt("hull") > 0.33 ) {
					int newhulldamage = (int)(eShipType.getInt("hull")*0.33 - (eShip.getInt("hull")-battle_ship.getInt("hull")));
					battle.logme("+ Zerst&ouml;rerpanzerung absorbiert Schaden ("+Common.ln(hulldamage-newhulldamage)+" dmg)\n");
					battle.logenemy("+ Zerst&ouml;rerpanzerung absorbiert Schaden  ("+Common.ln(hulldamage-newhulldamage)+" dmg)\n");
					
					hulldamage = newhulldamage;
				}
			}
			
			if( Ships.hasShipTypeFlag(eShipType, Ships.SF_GOD_MODE ) ) {
				if( battle_ship.getInt("hull") - hulldamage < 1 ) {
					hulldamage = battle_ship.getInt("hull") - 1;
					battle.logme("+ Schiff nicht zerst&ouml;rbar\n");
					battle.logenemy("+ Schiff nicht zerst&ouml;rbar\n");	
				}
			}

			if( battle_ship.getInt("hull") - hulldamage > 0 ) {
				ship_intact = true;
			}
			else {
				ship_intact = false;	
			}
	
			battle_ship.put("hull", battle_ship.getInt("hull") - hulldamage);
			if( battle_ship.getInt("hull") < 0 ) {
				battle_ship.put("hull", 0);
			}
	
			if( battle_ship.getInt("hull") > 0 ) {
				battle.logme( "+ Schaden (H&uuml;lle): "+Common.ln(hulldamage)+"\n" );
				battle.logenemy( "+ H&uuml;lle: "+Common.ln(hulldamage)+" Schaden\n" );
	
				//Subsysteme
				if( subdmgs != null && (subdmgs.length > 0) ) {
					List<String> subsysteme = new ArrayList<String>();
					subsysteme.add("sensors");
					subsysteme.add("comm");
					
					List<String> subsysteme_name = new ArrayList<String>();
					subsysteme_name.add("Sensoren");
					subsysteme_name.add("Kommunikation");
	
					if( eShipType.getInt("cost") > 0 ) {
						subsysteme.add("engine");
						subsysteme_name.add("Antrieb");
					}
	
					if( eShipType.getInt("military") > 0 ) {
						subsysteme.add("weapons");
						subsysteme_name.add("Waffen");
					}
					
					for( int i=0; i < subdmgs.length; i++ ) {
						int subdmg = subdmgs[i];
						
						if( subdmg < 1 ) {
							continue;
						}
						
						int rnd = rand.nextInt(subsysteme.size());
						String subsys = subsysteme.get(rnd);
						
						battle_ship.put(subsys, battle_ship.getInt(subsys) - subdmg);
						if( battle_ship.getInt(subsys) > 0 ) {
							battle.logme("+ "+subsysteme_name.get(rnd)+": "+Common.ln(subdmg)+" Schaden\n");
							battle.logenemy("+ "+subsysteme_name.get(rnd)+": "+Common.ln(subdmg)+" Schaden\n");
						} 
						else {
							battle.logme("+ "+subsysteme_name.get(rnd)+": ausgefallen\n");
							battle.logenemy("+ "+subsysteme_name.get(rnd)+": ausgefallen\n");
							battle_ship.put(subsys, 0);
						}
					}
				}
			}
			else {
				battle.logme( "[color=red]+ Schiff zerst&ouml;rt[/color]\n" );
				battle.logenemy( "[color=red]+ Schiff zerst&ouml;rt[/color]\n" );
				if( Configuration.getIntSetting("DESTROYABLE_SHIPS") == 0 ) {
					eShip.put("type", Configuration.getIntSetting("CONFIG_TRUEMMER"));
					eShip.put("hull", Configuration.getIntSetting("CONFIG_TRUEMMER_HUELLE"));
					eShip.put("crew", 0);
				}
				if( battle_ship.getInt("newcount") == 0 ) {
					battle_ship.put("newcount", eShip.getInt("count"));
				}
				if( battle_ship.getInt("newcount") > 1 ) {
					battle_ship.put("newcount", battle_ship.getInt("newcount")-1);
					battle.logme( "[color=red]+ "+battle_ship.getInt("newcount")+" Schiffe verbleiben[/color]\n" );
					battle.logenemy( "[color=red]+ "+battle_ship.getInt("newcount")+" Schiffe verbleiben[/color]\n" );
				
					battle_ship.put("hull", eShipType.getInt("hull"));
					battle_ship.put("shields", eShipType.getInt("shields"));
					battle_ship.put("engine", 100);
					battle_ship.put("weapons", 100);
					battle_ship.put("comm", 100);
					battle_ship.put("sensors", 100);
					ship_intact = true;					
				}
			}
		}
	
		if( !ship_intact ) {
			if( (battle_ship.getInt("action") & Battle.BS_HIT) != 0 ) {
				battle_ship.put("action", battle_ship.getInt("action") ^ Battle.BS_HIT);
			}
			battle_ship.put("action", battle_ship.getInt("action") | Battle.BS_DESTROYED);
		}
		else {
			battle_ship.put("action", battle_ship.getInt("action") | Battle.BS_HIT);
			if( (battle_ship.getInt("action") & Battle.BS_FLUCHTNEXT) != 0 && (battle_ship.getInt("engine") == 0) && (eShipType.getInt("cost") > 0) ) {
				battle_ship.put("action", battle_ship.getInt("action") ^ Battle.BS_FLUCHTNEXT);
			}
			if( (battle_ship.getInt("action") & Battle.BS_FLUCHT) != 0 && (battle_ship.getInt("engine") == 0) && (eShipType.getInt("cost") > 0) ) {
				battle_ship.put("action", battle_ship.getInt("action") ^ Battle.BS_FLUCHT);
				battle.logme( "+ Flucht gestoppt\n" );
				battle.logenemy( "[color=red]+ Flucht gestoppt[/color]\n" );
			}
		}
		eShip.put("action", battle_ship.getInt("action"));

		if( (battle_ship.getInt("hull") != battleShipBackup.getInt("hull")) || 
			(battle_ship.getInt("shields") != battleShipBackup.getInt("shields")) || 
			(battle_ship.getInt("engine") != battleShipBackup.getInt("engine")) || 
			(battle_ship.getInt("weapons") != battleShipBackup.getInt("weapons")) || 
			(battle_ship.getInt("comm") != battleShipBackup.getInt("comm")) || 
			(battle_ship.getInt("sensors") != battleShipBackup.getInt("sensors")) || 
			(battle_ship.getInt("action") != battleShipBackup.getInt("action")) ||
			(battle_ship.getInt("newcount") != battleShipBackup.getInt("newcount")) ) {
			db.tUpdate(1, "UPDATE battles_ships ",
						"SET hull=",battle_ship.getInt("hull"),", ",
							"shields=",battle_ship.getInt("shields"),", ",
							"engine=",battle_ship.getInt("engine"),", ",
							"weapons=",battle_ship.getInt("weapons"),", ",
							"comm=",battle_ship.getInt("comm"),", ",
							"sensors=",battle_ship.getInt("sensors"),", ",
							"action=",battle_ship.getInt("action"),", ",
							"newcount=",battle_ship.getInt("newcount")," ",
						"WHERE shipid=",eShip.getInt("id")," AND hull=",battleShipBackup.getInt("hull")," AND " ,
								"shields=",battleShipBackup.getInt("shields")," AND " ,
								"engine=",battleShipBackup.getInt("engine")," AND " ,
								"weapons=",battleShipBackup.getInt("weapons")," AND " ,
								"comm=",battleShipBackup.getInt("comm")," AND " ,
								"sensors=",battleShipBackup.getInt("sensors")," AND " ,
								"action=",battleShipBackup.getInt("action")," AND " ,
								"newcount=",battleShipBackup.getInt("newcount") );
								
		}
	
		return ship_intact;
	}
	
	private SQLResultRow getWeaponData( Battle battle ) {	
		SQLResultRow ownShipType = Ships.getShipType(this.ownShip);
		SQLResultRow enemyShipType = Ships.getShipType(this.enemyShip);
		
		SQLResultRow localweapon = new SQLResultRow();
		
		if( enemyShipType.getInt("size") < 4 ) {
			localweapon.put("deftrefferws", this.weapon.getDefSmallTrefferWS());
		} 
		else {
			localweapon.put("deftrefferws", this.weapon.getDefTrefferWS());
		}
		localweapon.put("basedamage", this.weapon.getBaseDamage(ownShipType));
		localweapon.put("shielddamage", this.weapon.getShieldDamage(ownShipType));
		localweapon.put("name", this.weapon.getName());
		localweapon.put("shotsPerShot", this.weapon.getSingleShots());
		localweapon.put("subws", this.weapon.getDefSubWS());
		localweapon.put("subdamage", this.weapon.getSubDamage(ownShipType));
		localweapon.put("destroyAfter", this.weapon.hasFlag(Weapon.Flags.DESTROY_AFTER));
		localweapon.put("areadamage", this.weapon.getAreaDamage());
		localweapon.put("destroyable", this.weapon.getDestroyable() ? 1.0 : 0.0);
		localweapon.put("ad_full", this.weapon.hasFlag(Weapon.Flags.AD_FULL));
		localweapon.put("long_range", this.weapon.hasFlag(Weapon.Flags.LONG_RANGE));
		
		return localweapon;
	}
	
	private SQLResultRow getAmmoBasedWeaponData( Battle battle ) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		SQLResultRow ownShip = this.ownShip;
		
		SQLResultRow enemyShipType = Ships.getShipType(this.enemyShip);
		SQLResultRow ownShipType = Ships.getShipType(ownShip);
		
		final String weaponName = context.getRequest().getParameterString("weapon");
		
		Map<String,String> weapons = Weapons.parseWeaponList(ownShipType.getString("weapons"));
		int weaponCount = Integer.parseInt(weapons.get(weaponName));
		
		weaponCount = (int)(weaponCount/(double)ownShipType.getInt("shipcount")*this.attCountForShip(this.ownShip, ownShipType, this.attcount));

		SQLResultRow ammo = null;
		ItemCargoEntry ammoitem = null;
		
		// Munition
		Cargo mycargo = new Cargo( Cargo.Type.STRING, ownShip.getString("cargo") );
	
		if( this.weapon.hasFlag(Weapon.Flags.AMMO_SELECT) ) {
			int ammoid = context.getRequest().getParameterInt("ammoid");
	
			ItemCargoEntry item = null;
			List<ItemCargoEntry> itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				if( itemlist.get(i).getItemID() == ammoid ) {
					item = itemlist.get(i);
					break;
				}
			}
	
			if( item == null ) {
				battle.logme( "Sie verf&uuml;gen nicht &uuml;ber den angegebenen Munitionstyp\n" );
				return null;
			}
	
			ammo = db.first("SELECT id,name,damage,shielddamage,trefferws,smalltrefferws,shotspershot,destroyable,subws,subdamage,areadamage,flags FROM ammo WHERE itemid=",ammoid," AND `type`='",this.weapon.getAmmoType(),"'");
			ammoitem = null;
	
			for( int i=0; i < itemlist.size(); i++ ) {
				IEAmmo effect = (IEAmmo)itemlist.get(i).getItemEffect();
				if( effect.getAmmoID() == ammo.getInt("id") ) {
					ammoitem = itemlist.get(i);
				}
			}
		} 
		else {
			List<Integer> ammoids = new ArrayList<Integer>();
			
			List<ItemCargoEntry> itemlist = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < itemlist.size(); i++ ) {
				IEAmmo effect = (IEAmmo)itemlist.get(i).getItemEffect();
				ammoids.add(effect.getAmmoID());
			}
	
			if( ammoids.size() == 0 ) {
				battle.logme( "Sie verf&uuml;gen &uuml;ber keine Munition\n" );
				return null;
			}
	
			ammo = db.first("SELECT id,name,damage,shielddamage,trefferws,smalltrefferws,shotspershot,destroyable,torptrefferws,subws,subdamage,areadamage,flags FROM ammo WHERE type='",this.weapon.getAmmoType(),"' AND id IN (",Common.implode(",",ammoids),") LIMIT 1");
	
			ammoitem = null;
			
			for( int i=0; i < itemlist.size(); i++ ) {
				IEAmmo effect = (IEAmmo)itemlist.get(i).getItemEffect();
				if( effect.getAmmoID() == ammo.getInt("id") ) {
					ammoitem = itemlist.get(i);
				}
			}
		}
	
		if( (ammo == null) || ammo.isEmpty() ) {
			battle.logme("Der angegebene Munitionstyp existiert nicht\n" );
			return null;
		}
	
		weaponCount = (int)(weaponCount*this.ownShip.getInt("count")/(double)ownShipType.getInt("shipcount"));
		
		if( ammoitem.getCount() <  weaponCount*this.weapon.getSingleShots() ) {
			battle.logme( this.weapon.getName()+" k&ouml;nnen nicht abgefeuert werden, da nicht genug Munition f&uuml;r alle Gesch&uuml;tze vorhanden ist.\n" );
			return null;
		}
	
		battle.logme( "Feuere "+ammo.getString("name")+" ab...\n" );
	
		SQLResultRow localweapon = new SQLResultRow();
		if( enemyShipType.getInt("size") < 4 ) {
			localweapon.put("deftrefferws", ammo.getInt("smalltrefferws"));
		} 
		else {
			localweapon.put("deftrefferws", ammo.getInt("trefferws"));
		}
		localweapon.put("basedamage", ammo.getInt("damage"));
		localweapon.put("shielddamage", ammo.getInt("shielddamage"));
		localweapon.put("shotsPerShot", ammo.getInt("shotspershot")*this.weapon.getSingleShots());
		localweapon.put("name", ammo.getString("name"));
		localweapon.put("subws", ammo.getInt("subws"));
		localweapon.put("subdamage", ammo.getInt("subdamage"));
		localweapon.put("destroyAfter", false);
		localweapon.put("areadamage", ammo.getInt("areadamage"));
		localweapon.put("destroyable", ammo.getDouble("destroyable"));
		localweapon.put("ammoitem", ammoitem);
		localweapon.put("ad_full", (ammo.getInt("flags") & Weapon.AmmoFlags.AD_FULL.getBits()) != 0);
		localweapon.put("long_range", this.weapon.hasFlag(Weapon.Flags.LONG_RANGE));
			
		return localweapon;
	}
	
	private int getAntiTorpTrefferWS(SQLResultRow enemyShipType) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		Map<String,String> eweapons = Weapons.parseWeaponList(enemyShipType.getString("weapons"));
				
		int antitorptrefferws = 1;
		Map<String,SQLResultRow> ammocache = new HashMap<String,SQLResultRow>();
				
		for( String wpn : eweapons.keySet() ) {
			int count = Integer.parseInt(eweapons.get(wpn));
			
			if( Weapons.get().weapon(wpn).getTorpTrefferWS() != 0 ) {
				antitorptrefferws *= Math.pow(1-(Weapons.get().weapon(wpn).getTorpTrefferWS()/100),count);
			}
			else if( !Weapons.get().weapon(wpn).getAmmoType().equals("none") && !Weapons.get().weapon(wpn).hasFlag(Weapon.Flags.AMMO_SELECT) ) {
				if( !ammocache.containsKey(Weapons.get().weapon(wpn).getAmmoType()) ) {
					ammocache.put(
							Weapons.get().weapon(wpn).getAmmoType(), 
							db.first("SELECT torptrefferws FROM ammo " +
									"WHERE type='",Weapons.get().weapon(wpn).getAmmoType(),"'")
					);
				}
				antitorptrefferws *= Math.pow(1-(ammocache.get(Weapons.get().weapon(wpn).getAmmoType()).getInt("torptrefferws"))/100,count);
			}
		}	
		antitorptrefferws = 1 - antitorptrefferws;
		antitorptrefferws *= 100;
		antitorptrefferws *= (this.enemyShip.getInt("weapons")/100);
		antitorptrefferws /= this.localweapon.getDouble("destroyable");
		
		return antitorptrefferws;	
	}
	
	private int getFighterDefense( Battle battle ) {
		int defcount = 0;
		int fightercount = 0;
		
		List<SQLResultRow> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow selectedShip = enemyShips.get(i);
			
			SQLResultRow type = Ships.getShipType(selectedShip);
			if( (type.getInt("torpedodef") == 0) && (type.getInt("size") > 3) ) {
				defcount++;
			}
			if( (selectedShip.getString("docked").length() == 0) && (selectedShip.getInt("action") & Battle.BS_FLUCHT) == 0 && 
				(selectedShip.getInt("action") & Battle.BS_JOIN) == 0 ) {
				fightercount += type.getInt("torpedodef");
			}
		}
		if( defcount == 0 ) {
			defcount = 1;	
		}
		int fighterdef = (int)Math.round((fightercount/(double)defcount)/localweapon.getDouble("destroyable"));
		if( fighterdef > 100 ) {
			fighterdef = 100;	
		}
			
		return fighterdef;
	}

	private List<SQLResultRow> getADShipList( Battle battle ) {
		int type = this.enemyShip.getInt("type");
		
		// schiffe zusammensuchen
		List<SQLResultRow> shiplist = new ArrayList<SQLResultRow>();
		List<SQLResultRow> backup = new ArrayList<SQLResultRow>();
		boolean gottarget = false;
		
		List<SQLResultRow> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow eship = enemyShips.get(i);
			
			if( eship.getInt("type") == type ) {
				if( eship.getInt("id") == this.enemyShip.getInt("id") ) {
					gottarget = true;
					continue;
				}
				else if( (eship.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
					continue;	
				}
				else if( (eship.getInt("action") & Battle.BS_FLUCHT) != 0 && (this.enemyShip.getInt("action") & Battle.BS_FLUCHT) == 0 ) {
					continue;
				}
				else if( (eship.getInt("action") & Battle.BS_FLUCHT) == 0 && (this.enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 ) {
					continue;
				}
				else if( (eship.getInt("action") & Battle.BS_JOIN) != 0 ) {
					continue;	
				}
				else if( eship.getString("docked").length() > 0 && (eship.getString("docked").charAt(0) == 'l') ) {
					continue;	
				}
				
				shiplist.add(eship);
				if( !gottarget && (i >= this.localweapon.getInt("areadamage")) ) {
					backup.add(shiplist.remove(0));
				}
				else {
					i++;
				}
				
				if( gottarget && (i >= this.localweapon.getInt("areadamage")*2) ) {
					break;
				}
			}	
		}
		
		if( shiplist.size() < this.localweapon.getInt("areadamage")*2 ) {
			for( int j=shiplist.size(); (j < this.localweapon.getInt("areadamage")*2) && !backup.isEmpty(); j++ )	{
				shiplist.add(backup.remove(backup.size()-1));
			}
		}
		
		final SQLResultRow emptyRow = new SQLResultRow();
		
		// Ein leeres Element hinzufuegen, falls wir nicht genug Elemente haben
		if( (shiplist.size() < this.localweapon.getInt("areadamage")*2) && (shiplist.size() % 2 != 0) ) {
			shiplist.add(emptyRow);
		}
		
		int listmiddle = shiplist.size()/2;
		
		List<SQLResultRow> areashiplist = new ArrayList<SQLResultRow>(shiplist.size());
		for( int i=0; i < shiplist.size()+1; i++ ) {
			areashiplist.add(emptyRow);
		}
		
		areashiplist.set(listmiddle, this.enemyShip);
		for( int i=1; i <= shiplist.size(); i++ ) {
			if( i % 2 == 0 ) {
				areashiplist.set(listmiddle-(i/2), shiplist.get(i-1));
			}
			else {
				areashiplist.set(listmiddle+(int)Math.ceil(i/2d), shiplist.get(i-1));
			}	
		}
		
		return areashiplist;
	}
	
	private int[] getSubDamages( int subPanzerung, int trefferWS, int subWS, double damageMod ) {
		int subDamage = (int)Math.round(this.localweapon.getInt("subdamage")*((10-subPanzerung)/10d)*damageMod);
	
		int hit=0;
		int[] tmpSubDmgs = new int[this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot")];
		int totalSize = 0;
		
		for( int i=1; i <= this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"); i++) {
			int rnd = rand.nextInt(101);
			if( rnd <= trefferWS ) {
				hit++;
			}
	
			if( rnd <= subWS ) {
				tmpSubDmgs[totalSize++] = (int)Math.round((rnd/(double)subWS)*subDamage);
			}
		}
		
		// Falls nicht alle Felder benoetigt wurden, dann das Array entsprechend gekuerzt zurueckgeben
		if( totalSize < tmpSubDmgs.length ) {
			int[] subDmgs = new int[totalSize];
			System.arraycopy(tmpSubDmgs, 0, subDmgs, 0, totalSize);
			
			return subDmgs;
		}
		
		return tmpSubDmgs;
	}
	
	private void calcADStep( Battle battle, int trefferWS, int navskill, SQLResultRow aeShip, int hit, int schaden, int shieldSchaden, double damagemod ) {
		Context context = ContextMap.getContext();
		User user = context.getActiveUser();	
		
		battle.logme("\n"+aeShip.getString("name")+" ("+aeShip.getInt("id")+"):\n");
		battle.logenemy("\n"+aeShip.getString("name")+" ("+aeShip.getInt("id")+"):\n");
					
		SQLResultRow aeShipType = Ships.getShipType(aeShip);
		
		int[] tmpsubdmgs = null;
					
		if( this.localweapon.getInt("subdamage") > 0 ) {
			int tmppanzerung = (int)Math.round(aeShipType.getInt("panzerung")*aeShip.getInt("hull")/(double)aeShipType.getInt("maxhull"));
						
			Offizier eOffizier = Offizier.getOffizierByDest('s', aeShip.getInt("id"));
			int defensivskill = this.getDefensivSkill( aeShipType, eOffizier );

			int subWS = this.getTrefferWS( battle, this.localweapon.getInt("subws"), aeShip, aeShipType, defensivskill, navskill );
			battle.logme( "SubsystemTWS: "+subWS+"%\n" );
					
			int subPanzerung = tmppanzerung;
			if( subPanzerung > 10 ) {
				subPanzerung = 10;
				battle.logme("Panzerung absorbiert Subsystemschaden\n");
			} 
			else if( subPanzerung > 0 ) {
				battle.logme("Panzerung reduziert Subsystemschaden ("+(subPanzerung*10)+"%)\n");
			}
			
			tmpsubdmgs = getSubDamages(subPanzerung, trefferWS, subWS, damagemod);
		}
		
		boolean mydamage = this.calcDamage( battle, aeShip, aeShipType, hit, (int)(shieldSchaden*damagemod), (int)(schaden*damagemod), tmpsubdmgs, "" );
		if( !mydamage && (Configuration.getIntSetting("DESTROYABLE_SHIPS") != 0) ) {
			this.destroyShip(user.getID(), battle, aeShip, false);
		}
	}
	
	private int getDamage(int damage, int offensivskill, SQLResultRow enemyShipType) {
		int schaden = (int)Math.round( (damage + damage*offensivskill/1500d) *
							(this.ownShip.getInt("weapons")/100d) *
							this.weapon.getBaseDamageModifier(enemyShipType));
							 
		if( schaden < 0 ) {
			schaden = 0;
		}
		
		return schaden;
	}
	
	@Override
	public int execute(Battle battle) {
		Context context = ContextMap.getContext();
		
		User user = context.getActiveUser();	
		
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.weapon == null ) {
			return RESULT_ERROR;
		}
		
		// Schiff laden
		
		Database db = context.getDatabase();
		this.ownShip = battle.getOwnShip();
		this.ownShipBackup = (SQLResultRow)this.ownShip.clone();
	
		SQLResultRow ownShipType = Ships.getShipType(this.ownShip);
		
		Map<String,String> weaponList = Weapons.parseWeaponList(ownShipType.getString("weapons"));
		Map<String,String> maxheatList = Weapons.parseWeaponList(ownShipType.getString("maxheat"));
		Map<String,String> heatList = Weapons.parseWeaponList(this.ownShip.getString("heat"));
		
		final String weaponName = context.getRequest().getParameterString("weapon");
		int weapons = Integer.parseInt(weaponList.get(weaponName));
		int maxheat = Integer.parseInt(maxheatList.get(weaponName));
		int heat = 0;
		if( heatList.containsKey(weaponName) ) {
			heat = Integer.parseInt(heatList.get(weaponName));
		}
		
		weapons = (int)(weapons/(double)ownShipType.getInt("shipcount"))*this.attCountForShip(this.ownShip, ownShipType, this.attcount);
		if( ownShipType.getInt("shipcount") > this.ownShip.getInt("count") ) {
			maxheat = (int)(maxheat*this.ownShip.getInt("count")/(double)ownShipType.getInt("shipcount"));
		}
		
		// Feststellen wie oft wird welchen Feuerloop durchlaufen sollen
		
		int apcost = 0;
		
		boolean firstentry = true; // Battlehistory-Log
		
		int sameShipLoop = 1; // Alphastrike (same ship)
		int nextShipLoop = 1; // Breitseite (cycle through ships)
		
		if( this.attmode.equals("alphastrike") ) {
			sameShipLoop = 5;	
		}
		else if( this.attmode.equals("strafe") ) {
			nextShipLoop = 5;	
		}
		else if( this.attmode.equals("alphastrike_max") ) {
			if( weapons > 0 ) {
				sameShipLoop = (int)((maxheat-heat)/(double)weapons);
			}
			else {
				sameShipLoop = 1;
			}
			if( sameShipLoop < 1 ) {
				sameShipLoop = 1;
			}
		}
		else if( this.attmode.equals("strafe_max") ) {
			if( weapons > 0 ) {
				nextShipLoop = (int)((maxheat-heat)/(double)weapons);
			}
			else {
				nextShipLoop = 1;
			}
			if( nextShipLoop < 1 ) {
				nextShipLoop = 1;	
			}
		}
		
		this.apcost = sameShipLoop*nextShipLoop*this.weapon.getAPCost()*this.attCountForShip(this.ownShip, ownShipType, this.attcount);
		if( this.apcost > battle.getPoints(battle.getOwnSide()) ) {
			battle.logme( "Sie haben nicht genug Aktionspunkte um mit dieser Waffe auf das fl&uuml;chtende Schiff zu feuern\n" );
			return RESULT_ERROR;
		}
		
		// Und nun checken wir mal ein wenig....
		
		if( (ownShipType.getInt("crew") > 0) && (this.ownShip.getInt("crew") <= (int)(ownShipType.getInt("crew")/4d)) ) {
			battle.logme( "Nicht genug Crew um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
			return RESULT_ERROR;
		}
		
		if( (this.ownShip.getInt("action") & Battle.BS_DISABLE_WEAPONS) != 0 ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( (this.ownShip.getInt("action") & Battle.BS_BLOCK_WEAPONS) != 0 ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		boolean gotone = false;
		if( Ships.hasShipTypeFlag(ownShipType, Ships.SF_DROHNE) ) {
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				SQLResultRow ashiptype = Ships.getShipType(aship);
				if( Ships.hasShipTypeFlag(ashiptype, Ships.SF_DROHNEN_CONTROLLER) ) {
					gotone = true;
					break;	
				}
			}
		}
		else {
			gotone = true;	
		}
		
		if( !gotone ) {
			battle.logme( "Sie ben&ouml;tigen ein Drohnen-Kontrollschiff um feuern zu k&ouml;nnen\n" );
			return RESULT_ERROR;
		}
		
		if( weapons <= 0 ) {
			battle.logme( "Das Schiff verf&uuml;gt nicht &uuml;ber die von ihnen gew&auml;hlte Waffe ("+weaponName+")\n" );
			return RESULT_ERROR;
		}
	
		if( this.ownShip.getInt("e") < this.weapon.getECost()*weapons ) {
			battle.logme( "Nicht genug Energie um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
			return RESULT_ERROR;
		}
	
		if( (this.ownShip.getString("docked").length() > 0) && (this.ownShip.getString("docked").charAt(0) == 'l') ) {
			battle.logme( "Sie k&ouml;nnen nicht mit gelandeten Schiffen feuern\n" );
			return RESULT_ERROR;
		}
		
		/*
		 * 	Offiziersdaten ermitteln
		 */
		Offizier offizier = Offizier.getOffizierByDest('s', this.ownShip.getInt("id"));
		
		int oldenemyship = battle.getEnemyShipIndex();
		
		boolean breakFlag = false;
	
		db.tBegin();
		
		// Die auessere Schleife laeuft ueber die generischen Schiffe
		// Die innere Scheife feuernt n Mal auf das gerade ausgewaehlte gegnerische Schiff
		
		for( int outerloop=0; outerloop < nextShipLoop; outerloop++ ) {
			// Nun das gegnerische Schiff laden und checken
			this.enemyShip = battle.getEnemyShip();
						
			for( int innerloop=0; innerloop < sameShipLoop; innerloop++ ) {
				if( (outerloop > 0) || (innerloop > 0) ) {
					battle.logme("\n[HR]");
					battle.logenemy("\n");
				}
		
				SQLResultRow enemyShipType = Ships.getShipType(this.enemyShip);
				
				/*
				 * 	Die konkreten Waffendaten ermitteln
				 */
				SQLResultRow localweapon = null;
				
				if( !this.weapon.getAmmoType().equals("none") ) {
					localweapon = this.getAmmoBasedWeaponData( battle );
					if( (localweapon == null) || localweapon.isEmpty() ) {
						breakFlag = true;
						break;
					}
				} 
				else {
					localweapon = this.getWeaponData( battle );
					if( (localweapon == null) || localweapon.isEmpty() ) {
						breakFlag = true;
						break;
					}
				}
				
				localweapon.put("count", weapons);
				
				this.localweapon = localweapon;
				
				if( (this.ownShip.getInt("action") & Battle.BS_SECONDROW) != 0 && 
					!this.localweapon.getBoolean("long_range") ) {
					battle.logme( this.weapon.getName()+" haben nicht die notwendige Reichweite, um aus der zweiten Reihe heraus abgefeuert zu werden\n" );
					breakFlag = true;
					break;
				}
				
				battle.logme( "Ziel: "+Battle.log_shiplink(this.enemyShip)+"\n" );
				
				if( heat + weapons > maxheat ) {
					battle.logme( this.weapon.getName()+" k&ouml;nnen nicht abgefeuert werden, da diese sonst &uuml;berhitzen w&uuml;rden\n" );
					breakFlag = true;
					break;
				}
				
				if( this.ownShip.getInt("e") < this.weapon.getECost()*weapons ) {
					battle.logme( "Nicht genug Energie um mit der Waffe "+this.weapon.getName()+" zu feuern\n" );
					breakFlag = true;
					break;
				}
					
				if( (this.enemyShip.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
					battle.logme( "Das angegebene Ziel ist bereits zerst&ouml;rt\n" );
					breakFlag = true;
					break;
				}
				
				if( (this.enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 && !Ships.hasShipTypeFlag(ownShipType, Ships.SF_ABFANGEN) ) {
					battle.logme( "Ihr Schiff kann keine fl&uuml;chtenden Schiffe abfangen\n" );
					breakFlag = true;
					break;
				}
				
				if( (this.enemyShip.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
					battle.logme( "Das angegebene Ziel wird vom Gegner abgeschirmt und ist daher nicht erreichbar\n" );
					breakFlag = true;
					break;
				}
				
				if( (this.enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 && (apcost + Math.ceil(this.weapon.getAPCost()*0.5)*this.attCountForShip(this.ownShip, ownShipType, this.attcount) > battle.getPoints(battle.getOwnSide()) ) ) {
					battle.logme( "Sie haben nicht genug Aktionspunkte um mit dieser Waffe auf das fl&uuml;chtende Schiff zu feuern\n" );
					breakFlag = true;
					break;
				}
			
				if( (this.enemyShip.getInt("action") & Battle.BS_JOIN) != 0 ) {
					battle.logme( "Sie k&ouml;nnen nicht auf einem Schiff feuern, dass gerade erst der Schlacht beitritt\n" );
					breakFlag = true;
					break;
				}
				
				/*
				 * 	Anti-Torp-Verteidigungswerte ermitteln
				 */
				int fighterdef = 0;
				int antitorptrefferws = 0;
					
				if( this.localweapon.getDouble("destroyable") > 0 ) {
					antitorptrefferws = this.getAntiTorpTrefferWS( enemyShipType );
					battle.logme("AntiTorp-TrefferWS: "+antitorptrefferws+"%\n");
					
					if( enemyShipType.getInt("size") > 3 ) {
						fighterdef = this.getFighterDefense(battle);
						if( fighterdef > 0 ) {
							battle.logme("Verteidigung durch Schiffe: "+fighterdef+"%\n");	
						}	
					}
				}
				
				/*
				 * 	Offiziersdaten ermitteln
				 */
				Offizier eOffizier = Offizier.getOffizierByDest('s', this.enemyShip.getInt("id"));
				
				ownShipType = Ships.getShipType(this.ownShip);
				this.weapon.calcShipTypes(ownShipType, enemyShipType);
			
				int offensivskill = this.getOffensivSkill( ownShipType, offizier );
				int navskill = this.getNavSkill( battle, ownShipType, offizier );
				int defensivskill = this.getDefensivSkill( enemyShipType, eOffizier );
			
				battle.logme( "Offensivskill: "+offensivskill+"\n" );
				
				/*
				 * 	Schadenswerte, Panzerung & TrefferWS ermitteln
				 */
				int absSchaden = this.getDamage(this.localweapon.getInt("basedamage"), offensivskill, enemyShipType);
				int shieldSchaden = this.getDamage(this.localweapon.getInt("shielddamage"), offensivskill, enemyShipType);
				
				int panzerung = (int)Math.round(enemyShipType.getInt("panzerung")*this.enemyShip.getInt("hull")/(double)enemyShipType.getInt("hull"));
				int schaden = absSchaden;
			
				int trefferWS = 0;
				if( enemyShipType.getInt("size") < 4 ) {
					trefferWS = this.getSmallTrefferWS( battle, this.localweapon.getInt("deftrefferws"), this.enemyShip, enemyShipType, defensivskill, navskill );
				} 
				else {
					trefferWS = this.getTrefferWS( battle, this.localweapon.getInt("deftrefferws"), this.enemyShip, enemyShipType, defensivskill, navskill );
				}
				
				battle.logme( "Basis-TrefferWS: "+trefferWS+"%\n");
				
				trefferWS -= antitorptrefferws;
				// Minimum bei 5% bei zerstoerbaren Waffen
				if( (trefferWS - fighterdef < 5) && (fighterdef > 0) ) {
					trefferWS = 5;
				} 
				else {
					trefferWS -= fighterdef;
				}
				battle.logme( "TrefferWS: "+trefferWS+"%\n" );
			
				int[] subdmgs = null;
				
				/*
				 * 	Subsystem-Schaden, falls notwendig, berechnen
				 */
				if( this.localweapon.getInt("subdamage") > 0 ) {
					int subWS = this.getTrefferWS( battle, this.localweapon.getInt("subws"), this.enemyShip, enemyShipType, defensivskill, navskill );
					battle.logme( "SubsystemTWS: "+subWS+"%\n" );
			
					int subPanzerung = panzerung;
					if( subPanzerung > 10 ) {
						subPanzerung = 10;
						battle.logme("Panzerung absorbiert Subsystemschaden\n");
					} 
					else if( subPanzerung > 0 ) {
						battle.logme("Panzerung reduziert Subsystemschaden ("+(subPanzerung*10)+"%)\n");
					}
					
					subdmgs = this.getSubDamages( subPanzerung, trefferWS, subWS, 1);
				} 
			
				if( schaden < 0 ) {
					schaden = 0;
				}
			
				if( firstentry ) {
					battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
					firstentry = false;
				}
				
				/*
				 * 	Treffer berechnen
				 */
				int hit = 0;
				int def = 0;
				for( int i=1; i <= this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"); i++) {
					int rnd = rand.nextInt(101);
					if( rnd <= trefferWS ) {
						hit++;
					}
					if( (rnd > trefferWS) && (rnd <= trefferWS+fighterdef) && (this.localweapon.getDouble("destroyable") > 0) ) {
						def++;
					}
				}
				battle.logme( this.weapon.getName()+": "+hit+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen haben getroffen\n" );
				battle.logenemy( Battle.log_shiplink(this.ownShip)+" feuert auf "+Battle.log_shiplink(this.enemyShip)+"\n+ Waffe: "+this.localweapon.getString("name")+"\n" );
				if( this.localweapon.getDouble("destroyable") > 0 && (def != 0) ) {
					battle.logme( this.weapon.getName()+": "+def+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen wurden abgefangen\n" );
					battle.logenemy( "+ "+this.weapon.getName()+": "+def+" von "+(this.localweapon.getInt("count")*this.localweapon.getInt("shotsPerShot"))+" Sch&uuml;ssen wurden abgefangen\n" );
				}
			
				boolean savedamage = this.calcDamage( battle, this.enemyShip, enemyShipType, hit, shieldSchaden, schaden, subdmgs, "" );
			
				/*
				 *  Areadamage - falls notwendig - berechnen
				 */
				if( (this.localweapon.getInt("areadamage") != 0) && (hit != 0) ) {
					List<SQLResultRow> areashiplist = this.getADShipList(battle);

					// In der $areashiplist ist das aktuell ausgewaehlte Schiff immer in der Mitte (abgerundet)
					int targetindex = areashiplist.size()/2;  
					
					// schaden anwenden
					int damagemod = 0;
					
					if( !this.localweapon.getBoolean("ad_full") ) {
						damagemod = 1 / (this.localweapon.getInt("areadamage")+1);
					}
					
					for( int i=1; i <= this.localweapon.getInt("areadamage"); i++ ) {
						// Es kann sein, dass die Liste nicht vollstaendig gefuellt ist (leere Elemente, Arrays mit Laenge 0).
						// Diese muessen wir jetzt rausfiltern
						if( (targetindex-i >= 0) && !areashiplist.get(targetindex-i).isEmpty() ) {
							SQLResultRow aeShip = areashiplist.get(targetindex-i);
							
							this.calcADStep(battle, trefferWS, navskill, aeShip, hit, schaden, shieldSchaden, 1-i*damagemod);
						}
						if( (targetindex+i < areashiplist.size()) && !areashiplist.get(targetindex+i).isEmpty() ) {
							SQLResultRow aeShip = areashiplist.get(targetindex+i);
							
							this.calcADStep(battle, trefferWS, navskill, aeShip, hit, schaden, shieldSchaden, 1-i*damagemod);
						}		
					}
				}
				
				/*
				 * 	E, AP, Muni usw in die DB schreiben
				 */
				heat += this.localweapon.getInt("count");
				this.ownShip.put("e", this.ownShip.getInt("e") - this.weapon.getECost()*this.localweapon.getInt("count"));
				
				if( (this.enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 )  { 
					apcost += (int)Math.ceil(this.weapon.getAPCost()*1.5)*this.attCountForShip(this.ownShip, ownShipType, this.attcount);
				} 
				else {
					apcost += this.weapon.getAPCost()*this.attCountForShip(this.ownShip, ownShipType, this.attcount);	
				}
				
				if( !this.weapon.getAmmoType().equals("none") ) {
					Cargo mycargo = new Cargo( Cargo.Type.STRING, this.ownShip.getString("cargo") );
					mycargo.substractResource( ((ItemCargoEntry)this.localweapon.get("ammoitem")).getResourceID(), this.localweapon.getInt("count")*this.weapon.getSingleShots() );
					this.ownShip.put("cargo", mycargo.save());
				}
				
				heatList.put(weaponName, Integer.toString(heat));
				this.ownShip.put("heat", Weapons.packWeaponList(heatList));
				
				
				/*
				 *  BETAK - Check
				 */
				if( battle.getBetakStatus(battle.getOwnSide()) && (enemyShipType.getInt("military") == 0) ) {
					battle.setBetakStatus(battle.getOwnSide(), false);
					battle.logme("[color=red][b]Sie haben die BETAK-Konvention verletzt[/b][/color]\n\n");
					battle.logenemy("[color=red][b]Die BETAK-Konvention wurde verletzt[/b][/color]\n\n");
				}
			
				/*
				 *	Schiff falls notwendig zerstoeren
				 */
				if( !savedamage && (Configuration.getIntSetting("DESTROYABLE_SHIPS") != 0) ) {
					this.destroyShip(user.getID(), battle, this.enemyShip, true);
				}
				
				/*
				 * 	Wenn das angreifende Schiff auch zerstoert werden muss tun wir das jetzt mal
				 */
				if( this.localweapon.getBoolean("destroyAfter") ) {
					battle.logme( "[color=red]+ Angreifer zerst&ouml;rt[/color]\n" );
					battle.logenemy( "[color=red]+ Angreifer zerst&ouml;rt[/color]\n" );
					
					if( Configuration.getIntSetting("DESTROYABLE_SHIPS") == 0 ) {
						this.ownShip.put("type", Configuration.getIntSetting("CONFIG_TRUEMMER"));
						this.ownShip.put("hull", Configuration.getIntSetting("CONFIG_TRUEMMER_HUELLE"));
					}
					else {
						this.destroyShipOnly(user.getID(), battle, this.ownShip, false, false);
						// Unschoen...ich weiss
						this.ownShipBackup.put("action", this.ownShip.getInt("action"));
						breakFlag = true;
						break;
					}
				}
			}
			
			if( outerloop < nextShipLoop - 1) {
				battle.setEnemyShipIndex(battle.getNewTargetIndex());
			}
			
			if( breakFlag ) {
				break;
			}
		}
		
		if( !Weapons.packWeaponList(heatList).equals(this.ownShipBackup.getString("heat")) || (this.ownShip.getInt("e") != this.ownShipBackup.getInt("e")) || this.ownShip.getBoolean("battleAction") ) {
			db.tUpdate(1, "UPDATE ships SET heat='"+Weapons.packWeaponList(heatList)+"',e="+this.ownShip.getInt("e")+",battleAction=1 WHERE id>0 AND id="+this.ownShip.getInt("id")+" AND heat='"+this.ownShipBackup.getString("heat")+"' AND e="+this.ownShipBackup.getInt("e"));
		}
		
		this.ownShip.put("action", this.ownShip.getInt("action") | Battle.BS_SHOT);
		if( this.ownShip.getInt("action") != this.ownShipBackup.getInt("action") ) {
			db.tUpdate(1, "UPDATE battles_ships SET action=",this.ownShip.getInt("action")," WHERE shipid=",this.ownShip.getInt("id")," AND action=",this.ownShipBackup.getInt("action"));
		}
		
		if( !this.ownShip.getString("cargo").equals(this.ownShipBackup.getString("cargo")) ) {
			db.tUpdate(1, "UPDATE ships SET cargo='",this.ownShip.getString("cargo"),"' WHERE id>0 AND id=",this.ownShip.getInt("id")," AND cargo='",this.ownShipBackup.getString("cargo"),"'");	
		}
		
		if( !firstentry ) {
			battle.logenemy("]]></action>\n");
		}
		
		if( (battle.getEnemyShip(oldenemyship).getInt("action") & Battle.BS_DESTROYED) == 0 ) {
			battle.setEnemyShipIndex(oldenemyship);	
		}
		
		battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide()) - apcost);
		
		battle.save(false);
		
		this.ownShip.put("status", Ships.recalculateShipStatus(this.ownShip.getInt("id")));
		
		if( !db.tCommit() ) {
			battle.logme("\n[color=red]FEHLER: Transaktion nicht erfolgreich - Bitte feuern sie erneut die Waffen ab[/color]\n");
			battle.logenemy("\n[color=red]FEHLER: Transaktion nicht erfolgreich[/color]\n");
			return RESULT_ERROR;	
		}
		
		return RESULT_OK;
	}
}
