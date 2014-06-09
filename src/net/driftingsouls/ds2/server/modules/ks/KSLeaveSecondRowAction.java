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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.List;

/**
 * Laesst ein Schiff die zweite Reihe verlassen.
 * @author Christopher Jung
 *
 */
public class KSLeaveSecondRowAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSLeaveSecondRowAction() {
		this.requireOwnShipReady(true);
	}
	
	@Override
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW) == 0 || (ownShip.getAction() & Battle.BS_DESTROYED) != 0 ||
			( ownShip.getAction() == 0 ) || ownShip.getShip().isLanded() || ownShip.getShip().isDocked() || (ownShip.getAction() & Battle.BS_FLUCHT) != 0 ||
			( ownShip.getAction() & Battle.BS_JOIN ) != 0 ) {
			return Result.ERROR;
		}
		
		if( (ownShip.getAction() & Battle.BS_SECONDROW_BLOCKED) != 0 ) {
			return Result.ERROR;
		}

		if( ownShip.getEngine() <= 0 ) {
			return Result.ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( ownShipType.getCost() == 0 ) {
			return Result.ERROR;
		}
		 
		if( (ownShipType.getCrew() > 0) && (ownShip.getCrew() == 0) ) {
			return Result.ERROR;
		}
		
		boolean gotone = false;
		if( ownShipType.hasFlag(ShipTypeFlag.DROHNE) ) {
			List<BattleShip> ownShips = battle.getOwnShips();
			for (BattleShip aship : ownShips)
			{
				ShipTypeData ashiptype = aship.getTypeData();
				if (ashiptype.hasFlag(ShipTypeFlag.DROHNEN_CONTROLLER))
				{
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
		
		return Result.OK;
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
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
		
		battle.logme( ownShip.getName()+" fliegt zur Front\n" );
		battle.logenemy( Battle.log_shiplink(ownShip.getShip())+" fliegt zur Front\n" );
		
		int action = ownShip.getAction();
		
		action ^= Battle.BS_SECONDROW;
		action |= Battle.BS_SECONDROW_BLOCKED;
		
		ownShip.setAction(action);
		
		int remove = 0;
		for(BattleShip ship: battle.getOwnShips())
		{
			if((ship.getShip().isLanded() || ship.getShip().isDocked()) && ship.getShip().getBaseShip().getId() == ownShip.getShip().getId())
			{
				ship.setAction((ship.getAction() ^ Battle.BS_SECONDROW) | Battle.BS_SECONDROW_BLOCKED);
				remove++;
			}
		}
		
		if( remove > 0 ) 
		{
			battle.logme(remove +" an "+ownShip.getName()+" gedockte Schiffe fliegen zur Front\n");
		}
		
		battle.logenemy("]]></action>\n");
	
		return Result.OK;
	}
}
