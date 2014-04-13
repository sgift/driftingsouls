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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.entities.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.config.items.effects.IEAmmo;
import net.driftingsouls.ds2.server.config.items.effects.ItemEffect;
import net.driftingsouls.ds2.server.entities.Ammo;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Laesst den Benutzer fuer eine ammobasierte Waffe die gewuenschte Munition auswaehlen (sofern
 * dies fuer die Waffe erlaubt wurde).
 * @author Christopher Jung
 *
 */
public class KSMenuAttackMuniSelectAction extends BasicKSMenuAction {	
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
	
		if( (ownShip.getAction() & Battle.BS_JOIN) != 0 ) {
			return Result.ERROR;
		}
		
		if( (enemyShip.getAction() & Battle.BS_JOIN) != 0 ) {
			return Result.ERROR;
		}
		
		if( (enemyShip.getAction() & Battle.BS_DESTROYED) != 0 ) {
			return Result.ERROR;
		}
		if(ownShip.getShip().isLanded()) 
		{
			return Result.ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_FLUCHT) != 0 ) {
			return Result.ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( (enemyShip.getAction() & Battle.BS_FLUCHT) != 0 &&	!ownShipType.hasFlag(ShipTypeFlag.ABFANGEN) ) {
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
	public Result execute(Battle battle) throws IOException {
		Result result = super.execute(battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		final String weapon = context.getRequest().getParameterString("weapon");
		
		String attmode = this.getAttMode();
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW) != 0 &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.LONG_RANGE) &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.VERY_LONG_RANGE) ) {
			battle.logme("Diese Waffe hat nicht die notwendige Reichweite um aus der zweiten Reihe heraus abgefeuert zu werden\n");
			return Result.ERROR;	
		}
		
		if( (enemyShip.getAction() & Battle.BS_SECONDROW) != 0 && 
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.VERY_LONG_RANGE)	) {
			battle.logme("Diese Waffe hat nicht die notwendige Reichweite um in die zweiten Reihe des Gegners abgefeuert zu werden\n");
			return Result.ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_BLOCK_WEAPONS) != 0 ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_DISABLE_WEAPONS) != 0 ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return Result.ERROR;
		}
		
		/*
		 * 	Feuermodusauswahl
		 */
									
		menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Feuermodus: "+ATTMODES.get(attmode)+"<br /> "+
				"<span style=\"font-size:12px\">&lt; Klicken um Feuermodus zu wechseln &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
				"ship",		ownShip.getId(),
			 	"attack",	enemyShip.getId(),
			 	"ksaction",	"attack",
			 	"attmode",	NEXTATTMODES.get(attmode));

		/*
		 *  Ammoauswahl
		 */

		if( Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.AMMO_SELECT) ) {
			Set<Integer> ammoids = new HashSet<>();

			Iterator<?> ammoIter = db.createQuery("from Ammo " +
					"where type in ('"+Common.implode("','", Weapons.get().weapon(weapon).getMunitionstypen())+"')")
				.iterate();

			while( ammoIter.hasNext() ) {
				Ammo ammo = (Ammo)ammoIter.next();
				ammoids.add(ammo.getId());
			}

			// Munition
			Cargo mycargo = ownShip.getCargo();

			List<ItemCargoEntry> items = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for (ItemCargoEntry item : items)
			{
				IEAmmo effect = (IEAmmo) item.getItemEffect();
				Item itemobject = item.getItemObject();

				if (ammoids.contains(effect.getAmmo().getId()))
				{
					menuEntry(itemobject.getName(), "ship", ownShip.getId(),
							"attack", enemyShip.getId(),
							"ksaction", "attack2",
							"weapon", weapon,
							"ammoid", item.getItemID(),
							"attmode", attmode);
				}
			}
		}
		
		menuEntry("zur&uuml;ck",	"ship",		ownShip.getId(),
									"attack",	enemyShip.getId(),
									"ksaction",	"attack",
									"attmode",	attmode );
		
		return Result.OK;
	}
}
