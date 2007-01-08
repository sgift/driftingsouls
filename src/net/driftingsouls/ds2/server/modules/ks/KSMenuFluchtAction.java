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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Zeigt das Menue fuer die verschiedenen Fluchtaktionen
 * @author Christopher Jung
 *
 */
public class KSMenuFluchtAction extends BasicKSMenuAction {
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow ownShipType = Ships.getShipType(ownShip);
		
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
		
		if( !fluchtmode.equals("current") && !fluchtmode.equals("next") ) {
			fluchtmode = "current";	
		}
		
		Map<String,String> fluchtmodes = new HashMap<String,String>();
		fluchtmodes.put("current", "Jetzt");
		fluchtmodes.put("next", "N&auml;chste Runde");
							
		Map<String,String> nextfluchtmode = new HashMap<String,String>();
		nextfluchtmode.put("current", "next");
		nextfluchtmode.put("next", "current");
							  
		this.menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Fluchtzeitpunkt: "+fluchtmodes.get(fluchtmode)+"<br />\n"+
						"<span style=\"font-size:12px\">&lt; Klicken um Fluchtzeitpunkt zu &auml;ndern &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
						"ship",		ownShip.getInt("id"),
						"attack",	enemyShip.getInt("id"),
						"ksaction",	"flucht",
						"fluchtmode",	nextfluchtmode.get(fluchtmode) );
	
		boolean possible = true;
		
		if( fluchtmode.equals("current") ) {			 			
			if( (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
				possible = false;
			}
			
			if( (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
				possible = false;
			}
		}
		
		if( possible ) {
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
	
			int action = ownShip.getInt("action");
			if( (action & Battle.BS_JOIN) == 0 && (action & Battle.BS_DESTROYED) == 0 && 
				(action & Battle.BS_FLUCHT) == 0 && (ownShip.getString("docked").length() == 0) && 
				(ownShip.getInt("engine") > 0) && 
				( !ownShip.getBoolean("battleAction") || fluchtmode.equals("next") ) && 
				gotone && ( (action & Battle.BS_FLUCHTNEXT) == 0 || fluchtmode.equals("current")) ) {
					
				int curr_engines = db.first("SELECT engine FROM battles_ships WHERE shipid=",ownShip.getInt("id")).getInt("engine");
				if( curr_engines > 0 ) {
					this.menuEntry("Flucht",
							"ship",		ownShip.getInt("id"),
							"attack",	enemyShip.getInt("id"),
							"ksaction",	"flucht_single",
							"fluchtmode",	fluchtmode );
				}
			}
					
			int fluchtidlist = 0;
			Map<Integer,Integer> fluchtclasslist = new HashMap<Integer,Integer>();
			
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				SQLResultRow ashiptype = Ships.getShipType(aship);
				
				if( (aship.getInt("action") & Battle.BS_JOIN ) == 0 && (aship.getInt("action") & Battle.BS_DESTROYED) == 0 && 
					(aship.getInt("action") & Battle.BS_FLUCHT) == 0 && (aship.getString("docked").length() == 0) && (aship.getInt("engine") > 0) &&
					!aship.getBoolean("battleAction") && gotone ) {
						
					fluchtidlist++;
					Common.safeIntInc(fluchtclasslist, ashiptype.getInt("class"));
				}	
			}
									
			if( fluchtidlist > 0 ) {
				this.menuEntryAsk( "Alle Fl&uuml;chten",
						new Object[] {
							"ship",		ownShip.getInt("id"),
							"attack",	enemyShip.getInt("id"),
							"ksaction",	"flucht_all",
							"fluchtmode",	fluchtmode  },
						"Wollen sie wirklich mit allen Schiffen fl&uuml;chten?" );
			}
			
			for( Integer classID : fluchtclasslist.keySet() ) { 
				int idlist = fluchtclasslist.get(classID);
				if( idlist == 0 ) {
					continue;
				} 
				this.menuEntryAsk( "Alle "+Ships.getShipClass(classID).getPlural()+" fl&uuml;chten lassen",
						new Object[] {
							"ship",		ownShip.getInt("id"),
							"attack",	enemyShip.getInt("id"),
							"ksaction",	"flucht_class",
							"fluchtclass",	classID,
							"fluchtmode",	fluchtmode },
						"Wollen sie wirklich mit allen Schiffen der Klasse '"+Ships.getShipClass(classID).getSingular()+"' fl&uuml;chten?" );
			}
		}
				
		this.menuEntry( "zur&uuml;ck",
				"ship",		ownShip.getInt("id"),
				"attack",	enemyShip.getInt("id") );
												
		return RESULT_OK;
	}
}
