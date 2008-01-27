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
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;

/**
 * Leitet die Uebernahme des Kommandos der Schlacht ein, wenn der aktuelle Kommandant inaktiv ist
 * @author Christopher Jung
 *
 */
public class KSTakeCommandAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSTakeCommandAction() {
		this.requireActive(false);
		this.requireCommander(false);
	}
	
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		User user = context.getActiveUser();	
		
		if( (battle.getAlly(battle.getOwnSide()) == 0) || (battle.getAlly(battle.getOwnSide()) != user.getAlly()) ) {
			battle.logme( "Sie geh&ouml;ren nicht der kommandierenden Allianz an\n" );
			return RESULT_ERROR;
		}
		
		if( battle.getTakeCommand(battle.getOwnSide()) != 0 ) {
			battle.logme( "Es versucht bereits ein anderer Spieler das Kommando zu &uuml;bernehmen\n" );
			return RESULT_ERROR;
		}
		
		User oldCommander = context.createUserObject(battle.getCommander(battle.getOwnSide()));
		if( oldCommander.getInactivity() <= 0 ) {
			battle.logme( "Der kommandierende Spieler ist noch anwesend\n" );
			return RESULT_ERROR;
		}
		
		battle.setTakeCommand(battle.getOwnSide(), user.getId());

		battle.save(false);

		battle.logme("Versuche das Kommando zu &uuml;bernehmen...\n\nBitte Warten sie bis zum n&auml;chsten automatischen Rundenwechsel!");
				
		return RESULT_OK;		
	}
}
