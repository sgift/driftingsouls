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
import net.driftingsouls.ds2.server.battles.SchlachtLogRundeBeendet;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Beendet die Kampfrunde des aktuellen Spielers.
 * @author Christopher Jung
 *
 */
@Component
public class KSEndTurnAction extends BasicKSAction {
	public KSEndTurnAction(BattleService battleService) {
		super(battleService, null);
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}

		if( battle.isReady(battle.getEnemySide()) )
		{
			if( !getBattleService().endTurn(battle,true) ) {
				return Result.HALT;
			}

			getBattleService().log(battle, new SchlachtLogRundeBeendet(battle.getOwnSide(), SchlachtLogRundeBeendet.Modus.ALLE));
			getBattleService().logme(battle, "++++ Runde beendet ++++" );
		}
		else
		{
			getBattleService().log(battle, new SchlachtLogRundeBeendet(battle.getOwnSide(), SchlachtLogRundeBeendet.Modus.EIGENE));

			getBattleService().logme(battle,"Zug beendet - warte auf Gegner");
			
			battle.setReady(battle.getOwnSide(), true);
		}
		
		return Result.OK;
	}
}
