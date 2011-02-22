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
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Laesst ein Schiff in die zweite Reihe fliegen.
 * @author Christopher Jung
 *
 */
public class KSSecondRowAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSSecondRowAction() {
		this.requireOwnShipReady(true);
	}
	
	@Override
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW) != 0 || (ownShip.getAction() & Battle.BS_DESTROYED) != 0 ||
			( ownShip.getShip().getEngine() == 0 ) || ownShip.getShip().isLanded() || ownShip.getShip().isLanded() || (ownShip.getAction() & Battle.BS_FLUCHT) != 0 ||
			( ownShip.getAction() & Battle.BS_JOIN ) != 0 ) {
			return Result.ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW_BLOCKED) != 0 ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_BLOCK_SECONDROW_0) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_BLOCK_SECONDROW_1) ) {
			return Result.ERROR;
		}
		
		if( ownShip.getShip().isBattleAction() ) {
			return Result.ERROR;	
		}
	
		if( ownShip.getEngine() <= 0 ) {
			return Result.ERROR;
		} 
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( !ownShipType.hasFlag(ShipTypes.SF_SECONDROW) ) {
			return Result.ERROR;
		}
		
		if( ownShipType.getCost() == 0 ) {
			return Result.ERROR;
		}
		 
		if( (ownShipType.getCrew() > 0) && (ownShip.getCrew() == 0) ) {
			return Result.ERROR;
		}
		
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
		
		if( !gotone ) {
			return Result.ERROR;
		}
		
		//Does a first row exist without this ship?
		for(BattleShip ship: battle.getShips(Side.OWN))
		{
			if(!ship.equals(ownShip) && !ship.isSecondRow())
			{
				return Result.OK;
			}
		}
				
		return Result.ERROR;
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
		BattleShip ownShip = battle.getOwnShip();
		
		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		
		battle.logme( ownShip.getName()+" fliegt in die zweite Reihe\n" );
		battle.logenemy( Battle.log_shiplink(ownShip.getShip())+" fliegt in die zweite Reihe\n" );
		
		int action = ownShip.getAction();
		
		action |= Battle.BS_SECONDROW;
		action |= Battle.BS_SECONDROW_BLOCKED;
		action |= Battle.BS_BLOCK_WEAPONS;
		
		ownShip.setAction(action);

		int remove = 1;
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip s = ownShips.get(i);
			
			if(s.getShip().getBaseShip() != null && s.getShip().getBaseShip().getId() == ownShip.getId())
			{
				s.setAction(s.getAction() | Battle.BS_SECONDROW | Battle.BS_SECONDROW_BLOCKED);	
				remove++;
			}
		}
		
		if( remove > 1 ) {
			battle.logme( (remove-1)+" an "+ownShip.getName()+" gedockte Schiffe fliegen in die zweite Reihe\n" );
		}
		
		battle.logenemy("]]></action>\n");
	
		return Result.OK;
	}
}
