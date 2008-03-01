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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt das Menue zum Aufladen der Schilde an
 * @author Christopher Jung
 *
 */
public class KSMenuShieldsAction extends BasicKSMenuAction {
	@Override
	public int validate(Battle battle) {
		boolean showshields = false;
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip aship = ownShips.get(i);
			ShipTypeData ashiptype = aship.getTypeData();
			
			if( (ashiptype.getShields() > 0) && (aship.getShip().getShields() < ashiptype.getShields()) ) {
				showshields = true;
				break;
			}	
		}

		//Schilde aufladen
		if( showshields ) {
			return RESULT_OK;
		}
		return RESULT_ERROR;
	}

	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return RESULT_ERROR;
		}
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
	
		if( (ownShipType.getShields() > 0) && (ownShip.getShip().getShields() < ownShipType.getShields()) ) {
			this.menuEntry("Schilde aufladen", 
					"ship",		ownShip.getId(),
					"attack",	enemyShip.getId(),
					"ksaction",	"shields_single" );
		}
				
		int shieldidlist = 0;
		Map<Integer,Integer> shieldclasslist = new HashMap<Integer,Integer>();
		
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip aship = ownShips.get(i);
			
			ShipTypeData ashiptype = aship.getTypeData();
			if( aship.getShip().getShields() < ashiptype.getShields() ) {
				shieldidlist++;
				Common.safeIntInc(shieldclasslist, ashiptype.getShipClass());
			}
		}
								
		if( shieldidlist > 0 ) {
			this.menuEntryAsk( "Alle Schilde aufladen",
					new Object[] {
						"ship",		ownShip.getId(),
						"attack",	enemyShip.getId(),
						"ksaction",	"shields_all" },
					"Wollen sie wirklich alle Schilde aufladen?" );
		}
		
		for( Map.Entry<Integer, Integer> entry: shieldclasslist.entrySet()) {
			int classID = entry.getKey();
			int idlist = entry.getValue();
			
			if( idlist == 0 ) {
				continue;
			} 
			this.menuEntryAsk( "Alle "+ShipTypes.getShipClass(classID).getPlural()+"-Schilde aufladen",
					new Object[] {
						"ship",		ownShip.getId(),
						"attack",	enemyShip.getId(),
						"ksaction",	"shields_class",
						"shieldclass",	classID },
					"Wollen sie wirklich bei allen Schiffen der Klasse '"+ShipTypes.getShipClass(classID).getSingular()+"' die Schilde aufladen?" );
		}
				
		this.menuEntry("zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"other" );
												
		return RESULT_OK;
	}
}
