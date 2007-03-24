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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Zeigt das Menue fuer Batterieentladeaktionen an
 * @author Christopher Jung
 *
 */
public class KSMenuBatteriesAction extends BasicKSMenuAction {
	@Override
	public int validate(Battle battle) {
		int battships = 0;
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			SQLResultRow ashiptype = ShipTypes.getShipType(aship);
			if( aship.getInt("e") >= ashiptype.getInt("eps") ) {
				continue;
			}
			
			Cargo mycargo = new Cargo( Cargo.Type.STRING, aship.getString("cargo") );
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
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
	
		if( this.isPossible(battle, new KSDischargeBatteriesSingleAction() ) == RESULT_OK ) {
			this.menuEntry( "Batterien entladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 3 AP</span>", 
								"ship",		ownShip.getInt("id"),
								"attack",	enemyShip.getInt("id"),
								"ksaction",	"batterien_single" );
		}
				
		int battsidlist = 0;
		Map<Integer,Integer> battsclasslist = new HashMap<Integer,Integer>();
				
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			SQLResultRow ashiptype = ShipTypes.getShipType(aship);
			if( aship.getInt("e") >= ashiptype.getInt("eps") ) {
				continue;
			}
			
			Cargo mycargo = new Cargo( Cargo.Type.STRING, aship.getString("cargo") );
			if( mycargo.hasResource( Resources.BATTERIEN ) ) {
				battsidlist++;
				Common.safeIntInc(battsclasslist, ashiptype.getInt("class"));
			}
		}
								
		if( battsidlist > 0 ) {
			this.menuEntryAsk( "Alle Batterien entladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: "+(battsidlist*3)+" AP</span>",
								new Object[] {	"ship",		ownShip.getInt("id"),
												"attack",	enemyShip.getInt("id"),
												"ksaction",	"batterien_all" },
								"Wollen sie wirklich bei allen Schiffen die Batterien entladen?" );
		}
		
		for( Integer classID : battsclasslist.keySet() ) {
			int idlist = battsclasslist.get(classID);
			
			if( idlist == 0 ) {
				continue;
			} 
			this.menuEntryAsk( "Bei allen "+ShipTypes.getShipClass(classID).getPlural()+"n die Batterien entladen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: "+(idlist*3)+" AP</span>",
								new Object[] { 	"ship",			ownShip.getInt("id"),
												"attack",		enemyShip.getInt("id"),
												"ksaction",		"batterien_class",
												"battsclass",	classID },
								"Wollen sie wirklich bei allen Schiffen der Klasse '"+ShipTypes.getShipClass(classID).getSingular()+"' die Batterien entladen?" );
		}
				
		this.menuEntry("zur&uuml;ck",	
				"ship",		ownShip.getInt("id"),
				"attack",	enemyShip.getInt("id"),
				"ksaction",	"other" );
												
		return RESULT_OK;
	}
}
