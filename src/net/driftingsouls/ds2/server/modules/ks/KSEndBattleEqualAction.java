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
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

import org.hibernate.Session;

/**
 * Ermoeglicht das Beenden der Schlacht im Falle vom einer klaren militaerischen Uebermacht gegenueber dem Gegner.
 * @author Christopher Jung
 *
 */
public class KSEndBattleEqualAction extends BasicKSAction {
	@Override
	public int validate(Battle battle) {
		//Check ob man nicht der Angreifer ist
		if( battle.getOwnSide() == 0 ) {
			return RESULT_ERROR;
		}
		
		ConfigValue endTieModifier = (ConfigValue)getDB().get(ConfigValue.class, "endtiemodifier");
		if((battle.getBattleValue(Side.ENEMY) == 0) || (battle.getBattleValue(Side.OWN) > (battle.getBattleValue(Side.ENEMY) * Integer.valueOf(endTieModifier.getValue())))) 
		{
			return RESULT_OK;
		}

		return RESULT_ERROR;
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		
		context.getResponse().getWriter().append("Sie haben die Schlacht mit einem unentschieden beendet");
		
		PM.send(user, battle.getCommander(battle.getEnemySide()).getId(), "Schlacht beendet", "Der Gegner hat die Schlacht mit einem unentschieden beendet. Somit ist die Schlacht bei "+battle.getLocation().displayCoordinates(false)+" gegen [userprofile="+user.getId()+"]"+user.getName()+"[/userprofile] zuende!");
	
		// Schlacht beenden -> 0 Siege fuer mich; 0 Niederlagen fuer den Gegner
		battle.endTurn(true);
		battle.endBattle( 0, 0, true );
	
		return RESULT_HALT;
	}
	
	private Session getDB()
	{
		return ContextMap.getContext().getDB();
	}
}
