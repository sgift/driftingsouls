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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.Munition;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.entities.Munitionsdefinition;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.*;

/**
 * Laesst den Benutzer fuer eine ammobasierte Waffe die gewuenschte Munition auswaehlen (sofern
 * dies fuer die Waffe erlaubt wurde).
 * @author Christopher Jung
 *
 */
public class KSMenuGroupAttackMuniSelectAction extends BasicKSMenuAction {
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
		
		if( enemyShip.hasFlag(BattleShipFlag.JOIN) ) {
			return Result.ERROR;
		}
		
		if( enemyShip.hasFlag(BattleShipFlag.DESTROYED) ) {
			return Result.ERROR;
		}
		if(ownShip.getShip().isLanded()) 
		{
			return Result.ERROR;
		}
		
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
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		final String weapon = context.getRequest().getParameterString("weapon");
		
		String attmode = this.getAttMode();
		
		if( ownShip.hasFlag(BattleShipFlag.SECONDROW) &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.LONG_RANGE) &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
			battle.logme("Diese Waffe hat nicht die notwendige Reichweite um aus der zweiten Reihe heraus abgefeuert zu werden\n");
			return Result.ERROR;	
		}
		
		if( enemyShip.hasFlag(BattleShipFlag.SECONDROW) &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.VERY_LONG_RANGE)	) {
			battle.logme("Diese Waffe hat nicht die notwendige Reichweite um in die zweiten Reihe des Gegners abgefeuert zu werden\n");
			return Result.ERROR;
		}
		
		if( ownShip.hasFlag(BattleShipFlag.BLOCK_WEAPONS) ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		if( ownShip.hasFlag(BattleShipFlag.DISABLE_WEAPONS) ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		/*
		 * 	Feuermodusauswahl
		 */
									
		menuEntry( t, "<span style=\"font-size:3px\">&nbsp;<br /></span>Feuermodus: "+ATTMODES.get(attmode)+"<br /> "+
				"<span style=\"font-size:12px\">&lt; Klicken um Feuermodus zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
				"ship",		ownShip.getId(),
			 	"attack",	enemyShip.getId(),
			 	"ksaction",	"groupattack",
			 	"attmode",	NEXTATTMODES.get(attmode));

		/*
		 *  Ammoauswahl
		 */

		if( Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.AMMO_SELECT) ) {
			Set<Integer> ammoids = new HashSet<>();

			Iterator<?> ammoIter = db.createQuery("from Munitionsdefinition " +
					"where type in ('"+Common.implode("','", Weapons.get().weapon(weapon).getMunitionstypen())+"')")
				.iterate();

			while( ammoIter.hasNext() ) {
				Munitionsdefinition ammo = (Munitionsdefinition)ammoIter.next();
				ammoids.add(ammo.getId());
			}

			// Munition
			Cargo mycargo = ownShip.getCargo();

			List<ItemCargoEntry<Munition>> items = mycargo.getItemsOfType(Munition.class);
			for (ItemCargoEntry<Munition> item : items)
			{
				IEAmmo effect = item.getItem().getEffect();
				Item itemobject = item.getItem();

				if (ammoids.contains(effect.getAmmo().getId()))
				{
					menuEntry(t, itemobject.getName(), "ship", ownShip.getId(),
							"attack", enemyShip.getId(),
							"ksaction", "groupattack2",
							"weapon", weapon,
							"ammoid", item.getItemID(),
							"attmode", attmode);
				}
			}
		}
		
		menuEntry(t, "zur&uuml;ck",	"ship",		ownShip.getId(),
									"attack",	enemyShip.getId(),
									"ksaction",	"groupattack",
									"attmode",	attmode );
		
		return Result.OK;
	}
}
