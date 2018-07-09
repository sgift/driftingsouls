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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Das Auswahlmenue fuer den Feuermodus und die abzufeuernde Waffe.
 *
 */
public class KSMenuGroupAttackAction extends BasicKSMenuAction {
	private static final Map<String,String> ATTMODES = new HashMap<>();
	private static final Map<String,String> NEXTATTMODES = new HashMap<>();
	
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
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
	
		if( ownShip.hasFlag(BattleShipFlag.JOIN) ) {
			return Result.ERROR;
		}
		
		/*
		if( (enemyShip.getAction() & Battle.BS_JOIN) != 0 ) {
			return Result.ERROR;
		}
		*/
		
		if( enemyShip.hasFlag(BattleShipFlag.DESTROYED) ) {
			return Result.ERROR;
		}
		
		/*
		if( (ownShip.getDocked().length() > 0) && ownShip.getDocked().charAt(0) == 'l' ) {
			return Result.ERROR;
		}
		*/
		
		if( ownShip.hasFlag(BattleShipFlag.FLUCHT) ) {
			return Result.ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( enemyShip.hasFlag(BattleShipFlag.FLUCHT) && !ownShipType.hasFlag(ShipTypeFlag.ABFANGEN) ) {
			return Result.ERROR;
		}
		
		boolean gotone = true;			
		if( ownShipType.hasFlag(ShipTypeFlag.DROHNE) ) {
			gotone = false;
			List<BattleShip> ownShips = battle.getOwnShips();
			for (BattleShip aship : ownShips)
			{
				ShipTypeData ashiptype = aship.getTypeData();
				if (ashiptype.hasFlag(ShipTypeFlag.DROHNEN_CONTROLLER))
				{
					gotone = true;
					break;
				}
			}
		}
						
		if( !gotone ) {
			return Result.ERROR;
		}
		
		return Result.OK;	
	}
	
	private String getAttMode() {
		Context context = ContextMap.getContext();
		String attmode = context.getRequest().getParameterString("attmode");
		
		String userattmode = "";
		
		if( attmode.length() == 0 ) {
			User user = (User)context.getActiveUser();
			attmode = userattmode = user.getUserValue(WellKnownUserValue.TBLORDER_KS_ATTACKMODE);
		}
		
		if( !ATTMODES.containsKey(attmode) ) {
			attmode = "single";
		}
		
		if( !attmode.equals(userattmode) ) {
			User user = (User)context.getActiveUser();
			user.setUserValue(WellKnownUserValue.TBLORDER_KS_ATTACKMODE, attmode);
		}
		
		return attmode;
	}
	
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		String attmode = this.getAttMode();
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		if( ownShip.hasFlag(BattleShipFlag.BLOCK_WEAPONS) ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		if( ownShip.hasFlag(BattleShipFlag.DISABLE_WEAPONS) ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		if( this.validate(battle) != Result.OK ) {
			battle.logme( "Validation failed\n" );
			return Result.ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		ShipTypeData enemyShipType = enemyShip.getTypeData();
									
		menuEntry( t, "<span style=\"font-size:3px\">&nbsp;<br /></span>Feuermodus: "+ATTMODES.get(attmode)+"<br /> "+
							"<span style=\"font-size:12px\">&lt; Klicken um Feuermodus zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
							"ship",		ownShip.getId(),
						 	"attack",	enemyShip.getId(),
						 	"ksaction",	"groupattack",
						 	"attmode",	NEXTATTMODES.get(attmode));			 			
	
		String ask = "";
		if( battle.getBetakStatus(battle.getOwnSide()) && !enemyShipType.isMilitary() ) {
			ask = "Wenn sie auf das gew&auml;hlte Ziel feuern, versto&szlig;en sie gegen die BETAK-Konvention. Wollen sie dies wirklich tun?";
		} 

		Map<String,Integer> weaponlist = ownShipType.getWeapons();

		for( Map.Entry<String, Integer> entry: weaponlist.entrySet() ) {
			String weaponname = entry.getKey();
			int weaponcount = entry.getValue();
			
			if( weaponcount == 0 ) {
				continue;
			}
			
			Weapon weapon = Weapons.get().weapon(weaponname);
			
			if( ownShip.hasFlag(BattleShipFlag.SECONDROW) &&
				!weapon.hasFlag(Weapon.Flags.LONG_RANGE) &&
				!weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
				continue;
			}
			
			if( enemyShip.hasFlag(BattleShipFlag.SECONDROW) &&
				!weapon.hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
				continue;
			}

			String schaden;
			if( !weapon.getMunitionstypen().isEmpty() ) {
				schaden = "Munitionstyp";
			}
			else {
				schaden = weapon.getBaseDamage()+"/Schuss";
			}

			String action = "groupattack2";
			if(weapon.hasFlag(Weapon.Flags.AMMO_SELECT) ) {
				action = "groupattack_select";
			}
			
			if( ask.length() != 0 ) {
				menuEntryAsk( t, weaponcount+" "+weapon.getName()+"<br /><span style=\"font-weight:normal;font-size:14px\"> "+
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
				menuEntry( t, weaponcount+" "+weapon.getName()+"<span style=\"font-weight:normal;font-size:14px\"><br /> "+
							  		"Schaden: "+schaden+"<br /> "+
							 		"Kosten: n*"+weapon.getECost()+"Energie</span>",
							 		"ship",		ownShip.getId(),
						 			"attack",	enemyShip.getId(),
						 			"ksaction",	action,
						 			"weapon",	weaponname, 
						 			"attmode",	attmode);
			}
		}
	
		this.menuEntry(t, "zur&uuml;ck",	"ship",		ownShip.getId(),
										"attack",	enemyShip.getId() );

		return Result.OK;		
	}
}
