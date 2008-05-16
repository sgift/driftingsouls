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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Entlaedt die Reservebatterien auf dem gerade ausgewaehlten Schiff
 * @author Christopher Jung
 *
 */
public class KSDischargeBatteriesSingleAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSDischargeBatteriesSingleAction() {
	}
	
	@Override
	public int validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		Cargo mycargo = ownShip.getCargo();
		if( mycargo.hasResource( Resources.BATTERIEN ) && (ownShip.getShip().getEnergy() < ownShipType.getEps()) ) {
			return RESULT_OK;
		}
		return RESULT_ERROR;	
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
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		Cargo mycargo = ownShip.getCargo();
		
		long batterien = mycargo.getResourceCount( Resources.BATTERIEN );

		if( batterien > ownShipType.getEps()-ownShip.getShip().getEnergy() ) {
			batterien = ownShipType.getEps()-ownShip.getShip().getEnergy();
		}	

		ownShip.getShip().setEnergy((int)(ownShip.getShip().getEnergy()+batterien));
		ownShip.getShip().setBattleAction(true);
	
		mycargo.substractResource( Resources.BATTERIEN, batterien );
		mycargo.addResource( Resources.LBATTERIEN, batterien );
		
		ownShip.getShip().setCargo(mycargo);

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

			
		battle.logme( ownShip.getName()+": "+batterien+" Reservebatterien entladen\n" );
		battle.logenemy(Battle.log_shiplink(ownShip.getShip())+": Reservebatterien entladen\n");
		
		ownShip.getShip().recalculateShipStatus();
		
		battle.logenemy("]]></action>\n");
		battle.resetInactivity();
		
		return RESULT_OK;
	}
}
