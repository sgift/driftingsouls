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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.List;

/**
 * Laesst alle Schiffe einer Seite fliehen.
 * @author Christopher Jung
 *
 */
public class KSFluchtAllAction extends BasicKSAction {
	
	/**
	 * Prueft, ob das Schiff fliehen kann oder nicht.
	 * @param fluchtmode Der Zeitpunkt der Flucht (<code>current</code> oder <code>next</code>) - dieser Parameter ist nur noch aus historischen Gruenden vorhanden und hat keinen Effekt mehr
	 * @param ship Das Schiff
	 * @param shiptype Der Schiffstyp
	 * @return <code>true</code>, wenn das Schiff fliehen kann
	 */
	protected boolean validateShipExt( String fluchtmode, BattleShip ship, ShipTypeData shiptype ) {
		// Extension Point
		return true;
	}
	
	@Override
	public final Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();

		String fluchtmode = context.getRequest().getParameterString("fluchtmode");

		Boolean gotone = null;
		
		int fluchtflag = Battle.BS_FLUCHTNEXT;
		
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip aship = ownShips.get(i);
			
			if( (aship.getAction() & Battle.BS_DESTROYED) != 0 ) {
				continue;
			}
			
			if( (aship.getAction() & Battle.BS_FLUCHT) != 0 ) {
				continue;
			}
			
			if( fluchtmode.equals("next") && (aship.getAction() & Battle.BS_FLUCHTNEXT) != 0 ) {
				continue;
			}
			
			if( (aship.getAction() & Battle.BS_JOIN) != 0 ) {
				continue;
			}
			 
			if( aship.getShip().getEngine() == 0 ) {
				continue;
			}
			
			if( aship.getShip().isLanded() || aship.getShip().isDocked() ) {
				continue;
			}

			if( aship.getEngine() <= 0 ) {
				continue;
			} 
			
			ShipTypeData ashipType = aship.getTypeData();
			 
			if( (ashipType.getCrew() > 0) && (aship.getCrew() < (int)(ashipType.getCrew()/4d)) ) {
				continue;
			}
	
			if( (gotone == null) && ashipType.hasFlag(ShipTypeFlag.DROHNE) ) {
				gotone = Boolean.FALSE;
				for (BattleShip as : ownShips)
				{
					ShipTypeData ast = as.getTypeData();
					if (ast.hasFlag(ShipTypeFlag.DROHNEN_CONTROLLER))
					{
						gotone = Boolean.TRUE;
						break;
					}
				}
			}
			if( (gotone == Boolean.FALSE) && ashipType.hasFlag(ShipTypeFlag.DROHNE) ) {
				continue;
			}
			
			if( !validateShipExt(fluchtmode, aship, ashipType) ) {
				continue;
			}

		
			battle.logme( aship.getName()+" flieht n&auml;chste Runde\n" );
			aship.setAction(aship.getAction() | Battle.BS_FLUCHTNEXT);
			
			int remove = 1;
			for (BattleShip s : ownShips)
			{
				if (s.getShip().getBaseShip() != null && s.getShip().getBaseShip().getId() == aship.getId())
				{
					s.setAction(s.getAction() | fluchtflag);

					remove++;
				}
			}
			
			if( remove > 1 ) {
				battle.logme( (remove-1)+" an "+aship.getName()+" gedockte Schiffe fliehen n&auml;chste Runde\n" );
			}
		}
		
		return Result.OK;	
	}
}
