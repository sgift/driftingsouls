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

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Zeigt das Cheatmenue an
 * @author Christopher Jung
 *
 */
public class KSMenuCheatsAction extends BasicKSMenuAction {
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
		BattleShip enemyShip = battle.getEnemyShip();

		this.menuEntry("+100 AP",	
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"cheat_ap" );
											
		this.menuEntry("Schiff regenerieren",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"cheat_regenerate" );
														
		this.menuEntry("Gegner regenerieren",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"cheat_regenerateenemy" );
														
		this.menuEntry("zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"other" );

		return RESULT_OK;		
	}
}
