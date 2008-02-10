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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.IEAmmo;
import net.driftingsouls.ds2.server.config.Item;
import net.driftingsouls.ds2.server.config.ItemEffect;
import net.driftingsouls.ds2.server.config.Weapon;
import net.driftingsouls.ds2.server.config.Weapons;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Laesst den Benutzer fuer eine ammobasierte Waffe die gewuenschte Munition auswaehlen (sofern
 * dies fuer die Waffe erlaubt wurde)
 * @author Christopher Jung
 *
 */
public class KSMenuAttackMuniSelectAction extends BasicKSMenuAction {	
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
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		final String weapon = context.getRequest().getParameterString("weapon");
		
		String attmode = this.getAttMode();
		
		if( (ownShip.getInt("action") & Battle.BS_SECONDROW) != 0 &&
			!Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.LONG_RANGE) ) {
			battle.logme("Diese Waffe hat nicht die notwendige Reichweite um aus der zweiten Reihe heraus abgefeuert zu werden\n");
			return RESULT_ERROR;	
		}
		
		if( (ownShip.getInt("action") & Battle.BS_BLOCK_WEAPONS) != 0 ) {
			battle.logme( "Sie k&ouml;nnen in dieser Runde keine Waffen mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		if( (ownShip.getInt("action") & Battle.BS_DISABLE_WEAPONS) != 0 ) {
			battle.logme( "Das Schiff kann seine Waffen in diesem Kampf nicht mehr abfeuern\n" );
			return RESULT_ERROR;
		}
		
		int attcount = this.getAttCount();
		
		/*
		 * 	Feuermodusauswahl
		 */
									
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

		/*
		 *  Ammoauswahl
		 */

		if( Weapons.get().weapon(weapon).hasFlag(Weapon.Flags.AMMO_SELECT) ) {
			Set<Integer> ammoids = new HashSet<Integer>();

			SQLQuery ammoid = db.query("SELECT id FROM ammo WHERE type='",Weapons.get().weapon(weapon).getAmmoType()+"'");
			while( ammoid.next() ) {
				ammoids.add(ammoid.getInt("id"));
			}
			ammoid.free();

			// Munition
			Cargo mycargo = new Cargo( Cargo.Type.STRING, ownShip.getString("cargo") );

			List<ItemCargoEntry> items = mycargo.getItemsWithEffect( ItemEffect.Type.AMMO );
			for( int i=0; i < items.size(); i++ ) {
				ItemCargoEntry item = items.get(i);
				
				IEAmmo effect = (IEAmmo)item.getItemEffect();
				Item itemobject = item.getItemObject();
						
				if( ammoids.contains(effect.getAmmo().getId()) ) {
					menuEntry(itemobject.getName(),	"ship",		ownShip.getInt("id"),
													"attack",	enemyShip.getInt("id"),
													"ksaction",	"attack2",
													"weapon",	weapon,
													"ammoid",	item.getItemID(),
													"attmode",	attmode,
													"attcount",	attcount );
				}
			}
		}
		
		menuEntry("zur&uuml;ck",	"ship",		ownShip.getInt("id"),
									"attack",	enemyShip.getInt("id"),
									"ksaction",	"attack",
									"attmode",	attmode,
									"attcount",	attcount );
		
		return RESULT_OK;
	}
}
