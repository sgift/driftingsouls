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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Ermoeglicht das Vorruecken gegen eine instabile zweite Reihe des Gegners.
 * @author Christopher Jung
 *
 */
public class KSSecondRowEngageAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSSecondRowEngageAction() {
	}
	
	@Override
	public Result validate(Battle battle) {
		if( battle.isSecondRowStable(battle.getEnemySide()) ) {
			return Result.ERROR;
		}  
		
		return Result.OK;
	}

	@Override
	public Result execute(Battle battle) throws IOException {
		Result result = super.execute(battle);
		if( result != Result.OK ) {
			return result;
		}
		
		if( this.validate(battle) != Result.OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return Result.ERROR;
		}
		
		Context context = ContextMap.getContext();
		
		List<BattleShip> enemyShips = battle.getEnemyShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			BattleShip eShip = enemyShips.get(i);
			
			if( (eShip.getAction() & Battle.BS_SECONDROW) != 0 ) {
				eShip.setAction(eShip.getAction() ^ Battle.BS_SECONDROW);
			}
		}
		
		battle.logme( "Ihre Schiffe r&uuml;cken vor und durchbrechen die feindlichen Linien\n");
		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		battle.logenemy("Die feindlichen Schiffe r&uuml;cken vor und durchbrechen trotz heftigen Widerstands die Linien\n");
		battle.logenemy("]]></action>\n");	
		
		if( battle.getOwnSide() == 0 ) {
			battle.setFlag(Battle.FLAG_BLOCK_SECONDROW_1, true);
		}
		else {
			battle.setFlag(Battle.FLAG_BLOCK_SECONDROW_0, true);
		}
		
		return Result.OK;
	}
}
