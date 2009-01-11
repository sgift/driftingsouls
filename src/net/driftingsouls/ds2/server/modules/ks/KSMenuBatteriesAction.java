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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt das Menue fuer Batterieentladeaktionen an.
 * @author Christopher Jung
 *
 */
public class KSMenuBatteriesAction extends BasicKSMenuAction {
	@Override
	public int validate(Battle battle) {
		int battships = 0;
		
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip aship = ownShips.get(i);
			
			ShipTypeData ashiptype = aship.getTypeData();
			if( aship.getShip().getEnergy() >= ashiptype.getEps() ) {
				continue;
			}
			
			Cargo mycargo = aship.getCargo();
			if( mycargo.hasResource( Resources.BATTERIEN ) ) {
				battships++;
				break;
			}
		}

		//Schilde aufladen
		if(	battships > 0 ) {
			return RESULT_OK;
		}
		return RESULT_ERROR;
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
	
		if( this.isPossible(battle, new KSDischargeBatteriesSingleAction() ) == RESULT_OK ) {
			this.menuEntry( "Batterien entladen", 
								"ship",		ownShip.getId(),
								"attack",	enemyShip.getId(),
								"ksaction",	"batterien_single" );
		}
				
		int battsidlist = 0;
		Map<Integer,Integer> battsclasslist = new HashMap<Integer,Integer>();
				
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip aship = ownShips.get(i);
			
			ShipTypeData ashiptype = aship.getTypeData();
			if( aship.getShip().getEnergy() >= ashiptype.getEps() ) {
				continue;
			}
			
			Cargo mycargo = aship.getCargo();
			if( mycargo.hasResource( Resources.BATTERIEN ) ) {
				battsidlist++;
				Common.safeIntInc(battsclasslist, ashiptype.getShipClass());
			}
		}
								
		if( battsidlist > 0 ) {
			this.menuEntryAsk( "Alle Batterien entladen",
								new Object[] {	"ship",		ownShip.getId(),
												"attack",	enemyShip.getId(),
												"ksaction",	"batterien_all" },
								"Wollen sie wirklich bei allen Schiffen die Batterien entladen?" );
		}
		
		for( Map.Entry<Integer, Integer> entry: battsclasslist.entrySet() ) {
			int classID = entry.getKey();
			int idlist = entry.getValue();
			
			if( idlist == 0 ) {
				continue;
			} 
			this.menuEntryAsk( "Bei allen "+ShipTypes.getShipClass(classID).getPlural()+"n die Batterien entladen",
								new Object[] { 	"ship",			ownShip.getId(),
												"attack",		enemyShip.getId(),
												"ksaction",		"batterien_class",
												"battsclass",	classID },
								"Wollen sie wirklich bei allen Schiffen der Klasse '"+ShipTypes.getShipClass(classID).getSingular()+"' die Batterien entladen?" );
		}
				
		this.menuEntry("zur&uuml;ck",	
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"other" );
												
		return RESULT_OK;
	}
}
