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
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;

/**
 * Zeigt das Hauptmenue des KS an
 * @author Christopher Jung
 *
 */
public class KSMenuDefaultAction extends BasicKSMenuAction {
	@Override
	public int execute( Battle battle ) {	
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		if( this.isPossible(battle, new KSMenuAttackAction()) == RESULT_OK ) {
			this.menuEntry("Angriff",	"ship",		ownShip.getInt("id"),
										"attack",	enemyShip.getInt("id"),
										"ksaction",	"attack" );
		}
		
		this.menuEntry("Flucht",	"ship",		ownShip.getInt("id"),
									"attack",	enemyShip.getInt("id"),
									"ksaction",	"flucht" );

		// Kampf beenden weil nur noch Zivilschiffe uebrig?
		if( this.isPossible(battle, new KSEndBattleCivilAction()) == RESULT_OK ) {
			this.menuEntry("Kampf beenden",	"ship",		ownShip.getInt("id"),
											"attack",	enemyShip.getInt("id"),
											"ksaction",	"endbattle" );
		}
			
		// Kampf beenden weil die eigene Streitmacht deutlich groesser ist?
		if( this.isPossible(battle, new KSEndBattleEqualAction()) == RESULT_OK ) {
			this.menuEntryAsk("Kampf beenden (unentschieden)", 
								new Object[] {	"ship",		ownShip.getInt("id"),
												"attack",	enemyShip.getInt("id"),
												"ksaction",	"endbattleequal" },
								"Wollen sie den Kampf wirklich mit einem Unentschieden beenden?" );
		}
		
		// Kapern?
		if( this.isPossible(battle, new KSKapernAction()) == RESULT_OK ) {
			this.menuEntry("Kapern<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 5 AP</span>", 
									"ship",			ownShip.getInt("id"),
									"attack",		enemyShip.getInt("id"),
									"ksaction",		"kapern" );
		}
		
		// Die zweiten Reihe stuermen
		if( this.isPossible(battle, new KSSecondRowAttackAction()) == RESULT_OK ) {
			this.menuEntry("Sturmangriff<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 300 AP</span>", 
									"ship",			ownShip.getInt("id"),
									"attack",		enemyShip.getInt("id"),
									"ksaction",		"secondrowattack" );
		}
		
		// Zur zweiten Reihe vorruecken
		if( this.isPossible(battle, new KSSecondRowEngageAction()) == RESULT_OK ) {
			this.menuEntry("Vorr&uuml;cken<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 100 AP</span>", 
									"ship",			ownShip.getInt("id"),
									"attack",		enemyShip.getInt("id"),
									"ksaction",		"secondrowengage" );
		}
		
		// Zweite Reihe verlassen
		if( this.isPossible(battle, new KSLeaveSecondRowAction()) == RESULT_OK ) {
			this.menuEntry("Zweite Reihe verlassen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 1 AP</span>", 
									"ship",			ownShip.getInt("id"),
									"attack",		enemyShip.getInt("id"),
									"ksaction",		"leavesecondrow" );
		}
		
		// Zweite Reihe
		if( this.isPossible(battle, new KSSecondRowAction()) == RESULT_OK ) {
			this.menuEntry("In zweite Reihe verlegen<br /><span style=\"font-weight:normal; font-size:14px\">Kosten: 1 AP</span>", 
									"ship",			ownShip.getInt("id"),
									"attack",		enemyShip.getInt("id"),
									"ksaction",		"secondrow" );
		}

		menuEntry("sonstiges",	"ship",		ownShip.getInt("id"),
								"attack",	enemyShip.getInt("id"),
								"ksaction",	"other" );
	
		return RESULT_OK;
	}
}
