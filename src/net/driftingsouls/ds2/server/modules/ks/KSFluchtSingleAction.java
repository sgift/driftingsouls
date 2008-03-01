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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Laesst das gerade ausgewaehlte Schiff fliehen
 * @author Christopher Jung
 *
 */
public class KSFluchtSingleAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSFluchtSingleAction() {
		this.requireOwnShipReady(true);
	}
	
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		BattleShip ownShip = battle.getOwnShip();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
	
		if( (ownShip.getAction() & Battle.BS_DESTROYED) != 0 ) {
			battle.logme( "Dieses Schiff explodiert am Ende der Runde\n" );
			return RESULT_ERROR;
		}
	
		if( ownShip.getShip().getEngine() == 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return RESULT_ERROR;
		}
		
		if( ownShip.getDocked().length() > 0 ) {
			battle.logme( "Sie k&ouml;nnen nicht mit gedockten/gelandeten Schiffen fliehen\n" );
			return RESULT_ERROR;
		}
	
		if( ownShip.getEngine() <= 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return RESULT_ERROR;
		}
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		 
		if( (ownShipType.getCrew() > 0) && (ownShip.getCrew() < (int)(ownShipType.getCrew()/4d)) ) {
			battle.logme( "Nicht genug Crew um zu fliehen\n" );
			return RESULT_ERROR;
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
			battle.logme( "Sie ben&ouml;tigen ein Drohnen-Kontrollschiff um fliehen zu k&ouml;nnen\n" );
			return RESULT_ERROR;
		}
		
		
		int fluchtflag = 0;
		battle.logme( ownShip.getName()+" wird n&auml;chste Runde fl&uuml;chten\n" );
			
		ownShip.setAction(ownShip.getAction() | Battle.BS_FLUCHTNEXT);
		fluchtflag = Battle.BS_FLUCHTNEXT;

		int remove = 1;
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int j=0; j < ownShips.size(); j++ ) {
			BattleShip s = ownShips.get(j);
			if( (s.getDocked().length() > 0) && (s.getDocked().equals(""+ownShip.getId()) || s.getDocked().equals("l "+ownShip.getId()) ) ) {
				s.setAction(s.getAction() | fluchtflag);

				remove++;
			}
		}
		
		if( fluchtmode.equals("next") && (remove > 1) ) {
			battle.logme( (remove-1)+" an "+ownShip.getName()+" gedockte Schiffe werden n&auml;chste Runde fliehen\n" );
		}
	
		return RESULT_OK;
	}
}
