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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt das Menue fuer die verschiedenen Fluchtaktionen.
 * @author Christopher Jung
 *
 */
public class KSMenuFluchtAction extends BasicKSMenuAction {
	@Override
	public Result execute(Battle battle) throws IOException {
		Result result = super.execute(battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		BattleShip enemyShip = battle.getEnemyShip();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
		
		if(!fluchtmode.equals("next")) 
		{
			fluchtmode = "next";	
		}
		
		Map<String,String> fluchtmodes = new HashMap<String,String>();
		fluchtmodes.put("next", "N&auml;chste Runde");
							
		Map<String,String> nextfluchtmode = new HashMap<String,String>();
		nextfluchtmode.put("next", "current");
							  
		this.menuEntry( "<span style=\"font-size:3px\">&nbsp;<br /></span>Fluchtzeitpunkt: "+fluchtmodes.get(fluchtmode)+"<br />\n"+
						"<span style=\"font-size:12px\">&lt; Klicken um Fluchtzeitpunkt zu &auml;ndern &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
						"ship",		ownShip.getId(),
						"attack",	enemyShip.getId(),
						"ksaction",	"flucht",
						"fluchtmode",	nextfluchtmode.get(fluchtmode) );
	
		boolean possible = true;
		
		if( possible ) {
			boolean gotone = false;
			if( ownShipType.hasFlag(ShipTypes.SF_DROHNE) ) {
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
			else {
				gotone = true;	
			}
	
			int action = ownShip.getAction();
			if( (action & Battle.BS_JOIN) == 0 && (action & Battle.BS_DESTROYED) == 0 && 
				(action & Battle.BS_FLUCHT) == 0 && !ownShip.getShip().isLanded() && !ownShip.getShip().isDocked() && 
				(ownShip.getShip().getEngine() > 0) && 
				( !ownShip.getShip().isBattleAction() || fluchtmode.equals("next") ) && 
				gotone && ( (action & Battle.BS_FLUCHTNEXT) == 0 || fluchtmode.equals("current")) ) {
					
				if( ownShip.getEngine() > 0 ) {
					this.menuEntry("Flucht",
							"ship",		ownShip.getId(),
							"attack",	enemyShip.getId(),
							"ksaction",	"flucht_single",
							"fluchtmode",	fluchtmode );
				}
			}
					
			int fluchtidlist = 0;
			Map<Integer,Integer> fluchtclasslist = new HashMap<Integer,Integer>();
			
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				BattleShip aship = ownShips.get(i);
				ShipTypeData ashiptype = aship.getTypeData();
				
				if( (aship.getAction() & Battle.BS_JOIN ) == 0 && (aship.getAction() & Battle.BS_DESTROYED) == 0 && 
					(aship.getAction() & Battle.BS_FLUCHT) == 0 && !ownShip.getShip().isLanded() && !ownShip.getShip().isDocked() && (aship.getShip().getEngine() > 0) &&
					!aship.getShip().isBattleAction() && gotone ) {
						
					fluchtidlist++;
					Common.safeIntInc(fluchtclasslist, ashiptype.getShipClass());
				}	
			}
									
			if( fluchtidlist > 0 ) {
				this.menuEntryAsk( "Alle Fl&uuml;chten",
						new Object[] {
							"ship",		ownShip.getId(),
							"attack",	enemyShip.getId(),
							"ksaction",	"flucht_all",
							"fluchtmode",	fluchtmode  },
						"Wollen sie wirklich mit allen Schiffen fl&uuml;chten?" );
			}
			
			for( Map.Entry<Integer, Integer> entry: fluchtclasslist.entrySet()) { 
				int classID = entry.getKey();
				int idlist = entry.getValue();
				if( idlist == 0 ) {
					continue;
				} 
				this.menuEntryAsk( "Alle "+ShipTypes.getShipClass(classID).getPlural()+" fl&uuml;chten lassen",
						new Object[] {
							"ship",		ownShip.getId(),
							"attack",	enemyShip.getId(),
							"ksaction",	"flucht_class",
							"fluchtclass",	classID,
							"fluchtmode",	fluchtmode },
						"Wollen sie wirklich mit allen Schiffen der Klasse '"+ShipTypes.getShipClass(classID).getSingular()+"' fl&uuml;chten?" );
			}
		}
				
		this.menuEntry( "zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId() );
												
		return Result.OK;
	}
}
