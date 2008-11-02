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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Cheat eigenes Schiff regenerieren
 * @author Christopher Jung
 *
 */
public class KSCheatRegenerateOwnAction extends BasicKSAction {
	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		if( Configuration.getIntSetting("ENABLE_CHEATS") == 0 ) {
			context.addError("Cheats sind deaktiviert!");
			return RESULT_HALT;
		}
		
		BattleShip ownShip = battle.getOwnShip();

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		
		ShipTypeData ownShipType = ownShip.getTypeData();
		ownShip.getShip().setCrew(ownShipType.getCrew());
		ownShip.getShip().setHull(ownShipType.getHull());
		ownShip.getShip().setEnergy(ownShipType.getEps());
		ownShip.getShip().setShields(ownShipType.getShields());
		ownShip.getShip().setEngine(100);
		ownShip.getShip().setWeapons(100);
		ownShip.getShip().setSensors(100);
		ownShip.getShip().setComm(100);
		ownShip.getShip().setHeat(0);
		ownShip.getShip().setWeaponHeat("");
		
		ownShip.setHull(ownShip.getShip().getHull());
		ownShip.setShields(ownShip.getShip().getShields());
		ownShip.setEngine(100);
		ownShip.setWeapons(100);
		ownShip.setSensors(100);
		ownShip.setComm(100);
		ownShip.setCount(ownShipType.getShipCount());
		ownShip.setNewCount(0);
		ownShip.setAction(0);
		
		battle.logme( "CHEAT: Gegnerisches Schiff regeneriert\n" );
		battle.logenemy( "CHEAT: [color=green]"+ownShip.getName()+"[/color] regeneriert\n" );

		battle.logenemy("]]></action>\n");
		
		ownShip.getShip().recalculateShipStatus();
		
		return RESULT_OK;
	}
}
