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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;
import java.util.HashMap;

/**
 * Cheat Gegner regenerieren.
 * @author Christopher Jung
 *
 */
public class KSCheatRegenerateEnemyAction extends BasicKSAction {
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		if( !new ConfigService().getValue(WellKnownConfigValue.ENABLE_CHEATS) ) {
			context.addError("Cheats sind deaktiviert!");
			return Result.HALT;
		}
		
		BattleShip enemyShip = battle.getEnemyShip();

		ShipTypeData enemyShipType = enemyShip.getTypeData();
		enemyShip.getShip().setCrew(enemyShipType.getCrew());
		enemyShip.getShip().setHull(enemyShipType.getHull());
		enemyShip.getShip().setEnergy(enemyShipType.getEps());
		enemyShip.getShip().setShields(enemyShipType.getShields());
		enemyShip.getShip().setEngine(100);
		enemyShip.getShip().setWeapons(100);
		enemyShip.getShip().setSensors(100);
		enemyShip.getShip().setComm(100);
		enemyShip.getShip().setHeat(0);
		enemyShip.getShip().setWeaponHeat(new HashMap<>());
		
		enemyShip.setHull(enemyShip.getShip().getHull());
		enemyShip.setShields(enemyShip.getShip().getShields());
		enemyShip.setEngine(100);
		enemyShip.setWeapons(100);
		enemyShip.setSensors(100);
		enemyShip.setComm(100);
		enemyShip.removeAllFlags();
		
		battle.logme( "CHEAT: Gegnerisches Schiff regeneriert\n" );
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), "CHEAT: [color=green]"+enemyShip.getName()+"[/color] regeneriert"));

		enemyShip.getShip().recalculateShipStatus();

		return Result.OK;
	}
}
