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
 * Cheat eigenes Schiff regenerieren
 * @author Christopher Jung
 *
 */
public class KSCheatRegenerateOwnAction extends BasicKSAction {
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
		SQLResultRow ownShip = battle.getOwnShip();

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		
		SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
		ownShip.put("crew", ownShipType.getInt("crew"));
		ownShip.put("hull", ownShipType.getInt("hull"));
		ownShip.put("e", ownShipType.getInt("eps"));
		ownShip.put("shields", ownShipType.getInt("shields"));
		ownShip.put("engine", 100);
		ownShip.put("weapons", 100);
		ownShip.put("sensors", 100);
		ownShip.put("comm", 100);
		ownShip.put("s", 0);
		ownShip.put("heat", "");
		db.update("UPDATE ships SET crew=",ownShip.getInt("crew"),",hull=",ownShip.getInt("hull"),",e=",ownShip.getInt("e"),",shields=",ownShip.getInt("shields"),",engine=",ownShip.getInt("engine"),",weapons=",ownShip.getInt("weapons"),",sensors=",ownShip.getInt("sensors"),",comm=",ownShip.getInt("comm"),",s=",ownShip.getInt("s"),",heat='",ownShip.getString("heat"),"' WHERE id>0 AND id=",ownShip.getInt("id"));
		db.update("UPDATE battles_ships SET hull=",ownShip.getInt("hull"),",shields=",ownShip.getInt("shields"),",engine=",ownShip.getInt("engine"),",weapons=",ownShip.getInt("weapons"),",sensors=",ownShip.getInt("sensors"),",comm=",ownShip.getInt("comm"),",count=",ownShipType.getInt("shipcount"),",newcount=",ownShipType.getInt("shipcount")," WHERE shipid=",ownShip.getInt("id"));
		battle.logme( "CHEAT: Gegnerisches Schiff regeneriert\n" );
		battle.logenemy( "CHEAT: [color=green]"+ownShip.getString("name")+"[/color] regeneriert\n" );

		battle.logenemy("]]></action>\n");
		
		ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));
		
		return RESULT_OK;
	}
}
