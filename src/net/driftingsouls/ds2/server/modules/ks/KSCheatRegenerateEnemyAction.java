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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Cheat Gegner regenerieren.
 * @author Christopher Jung
 *
 */
@Configurable
public class KSCheatRegenerateEnemyAction extends BasicKSAction {
	
	private Configuration config;
	
    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config) 
    {
    	this.config = config;
    }
    
	@Override
	public Result execute(Battle battle) throws IOException {
		Result result = super.execute(battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		if( config.getInt("ENABLE_CHEATS") == 0 ) {
			context.addError("Cheats sind deaktiviert!");
			return Result.HALT;
		}
		
		BattleShip enemyShip = battle.getEnemyShip();

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

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
		enemyShip.getShip().setWeaponHeat("");
		
		enemyShip.setHull(enemyShip.getShip().getHull());
		enemyShip.setShields(enemyShip.getShip().getShields());
		enemyShip.setEngine(100);
		enemyShip.setWeapons(100);
		enemyShip.setSensors(100);
		enemyShip.setComm(100);
		enemyShip.setAction(0);
		
		battle.logme( "CHEAT: Gegnerisches Schiff regeneriert\n" );
		battle.logenemy( "CHEAT: [color=green]"+enemyShip.getName()+"[/color] regeneriert\n" );

		battle.logenemy("]]></action>\n");
		
		enemyShip.getShip().recalculateShipStatus();

		return Result.OK;
	}
}
