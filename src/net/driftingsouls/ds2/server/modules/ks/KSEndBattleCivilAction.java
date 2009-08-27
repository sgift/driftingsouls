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
import java.util.List;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Ermoeglicht es eine Schlacht zu beenden, wenn der Gegner nur noch zivile Schiffe hat.
 * @author Christopher Jung
 *
 */
public class KSEndBattleCivilAction extends BasicKSAction {
	@Override
	public int validate(Battle battle) {
		/*
		if( battle.getBetakStatus(battle.getOwnSide()) ) {
			boolean onlyCivil = true;
			
			List<BattleShip> enemyShips = battle.getEnemyShips();
			for( int i=0; i < enemyShips.size(); i++ ) {
				BattleShip eship = enemyShips.get(i);
				
				ShipTypeData eshiptype = eship.getTypeData();
				if( eshiptype.isMilitary() ) {
					onlyCivil = false;
					break;
				}
			}
			if( onlyCivil ) {
				return RESULT_OK;
			}
		}
		*/
		return RESULT_ERROR;
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return RESULT_ERROR;
		}

		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		
		context.getResponse().getWriter().append("Sie haben die Schlacht gewonnen.");

		PM.send(user, battle.getCommander(battle.getEnemySide()).getId(), "Schlacht verloren", "Der Gegner hat die Schlacht beendet, da du nur noch zivile Schiffe hattest. Du hast die Schlacht bei "+battle.getLocation().displayCoordinates(false)+" gegen [userprofile="+user.getId()+"]"+user.getName()+"[/userprofile] somit verloren!");

		// Schlacht beenden -> +1 Siege fuer mich; +1 Niederlagen fuer den Gegner
		battle.endTurn(true);
		battle.endBattle( 1, -1, true );

		return RESULT_HALT;
	}
}
