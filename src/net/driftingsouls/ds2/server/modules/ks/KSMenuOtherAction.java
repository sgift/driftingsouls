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
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Zeigt das Menue fuer sonstige Aktionen an, welche unter keine andere Kategorie fallen
 * @author Christopher Jung
 *
 */
public class KSMenuOtherAction extends BasicKSMenuAction {
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		//Cheat-Menue
		if( Configuration.getIntSetting("ENABLE_CHEATS") != 0 ) {
			menuEntry("Cheats",	"ship",		ownShip.getInt("id"),
								"attack",	enemyShip.getInt("id"),
								"ksaction",	"cheats" );
		}

		//Alle Abdocken
		if( this.isPossible(battle, new KSUndockAllAction()) == RESULT_OK ) {
			menuEntry("Alle Abdocken<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 1 AP</span>", 
						"ship",		ownShip.getInt("id"),
						"attack",	enemyShip.getInt("id"),
						"ksaction",	"alleabdocken" );
		}

		//Schilde aufladen
		if( this.isPossible(battle, new KSMenuShieldsAction()) == RESULT_OK ) {
			menuEntry("Schilde aufladen",
						"ship",			ownShip.getInt("id"),
						"attack",		enemyShip.getInt("id"),
						"ksaction",		"shields" );
		}

		if( this.isPossible(battle, new KSMenuBatteriesAction()) == RESULT_OK ) {
			menuEntry("Batterien entladen",
						"ship",			ownShip.getInt("id"),
						"attack",		enemyShip.getInt("id"),
						"ksaction",		"batterien" );
		}

		//Kampf uebergeben
		menuEntry("Kampf &uuml;bergeben",	"ship",		ownShip.getInt("id"),
											"attack",	enemyShip.getInt("id"),
											"ksaction",	"new_commander" );

		//History
		menuEntry("Logbuch",	"ship",		ownShip.getInt("id"),
								"attack",	enemyShip.getInt("id"),
								"ksaction",	"history" );

		menuEntry("zur&uuml;ck",	"ship",		ownShip.getInt("id"),
									"attack",	enemyShip.getInt("id") );
		
		return RESULT_OK;
	}
}
