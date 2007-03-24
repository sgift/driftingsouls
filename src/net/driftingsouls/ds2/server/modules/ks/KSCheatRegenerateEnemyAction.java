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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Cheat Gegner regenerieren
 * @author Christopher Jung
 *
 */
public class KSCheatRegenerateEnemyAction extends BasicKSAction {
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		if( Configuration.getIntSetting("ENABLE_CHEATS") == 0 ) {
			context.addError("Cheats sind deaktiviert!");
			return RESULT_HALT;
		}
		
		Database db = context.getDatabase();
		SQLResultRow enemyShip = battle.getEnemyShip();

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

		SQLResultRow enemyShipType = ShipTypes.getShipType( enemyShip );
		enemyShip.put("crew", enemyShipType.getInt("crew"));
		enemyShip.put("hull", enemyShipType.getInt("hull"));
		enemyShip.put("e", enemyShipType.getInt("eps"));
		enemyShip.put("shields", enemyShipType.getInt("shields"));
		enemyShip.put("engine", 100);
		enemyShip.put("weapons", 100);
		enemyShip.put("sensors", 100);
		enemyShip.put("comm", 100);
		enemyShip.put("s", 0);
		enemyShip.put("heat", "");
		db.update("UPDATE ships SET crew=",enemyShip.getInt("crew"),",hull=",enemyShip.getInt("hull"),",e=",enemyShip.getInt("e"),",shields=",enemyShip.getInt("shields"),",engine=",enemyShip.getInt("engine"),",weapons=",enemyShip.getInt("weapons"),",sensors=",enemyShip.getInt("sensors"),",comm=",enemyShip.getInt("comm"),",s=",enemyShip.getInt("s"),",heat='",enemyShip.getString("heat"),"' WHERE id>0 AND id=",enemyShip.getInt("id"));
		db.update("UPDATE battles_ships SET hull=",enemyShip.getInt("hull"),",shields=",enemyShip.getInt("shields"),",engine=",enemyShip.getInt("engine"),",weapons=",enemyShip.getInt("weapons"),",sensors=",enemyShip.getInt("sensors"),",comm=",enemyShip.getInt("comm"),",count=",enemyShipType.getInt("shipcount"),",newcount=",enemyShipType.getInt("shipcount")," WHERE shipid=",enemyShip.getInt("id"));
		battle.logme( "CHEAT: Gegnerisches Schiff regeneriert\n" );
		battle.logenemy( "CHEAT: [color=green]"+enemyShip.getString("name")+"[/color] regeneriert\n" );

		battle.logenemy("]]></action>\n");
		
		enemyShip.put("status", Ships.recalculateShipStatus(enemyShip.getInt("id")));

		return RESULT_OK;
	}
}
