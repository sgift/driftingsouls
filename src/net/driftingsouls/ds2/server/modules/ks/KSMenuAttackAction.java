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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Das Auswahlmenue fuer den Feuermodus und die abzufeuernde Waffe
 * @author Christopher Jung
 *
 */
public class KSMenuAttackAction extends BasicKSMenuAction {
	private static final Map<String,String> ATTMODES = new HashMap<String,String>();
	private static final Map<String,String> NEXTATTMODES = new HashMap<String,String>();
	
	static {
		ATTMODES.put("single", "Einzelsalve");
		ATTMODES.put("alphastrike", "Mehrfachsalve (5)");
		ATTMODES.put("strafe", "Sperrfeuer (5)");
		ATTMODES.put("alphastrike_max", "Mehrfachsavle (max)");
		ATTMODES.put("strafe_max", "Sperrfeuer (max)");
		
		NEXTATTMODES.put("single", "alphastrike");
		NEXTATTMODES.put("alphastrike", "strafe");
		NEXTATTMODES.put("strafe", "alphastrike_max");
		NEXTATTMODES.put("alphastrike_max", "strafe_max");
		NEXTATTMODES.put("strafe_max", "single");
	}
	
	@Override
	public int validate(Battle battle) {
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
	
		if( (ownShip.getInt("action") & Battle.BS_JOIN) != 0 ) {
			return RESULT_ERROR;
		}
		
		if( (enemyShip.getInt("action") & Battle.BS_JOIN) != 0 ) {
			return RESULT_ERROR;
		}
		
		if( (enemyShip.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
			return RESULT_ERROR;
		}
		if( (ownShip.getString("docked").length() > 0) && ownShip.getString("docked").charAt(0) == 'l' ) {
			return RESULT_ERROR;
		}
		
		if( (ownShip.getInt("action") & Battle.BS_FLUCHT) != 0 ) {
			return RESULT_ERROR;
		}
		if( (enemyShip.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
			return RESULT_ERROR;
		}
		
		SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
		
		if( (enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 &&	!ShipTypes.hasShipTypeFlag(ownShipType, ShipTypes.SF_ABFANGEN) ) {
			return RESULT_ERROR;
		}
		
		boolean gotone = true;			
		if( ShipTypes.hasShipTypeFlag(ownShipType, ShipTypes.SF_DROHNE) ) {
			gotone = false;
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				SQLResultRow ashiptype = ShipTypes.getShipType(aship);
				if( ShipTypes.hasShipTypeFlag(ashiptype, ShipTypes.SF_DROHNEN_CONTROLLER) ) {
					gotone = true;
					break;	
				}
			}
		}
						
		if( !gotone ) {
			return RESULT_ERROR;
		}
		
		return RESULT_OK;	
	}
	
	private String getAttMode() {
		Context context = ContextMap.getContext();
		String attmode = context.getRequest().getParameterString("attmode");
		
		String userattmode = "";
		
		if( attmode.length() == 0 ) {
			User user = (User)context.getActiveUser();
			attmode = userattmode = user.getUserValue("TBLORDER/ks/attackmode");
		}
		
		if( !ATTMODES.containsKey(attmode) ) {
			attmode = "single";
		}
		
		if( !attmode.equals(userattmode) ) {
			User user = (User)context.getActiveUser();
			user.setUserValue("TBLORDER/ks/attackmode", attmode);
		}
		
		return attmode;
	}
	
	private int getAttCount() {
		Context context = ContextMap.getContext();
		int attcount = context.getRequest().getParameterInt("attcount");
		
		int userattcount = 0;
		
		if( attcount == 0 ) {
			User user = (User)context.getActiveUser();
			String attCountStr = user.getUserValue("TBLORDER/ks/attackcount");
			attcount = userattcount = (attCountStr.length() > 0 ? Integer.parseInt(attCountStr) : 0);
		}
		
		if( attcount <= 0 || attcount > 3 ) {
			attcount = 3;
		}
		
		if( attcount != userattcount ) {
			User user = (User)context.getActiveUser();
			user.setUserValue("TBLORDER/ks/attackcount", Integer.toString(attcount));
		}
		
		return attcount;	
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
	
	private int nextAttCount( int attcount ) {
		if( attcount == 3 ) {
			attcount = 1;
		}
		else {
			attcount++;
		}
		return attcount;
	}
	
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		String attmode = this.getAttMode();
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		if( (ownShip.getInt("action") & Battle.BS_BLOCK_WEAPONS) != 0 ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( (ownShip.getInt("action") & Battle.BS_DISABLE_WEAPONS) != 0 ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Validation failed\n" );
			return RESULT_ERROR;
		}
		
		SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
		SQLResultRow enemyShipType = ShipTypes.getShipType(enemyShip);
				
		int attcount = this.getAttCount();		
					  
		int apmulti = 1;
		if( attmode.equals("alphastrike") || attmode.equals("strafe") ) {
			apmulti = 5;	
		}
							
		menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Feuermodus: "+ATTMODES.get(attmode)+"<br /> "+
							"<span style=\"font-size:12px\">&lt; Klicken um Feuermodus zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
							"ship",		ownShip.getInt("id"),
						 	"attack",	enemyShip.getInt("id"),
						 	"ksaction",	"attack",
						 	"attmode",	NEXTATTMODES.get(attmode),
						 	"attcount",	attcount );
						 			
		if( ownShip.getInt("count") > 1 ) {
			this.menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Schiffsanzahl: "+this.attCountForShip(ownShip, ownShipType, attcount)+"<br /> "+
							"<span style=\"font-size:12px\">&lt; Klicken um Anzahl zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
							"ship",		ownShip.getInt("id"),
						 	"attack",	enemyShip.getInt("id"),
						 	"ksaction",	"attack",
						 	"attmode",	attmode,
						 	"attcount",	nextAttCount(attcount) );
		}
						 			
	
		String ask = "";
		if( battle.getBetakStatus(battle.getOwnSide()) && enemyShipType.getInt("military") == 0 ) {
			ask = "Wenn sie auf das gew&auml;hlte Ziel feuern, versto&szlig;en sie gegen die BETAK-Konvention. Wollen sie dies wirklich tun?";
		} 

		Map<String,String> currentheatlist = Weapons.parseWeaponList(ownShip.getString("heat"));

		Map<String,String> heatlist = Weapons.parseWeaponList(ownShipType.getString("maxheat"));
		Map<String,String> weaponlist = Weapons.parseWeaponList(ownShipType.getString("weapons"));

		for( String wpnname : weaponlist.keySet() ) {
			int wpncount = Integer.parseInt(weaponlist.get(wpnname));
			
			if( wpncount == 0 ) {
				continue;
			}
			
			Weapon wpn = Weapons.get().weapon(wpnname);
			
			wpncount = (int)(wpncount/(double)ownShipType.getInt("shipcount")*this.attCountForShip(ownShip, ownShipType, attcount));
				
			if( (ownShip.getInt("action") & Battle.BS_SECONDROW) != 0 &&
				!wpn.hasFlag(Weapon.Flags.LONG_RANGE) ) {
				continue;
			}

			String schaden = "";
			if( !wpn.getAmmoType().equals("none") ) {
				schaden = "Munitionstyp";
			}
			else {
				schaden = wpn.getBaseDamage(ownShipType)+"/Schuss";
			}
			
			if( attmode.equals("alphastrike_max") || attmode.equals("strafe_max") ) {
				int maxheat = Integer.parseInt(heatlist.get(wpnname));
				if( currentheatlist.containsKey(wpnname) ) {
					maxheat -= Integer.parseInt(currentheatlist.get(wpnname));
				}
				apmulti = (int)(maxheat/(double)Integer.parseInt(weaponlist.get(wpnname)));
				if( apmulti < 1 ) {
					apmulti = 1;	
				}
			}
			
			// TODO: ap an attcount anpassen
			apmulti *= this.attCountForShip(ownShip, ownShipType, attcount);
			
			int apcost = 0;
			if( (enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 ) { 
				apcost = (int)Math.ceil(wpn.getAPCost()*1.5d)*apmulti;
			} 
			else {
				apcost = wpn.getAPCost()*apmulti;
			}
	
			String action = "attack2";
			if(wpn.hasFlag(Weapon.Flags.AMMO_SELECT) ) {
				action = "attack_select";
			}
			
			if( ask.length() != 0 ) {
				menuEntryAsk( wpncount+" "+wpn.getName()+"<br /><span style=\"font-weight:normal;font-size:14px\"> "+
							  		"Schaden: "+schaden+"<br /> "+
							 		"Kosten: n*"+wpn.getECost()+"E "+apcost+"AP</span>",
							 		new Object[] {	
											"ship",		ownShip.getInt("id"),
						 					"attack",	enemyShip.getInt("id"),
						 					"ksaction",	action,
						 					"weapon",	wpnname, 
						 					"attmode",	attmode,
						 					"attcount",	attcount },
						 			ask);
			}
			else {
				menuEntry( wpncount+" "+wpn.getName()+"<span style=\"font-weight:normal;font-size:14px\"><br /> "+
							  		"Schaden: "+schaden+"<br /> "+
							 		"Kosten: n*"+wpn.getECost()+"E "+apcost+"AP</span>",
							 		"ship",		ownShip.getInt("id"),
						 			"attack",	enemyShip.getInt("id"),
						 			"ksaction",	action,
						 			"weapon",	wpnname, 
						 			"attmode",	attmode,
						 			"attcount",	attcount );
			}
		}
	
		this.menuEntry("zur&uuml;ck",	"ship",		ownShip.getInt("id"),
										"attack",	enemyShip.getInt("id") );

		return RESULT_OK;		
	}
}
