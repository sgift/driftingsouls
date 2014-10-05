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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.List;

/**
 * Laesst das gerade ausgewaehlte Schiff fliehen.
 * @author Christopher Jung
 *
 */
public class KSFluchtSingleAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSFluchtSingleAction() {
		this.requireOwnShipReady(true);
	}
	
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		BattleShip ownShip = battle.getOwnShip();

		if( ownShip.hasFlag(BattleShipFlag.DESTROYED) ) {
			battle.logme( "Dieses Schiff explodiert am Ende der Runde\n" );
			return Result.ERROR;
		}
	
		if( ownShip.getShip().getEngine() == 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return Result.ERROR;
		}
		
		if( ownShip.getShip().isDocked() || ownShip.getShip().isLanded()) {
			battle.logme( "Sie k&ouml;nnen nicht mit gedockten/gelandeten Schiffen fliehen\n" );
			return Result.ERROR;
		}
	
		if( ownShip.getEngine() <= 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return Result.ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		 
		if( (ownShipType.getMinCrew() > 0) && (ownShip.getCrew() < (int)(ownShipType.getMinCrew()/4d)) ) {
			battle.logme( "Nicht genug Crew um zu fliehen\n" );
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
			battle.logme( "Sie ben&ouml;tigen ein Drohnen-Kontrollschiff um fliehen zu k&ouml;nnen\n" );
			return Result.ERROR;
		}

		battle.logme( ownShip.getName()+" wird n&auml;chste Runde fl&uuml;chten\n" );
			
		ownShip.addFlag(BattleShipFlag.FLUCHTNEXT);

		int remove = 1;
		List<BattleShip> ownShips = battle.getOwnShips();
		for (BattleShip s : ownShips)
		{
			if (s.getShip().getBaseShip() != null && s.getShip().getBaseShip().getId() == ownShip.getId())
			{
				s.addFlag(BattleShipFlag.FLUCHTNEXT);

				remove++;
			}
		}
		
		if( remove > 1 ) {
			battle.logme( (remove-1)+" an "+ownShip.getName()+" gedockte Schiffe werden n&auml;chste Runde fliehen\n" );
		}
	
		return Result.OK;
	}
}
