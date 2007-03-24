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

import java.util.List;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Ermoeglicht das Beenden der Schlacht im Falle vom einer klaren militaerischen Uebermacht gegenueber dem Gegner
 * @author Christopher Jung
 *
 */
public class KSEndBattleEqualAction extends BasicKSAction {
	@Override
	public int validate(Battle battle) {
		//Check ob man nicht der Angreifer ist
		if( battle.getOwnSide() == 0 ) {
			return RESULT_ERROR;
		}
	
		//Flottenstaerke berechnen
		int ownpower = 0;
		int enemypower = 0;
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			if( (aship.getInt("action") & Battle.BS_JOIN) != 0 ) {
				continue;
			}
			
			SQLResultRow aShipType = ShipTypes.getShipType( aship );
				
			if( aShipType.getInt("military") == 0 ) {
				continue;
			}
			if( (aship.getInt("crew") == 0) && (aShipType.getInt("crew") != 0) ) {
				continue;
			}
			if( ShipTypes.hasShipTypeFlag(aShipType, ShipTypes.SF_JAEGER) ) {
				ownpower++;
			} else { 
				ownpower += 10;
			}
		}	
					
		List<SQLResultRow> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow aship = enemyShips.get(i);
			
			if( (aship.getInt("action") & Battle.BS_JOIN) != 0 ) {
				continue;
			}
			
			SQLResultRow aShipType = ShipTypes.getShipType( aship );
				
			if( aShipType.getInt("military") == 0 ) {
				continue;
			}
			if( (aship.getInt("crew") == 0) && (aShipType.getInt("crew") != 0) ) {
				continue;
			}
			if( ShipTypes.hasShipTypeFlag(aShipType, ShipTypes.SF_JAEGER) ) {
				enemypower++;
			} else { 
				enemypower += 10;
			}
		}		
			
		if( (enemypower == 0) || (ownpower > enemypower*5) ) {
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
		
		Context context = ContextMap.getContext();
		User user = context.getActiveUser();
		
		context.getResponse().getContent().append("Sie haben die Schlacht mit einem unentschieden beendet");
		
		PM.send(context, user.getID(), battle.getCommander(battle.getEnemySide()), "Schlacht beendet", "Der Gegner hat die Schlacht mit einem unentschieden beendet. Somit ist die Schlacht bei "+battle.getSystem()+" : "+battle.getX()+"/"+battle.getY()+" gegen [userprofile="+user.getID()+"]"+user.getName()+"[/userprofile] zuende!");
	
		// Schlacht beenden -> 0 Siege fuer mich; 0 Niederlagen fuer den Gegner
		battle.endTurn(true);
		battle.endBattle( 0, 0, true );
	
		return RESULT_HALT;
	}
}
