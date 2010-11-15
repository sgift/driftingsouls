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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Das Auswahlmenue fuer den Feuermodus und die abzufeuernde Waffe.
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
		ATTMODES.put("alphastrike_max", "Mehrfachsalve (max)");
		ATTMODES.put("strafe_max", "Sperrfeuer (max)");
		
		NEXTATTMODES.put("single", "alphastrike");
		NEXTATTMODES.put("alphastrike", "strafe");
		NEXTATTMODES.put("strafe", "alphastrike_max");
		NEXTATTMODES.put("alphastrike_max", "strafe_max");
		NEXTATTMODES.put("strafe_max", "single");
	}
	
	@Override
	public int validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
	
		if( (ownShip.getAction() & Battle.BS_JOIN) != 0 ) {
			return RESULT_ERROR;
		}
		
		/*
		if( (enemyShip.getAction() & Battle.BS_JOIN) != 0 ) {
			return RESULT_ERROR;
		}
		*/
		
		if( (enemyShip.getAction() & Battle.BS_DESTROYED) != 0 ) {
			return RESULT_ERROR;
		}
		
		/*
		if( (ownShip.getDocked().length() > 0) && ownShip.getDocked().charAt(0) == 'l' ) {
			return RESULT_ERROR;
		}
		*/
		
		if( (ownShip.getAction() & Battle.BS_FLUCHT) != 0 ) {
			return RESULT_ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( (enemyShip.getAction() & Battle.BS_FLUCHT) != 0 &&	!ownShipType.hasFlag(ShipTypes.SF_ABFANGEN) ) {
			return RESULT_ERROR;
		}
		
		boolean gotone = true;			
		if( ownShipType.hasFlag(ShipTypes.SF_DROHNE) ) {
			gotone = false;
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				BattleShip aship = ownShips.get(i);
				ShipTypeData ashiptype = aship.getTypeData();
				if( ashiptype.hasFlag(ShipTypes.SF_DROHNEN_CONTROLLER) ) {
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
	
	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		String attmode = this.getAttMode();
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		if( (ownShip.getAction() & Battle.BS_BLOCK_WEAPONS) != 0 ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_DISABLE_WEAPONS) != 0 ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Validation failed\n" );
			return RESULT_ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		ShipTypeData enemyShipType = enemyShip.getTypeData();
									
		menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Feuermodus: "+ATTMODES.get(attmode)+"<br /> "+
							"<span style=\"font-size:12px\">&lt; Klicken um Feuermodus zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
							"ship",		ownShip.getId(),
						 	"attack",	enemyShip.getId(),
						 	"ksaction",	"attack",
						 	"attmode",	NEXTATTMODES.get(attmode));			 			
	
		String ask = "";
		if( battle.getBetakStatus(battle.getOwnSide()) && !enemyShipType.isMilitary() ) {
			ask = "Wenn sie auf das gew&auml;hlte Ziel feuern, versto&szlig;en sie gegen die BETAK-Konvention. Wollen sie dies wirklich tun?";
		} 

		Map<String,String> currentheatlist = Weapons.parseWeaponList(ownShip.getWeaponHeat());

		Map<String,String> heatlist = Weapons.parseWeaponList(ownShipType.getMaxHeat());
		Map<String,String> weaponlist = Weapons.parseWeaponList(ownShipType.getWeapons());

		for( Map.Entry<String, String> entry: weaponlist.entrySet() ) {
			String weaponname = entry.getKey();
			int weaponcount = Integer.parseInt(entry.getValue());
			
			if( weaponcount == 0 ) {
				continue;
			}
			
			Weapon weapon = Weapons.get().weapon(weaponname);
			
			if( (ownShip.getAction() & Battle.BS_SECONDROW) != 0 &&
				!weapon.hasFlag(Weapon.Flags.LONG_RANGE) &&
				!weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
				continue;
			}
			
			if( (enemyShip.getAction() & Battle.BS_SECONDROW) != 0 && 
				!weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
				continue;
			}

			String schaden = "";
			if( weapon.getAmmoType().length > 0 ) {
				schaden = "Munitionstyp";
			}
			else {
				schaden = weapon.getBaseDamage(ownShipType)+"/Schuss";
			}
			
			if( attmode.equals("alphastrike_max") || attmode.equals("strafe_max") ) {
				int maxheat = Integer.parseInt(heatlist.get(weaponname));
				if( currentheatlist.containsKey(weaponname) ) {
					maxheat -= Integer.parseInt(currentheatlist.get(weaponname));
				}
			}
			
			String action = "attack2";
			if(weapon.hasFlag(Weapon.Flags.AMMO_SELECT) ) {
				action = "attack_select";
			}
			
			if( ask.length() != 0 ) {
				menuEntryAsk( weaponcount+" "+weapon.getName()+"<br /><span style=\"font-weight:normal;font-size:14px\"> "+
							  		"Schaden: "+schaden+"<br /> "+
							 		"Kosten: n*"+weapon.getECost()+"Energie</span>",
							 		new Object[] {	
											"ship",		ownShip.getId(),
						 					"attack",	enemyShip.getId(),
						 					"ksaction",	action,
						 					"weapon",	weaponname, 
						 					"attmode",	attmode, },
						 			ask);
			}
			else {
				menuEntry( weaponcount+" "+weapon.getName()+"<span style=\"font-weight:normal;font-size:14px\"><br /> "+
							  		"Schaden: "+schaden+"<br /> "+
							 		"Kosten: n*"+weapon.getECost()+"Energie</span>",
							 		"ship",		ownShip.getId(),
						 			"attack",	enemyShip.getId(),
						 			"ksaction",	action,
						 			"weapon",	weaponname, 
						 			"attmode",	attmode);
			}
		}
	
		this.menuEntry("zur&uuml;ck",	"ship",		ownShip.getId(),
										"attack",	enemyShip.getId() );

		return RESULT_OK;		
	}
}
