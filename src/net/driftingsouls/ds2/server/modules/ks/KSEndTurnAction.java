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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Beendet die Kampfrunde des aktuellen Spielers
 * @author Christopher Jung
 *
 */
public class KSEndTurnAction extends BasicKSAction {
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();

		if( battle.isReady(battle.getEnemySide()) ) {
			if( !battle.endTurn(true) ) {
				return RESULT_HALT;
			}

			battle.logenemy("<endturn type=\"all\" side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\" />\n");
			battle.logme( "++++ Runde beendet ++++" );
			battle.addComMessage(battle.getEnemySide(), "++++ "+Common._titleNoFormat(user.getName())+" hat die Runde beendet ++++\n\n");
			
			battle.resetInactivity();
		}
		else {
			battle.logenemy("<endturn type=\"own\" side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\" />\n");

			battle.logme("Zug beendet - warte auf Gegner");
			battle.addComMessage(battle.getEnemySide(), Common._titleNoFormat(user.getName())+" hat seinen Zug beendet\n\n");
			
			battle.setReady(battle.getOwnSide(), true);
			battle.resetInactivity();
		}
		
		return RESULT_OK;
	}
}
