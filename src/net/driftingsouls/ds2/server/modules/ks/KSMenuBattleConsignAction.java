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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Zeigt das Menue zur Uebergabe der Schlacht an einen anderen an der Schlacht
 * beteiligten Spieler
 * @author Christopher Jung
 *
 */
public class KSMenuBattleConsignAction extends BasicKSMenuAction {
	@Override
	public int execute(Battle battle) {		
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();	
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		String query =  "SELECT DISTINCT u.id FROM users u WHERE id IN (SELECT s.owner FROM ships s JOIN battles_ships bs ON s.id=bs.shipid WHERE s.battle="+battle.getID()+" AND bs.side="+battle.getOwnSide()+")";
		if( battle.getAlly(battle.getOwnSide()) > 0 ) {
			query += " OR ally="+battle.getAlly(battle.getOwnSide());
		}
		query += " ORDER BY u.id";
		
		SQLQuery userQuery = context.getDatabase().query(query);
		while( userQuery.next() ) {
			User member = (User)context.getDB().get(User.class, userQuery.getInt("id"));
			if( member.getId() == user.getId() ) {
				continue;
			}
			this.menuEntryAsk( Common._titleNoFormat(member.getName()),
								new Object[] {	"ship",		ownShip.getInt("id"),
												"attack",	enemyShip.getInt("id"),
												"ksaction",	"new_commander2",
												"newcom",	member.getId() },
								"Wollen sie das Kommando wirklich an "+Common._titleNoFormat(member.getName())+" &uuml;bergeben?" );
		}
		userQuery.free();
		
		this.menuEntry("zur&uuml;ck",
				"ship",		ownShip.getInt("id"),
				"attack",	enemyShip.getInt("id"),
				"ksaction",	"other" );
		
		return RESULT_OK;
	}
}
