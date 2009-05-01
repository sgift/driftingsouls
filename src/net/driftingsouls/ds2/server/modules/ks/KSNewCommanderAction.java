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
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;

/**
 * Uebergibt das Kommando ueber die Schlacht an einen anderen Spieler.
 * @author Christopher Jung
 *
 */
public class KSNewCommanderAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 */
	public KSNewCommanderAction() {
		this.requireActive(false);
	}
	
	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();		

		int newcom = context.getRequest().getParameterInt("newcom");
		User com = (User)context.getDB().get(User.class, newcom);
		
		if( user.getId() == com.getId() ) {
			battle.logme( "Sie k&ouml;nnen die Leitung der Schlacht nicht an sich selbst &uuml;bertragen\n" );
			return RESULT_ERROR;
		}
		
		if( (battle.getAlly(battle.getOwnSide()) == 0) || 
			((com.getAlly() != null) && (com.getAlly().getId() != battle.getAlly(battle.getOwnSide()))) ) {
			
			boolean found = false;
			List<BattleShip> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				if( ownShips.get(i).getOwner() == com ) {
					found = true;
					break;
				}
			}
			if( !found ) {
				battle.logme( "Sie k&ouml;nnen diesem Spieler nicht die Leitung der Schlacht &uuml;bertragen!\n" );
				return RESULT_ERROR;
			}
		}
		
		if( (com.getVacationCount() != 0) && (com.getWait4VacationCount() == 0) ) {
			battle.logme( "Der Spieler befindet sich im Vacation-Modus!\n" );
			return RESULT_ERROR;
		} 

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

		PM.send(user, com.getId(), "Schlacht &uuml;bergeben", "Ich habe dir die Leitung der Schlacht bei "+battle.getLocation()+" &uuml;bergeben.");

		battle.logenemy("[userprofile="+com.getId()+",profile_alog]"+Common._titleNoFormat(com.getName())+"[/userprofile] kommandiert nun die gegnerischen Truppen\n\n");

		battle.setCommander(battle.getOwnSide(), com);

		battle.logenemy("]]></action>\n");

		battle.logenemy("<side"+(battle.getOwnSide()+1)+" commander=\""+battle.getCommander(battle.getOwnSide()).getId()+"\" ally=\""+battle.getAlly(battle.getOwnSide())+"\" />\n");

		battle.setTakeCommand(battle.getOwnSide(), 0);
		
		return RESULT_OK;
	}
}
