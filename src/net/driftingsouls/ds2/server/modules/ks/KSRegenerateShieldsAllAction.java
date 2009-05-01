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
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Laedt die Schilde aller eigener Schiffe auf.
 * @author Christopher Jung
 *
 */
public class KSRegenerateShieldsAllAction extends BasicKSAction {
	/**
	 * Prueft, ob das Schiff seine Schilde aufladen soll oder nicht.
	 * @param ship Das Schiff
	 * @param shiptype Der Schiffstyp
	 * @return <code>true</code>, wenn das Schiff seine Schilde aufladen soll
	 */
	protected boolean validateShipExt( BattleShip ship, ShipTypeData shiptype ) {
		// Extension Point
		return true;
	}
	
	@Override
	public final int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		int shipcount = 0;
		StringBuilder eshieldlog = new StringBuilder();
		
		List<BattleShip> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			BattleShip ownShip = ownShips.get(i);
						
			if( ownShip.getShip().getEnergy() < 1 ) {
				continue;
			}
			
			ShipTypeData ownShipType = ownShip.getTypeData();
			
			if( ownShipType.getShields() < 1 ) {
				continue;
			}
			
			if( ownShip.getShip().getShields() >= ownShipType.getShields() ) {
				continue;
			}
			
			if( !this.validateShipExt(ownShip, ownShipType) ) {
				continue;
			}
	
			int shieldfactor = 10;
			if( ownShipType.getShields() > 1000 ) {
				shieldfactor = 100;
			}
	
			int load = 0;
			if( ownShip.getShip().getEnergy() < Math.ceil((ownShipType.getShields()-ownShip.getShip().getShields())/(double)shieldfactor) ) {
				load = ownShip.getShip().getEnergy();
			}
			else {
				load = (int)Math.ceil((ownShipType.getShields()-ownShip.getShip().getShields())/(double)shieldfactor);
			}

			ownShip.getShip().setEnergy(ownShip.getShip().getEnergy() - load);
			ownShip.getShip().setShields(ownShip.getShip().getShields() + load*shieldfactor);
			if( ownShip.getShip().getShields() > ownShipType.getShields() ) {
				ownShip.getShip().setShields(ownShipType.getShields());
			}
	
			battle.logme( ownShip.getName()+": Schilde bei "+ownShip.getShip().getShields()+"/"+ownShipType.getShields()+"\n" );
			eshieldlog.append(Battle.log_shiplink(ownShip.getShip())+": Schilde aufgeladen\n");
	
			ownShip.getShip().setBattleAction(true);
	
			int curShields = ownShip.getShields();
			curShields += load*shieldfactor;
			if( curShields > ownShipType.getShields() ) {
				curShields = ownShipType.getShields();
			}
			ownShip.setShields(curShields);
			
			ownShip.getShip().recalculateShipStatus();

			shipcount++;
		}

		if( shipcount > 0 ) {	
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(eshieldlog.toString());
			battle.logenemy("]]></action>\n");
		}
	
		return RESULT_OK;
	}
}
