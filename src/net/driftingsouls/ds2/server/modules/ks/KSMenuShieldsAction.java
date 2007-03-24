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
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
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
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			SQLResultRow ashiptype = ShipTypes.getShipType(aship);
			
			if( (ashiptype.getInt("shields") > 0) && (aship.getInt("shields") < ashiptype.getInt("shields")) ) {
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
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
	
		if( (ownShipType.getInt("shields") > 0) && (ownShip.getInt("shields") < ownShipType.getInt("shields")) ) {
			this.menuEntry("Schilde aufladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 1 AP</span>", 
					"ship",		ownShip.getInt("id"),
					"attack",	enemyShip.getInt("id"),
					"ksaction",	"shields_single" );
		}
				
		int shieldidlist = 0;
		Map<Integer,Integer> shieldclasslist = new HashMap<Integer,Integer>();
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			SQLResultRow ashiptype = ShipTypes.getShipType(aship);
			if( (aship.getInt("shields") > 0) && (aship.getInt("shields") < ashiptype.getInt("shields")) ) {
				shieldidlist++;
				Common.safeIntInc(shieldclasslist, ashiptype.getInt("class"));
			}
		}
								
		if( shieldidlist > 0 ) {
			this.menuEntryAsk( "Alle Schilde aufladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: "+shieldidlist+" AP</span>",
					new Object[] {
						"ship",		ownShip.getInt("id"),
						"attack",	enemyShip.getInt("id"),
						"ksaction",	"shields_all" },
					"Wollen sie wirklich alle Schilde aufladen?" );
		}
		
		for( Integer classID : shieldclasslist.keySet() ) {
			int idlist = shieldclasslist.get(classID);
			
			if( idlist == 0 ) {
				continue;
			} 
			this.menuEntryAsk( "Alle "+ShipTypes.getShipClass(classID).getPlural()+"-Schilde aufladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: "+idlist+" AP</span>",
					new Object[] {
						"ship",		ownShip.getInt("id"),
						"attack",	enemyShip.getInt("id"),
						"ksaction",	"shields_class",
						"shieldclass",	classID },
					"Wollen sie wirklich bei allen Schiffen der Klasse '"+ShipTypes.getShipClass(classID).getSingular()+"' die Schilde aufladen?" );
		}
				
		this.menuEntry("zur&uuml;ck",
				"ship",		ownShip.getInt("id"),
				"attack",	enemyShip.getInt("id"),
				"ksaction",	"other" );
												
		return RESULT_OK;
	}
}
