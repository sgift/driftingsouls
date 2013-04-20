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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Ermoeglicht den Angriff auf die zweite Reihe des Gegners.
 * @author Christopher Jung
 *
 */
public class KSSecondRowAttackAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSSecondRowAttackAction() {
	}
	
	@Override
	public Result validate(Battle battle) {
		if( battle.hasFlag(Battle.FLAG_FIRSTROUND) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
			return Result.ERROR;
		}
		
		//The attacker needs a destroyer
		boolean hasDestroyer = false;
		for(BattleShip aship: battle.getOwnShips()) 
		{
			if( (aship.getAction() & Battle.BS_FLUCHT) != 0 || (aship.getAction() & Battle.BS_JOIN) != 0 ||
				(aship.getAction() & Battle.BS_SECONDROW) != 0 ) 
			{
				continue;
			}
			ShipTypeData shiptype = aship.getTypeData();
			
			if( shiptype.getShipClass() == ShipClasses.ZERSTOERER ) {
				hasDestroyer = true;
			}
		}
		
		if( !hasDestroyer ) {
			return Result.ERROR;
		}
		
		if(battle.getBattleValue(Side.OWN) < battle.getBattleValue(Side.ENEMY)*2)
		{
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
		
		return Result.OK;
	}
}
