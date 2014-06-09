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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Zeigt das Menue zur Uebergabe der Schlacht an einen anderen an der Schlacht
 * beteiligten Spieler.
 * @author Christopher Jung
 *
 */
public class KSMenuBattleConsignAction extends BasicKSMenuAction {
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();	
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		List<?> sideUsers = db.createQuery("select distinct u " +
				"from BattleShip as bs " +
				"join bs.ship.owner as u " +
			"where bs.battle= :battleId and bs.side= :sideId order by u.id")
			.setInteger("battleId", battle.getId())
			.setInteger("sideId", battle.getOwnSide())
			.list();
		
		Set<User> users = new LinkedHashSet<>();

		for (Object sideUser : sideUsers)
		{
			users.add((User) sideUser);
		}
		
		if( battle.getAlly(battle.getOwnSide()) > 0 ) {
			Ally ally = (Ally)db.get(Ally.class, battle.getAlly(battle.getOwnSide()));
			users.addAll(ally.getMembers());
		}
		
		for( User member : users ) {
			if( member.getId() == user.getId() ) {
				continue;
			}
			this.menuEntryAsk(t, Common._titleNoFormat(member.getName()),
								new Object[] {	"ship",		ownShip.getId(),
												"attack",	enemyShip.getId(),
												"ksaction",	"new_commander2",
												"newcom",	member.getId() },
								"Wollen sie das Kommando wirklich an "+Common._titleNoFormat(member.getName())+" &uuml;bergeben?" );
		}
		
		this.menuEntry(t, "zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"other" );
		
		return Result.OK;
	}
}
