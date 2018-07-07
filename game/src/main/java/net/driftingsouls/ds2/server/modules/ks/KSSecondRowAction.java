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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleFlag;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.List;

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
		
		if( ownShip.hasFlag(BattleShipFlag.SECONDROW) || ownShip.hasFlag(BattleShipFlag.DESTROYED) ||
				ownShip.getShip().getEngine() == 0 || ownShip.getShip().isLanded() ||
				ownShip.getShip().isDocked() || ownShip.hasFlag(BattleShipFlag.FLUCHT) ||
			ownShip.hasFlag(BattleShipFlag.JOIN) ) {
			return Result.ERROR;
		}
		
		if( ownShip.hasFlag(BattleShipFlag.SECONDROW_BLOCKED) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(BattleFlag.DROP_SECONDROW_0) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(BattleFlag.DROP_SECONDROW_1) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 0) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_0) ) {
			return Result.ERROR;
		}
		
		if( (battle.getOwnSide() == 1) && battle.hasFlag(BattleFlag.BLOCK_SECONDROW_1) ) {
			return Result.ERROR;
		}
		
		if( ownShip.getShip().isBattleAction() ) {
			return Result.ERROR;	
		}
	
		if( ownShip.getEngine() <= 0 ) {
			return Result.ERROR;
		} 
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( !ownShipType.hasFlag(ShipTypeFlag.SECONDROW) ) {
			return Result.ERROR;
		}
		
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
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		if( this.validate(battle) != Result.OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return Result.ERROR;
		}
		
		BattleShip ownShip = battle.getOwnShip();

		battle.logme( ownShip.getName()+" fliegt in die zweite Reihe\n" );
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), Battle.log_shiplink(ownShip.getShip())+" fliegt in die zweite Reihe"));

		ownShip.addFlag(BattleShipFlag.SECONDROW);
		ownShip.addFlag(BattleShipFlag.SECONDROW_BLOCKED);
		ownShip.addFlag(BattleShipFlag.BLOCK_WEAPONS);

		int remove = 1;
		List<BattleShip> ownShips = battle.getOwnShips();
		for (BattleShip s : ownShips)
		{
			if (s.getShip().getBaseShip() != null && s.getShip().getBaseShip().getId() == ownShip.getId())
			{
				if(!s.getShip().isLanded()){
					s.addFlag(BattleShipFlag.SECONDROW);
					s.addFlag(BattleShipFlag.SECONDROW_BLOCKED);
					remove++;
				}
			}
		}
		
		if( remove > 1 ) {
			battle.logme( (remove-1)+" an "+ownShip.getName()+" gedockte Schiffe fliegen in die zweite Reihe\n" );
		}

		return Result.OK;
	}
}
