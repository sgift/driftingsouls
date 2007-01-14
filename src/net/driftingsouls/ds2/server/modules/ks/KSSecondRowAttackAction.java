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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.ShipClasses;

/**
 * Ermoeglicht den Angriff auf die zweite Reihe des Gegners
 * @author Christopher Jung
 *
 */
public class KSSecondRowAttackAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSSecondRowAttackAction() {
		this.requireAP(300);
	}
	
	@Override
	public int validate(Battle battle) {
		if( battle.hasFlag(Battle.FLAG_FIRSTROUND) ) {
			return RESULT_ERROR;
		}
		
		if( battle.getPoints(battle.getOwnSide()) < 300 ) {
			return RESULT_ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
			return RESULT_ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
			return RESULT_ERROR;
		}
		
		if( !battle.isSecondRowStable(battle.getEnemySide(), null) ) {
			return RESULT_ERROR;
		}  
		
		int size = 0;
		int rowcount = 0;
		boolean gotone = false;
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			if( (aship.getInt("action") & Battle.BS_FLUCHT) != 0 || (aship.getInt("action") & Battle.BS_JOIN) != 0 ||
				(aship.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
				continue;
			}
			SQLResultRow shiptype = Ships.getShipType(aship);
			
			if( shiptype.getInt("class") == ShipClasses.ZERSTOERER.ordinal() ) {
				gotone = true;
			}
			
			if( shiptype.getInt("size") > 3 ) {
				size += shiptype.getInt("size");
			}
		}
		
		if( !gotone ) {
			return RESULT_ERROR;
		}
		
		List<SQLResultRow> enemyShips = battle.getOwnShips();
		for( int i=0; i < enemyShips.size(); i++ ) {
			SQLResultRow aship = enemyShips.get(i);
			
			if( (aship.getInt("action") & Battle.BS_FLUCHT) != 0 || (aship.getInt("action") & Battle.BS_JOIN) != 0  ) {
				continue;
			}
			if( (aship.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
				rowcount++;
				continue;
			}
			SQLResultRow shiptype = Ships.getShipType(aship);
			
			if( shiptype.getInt("size") > 3 ) {
				size += shiptype.getInt("size");
			}
		}
		
		if( (rowcount == 0) || (size < 0) ) {
			return RESULT_ERROR;
		}
		
		return RESULT_OK;
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
		
		battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide())-300);
		if( battle.getOwnSide() == 0 ) {
			battle.setFlag(Battle.FLAG_DROP_SECONDROW_1, true);
		}
		else {
			battle.setFlag(Battle.FLAG_DROP_SECONDROW_0, true);
		}
		
		battle.logme( "Ihre Schiffe r&uuml;cken vor und dr&auml;ngen die feindlichen Linien unter schwerem Feuer langsam zur&uuml;ck");
		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		battle.logenemy("Die feindlichen Schiffe r&uuml;cken unter schwerem Feuer langsam vor und dr&auml;ngen trotz heftigsten Widerstands die Linien zur&uuml;ck\n");
		battle.logenemy("]]></action>\n");	
		
		battle.save(false);
		
		return RESULT_OK;
	}
}
