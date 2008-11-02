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

/**
 * Zeigt das Hauptmenue des KS an
 * @author Christopher Jung
 *
 */
public class KSMenuDefaultAction extends BasicKSMenuAction {
	@Override
	public int execute( Battle battle ) throws IOException {	
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		BattleShip ownShip = battle.getOwnShip();
		BattleShip enemyShip = battle.getEnemyShip();
		
		if( this.isPossible(battle, new KSMenuAttackAction()) == RESULT_OK ) {
			this.menuEntry("Angriff",	"ship",		ownShip.getId(),
										"attack",	enemyShip.getId(),
										"ksaction",	"attack" );
		}
		
		this.menuEntry("Flucht",	"ship",		ownShip.getId(),
									"attack",	enemyShip.getId(),
									"ksaction",	"flucht" );

		// Kampf beenden weil nur noch Zivilschiffe uebrig?
		if( this.isPossible(battle, new KSEndBattleCivilAction()) == RESULT_OK ) {
			this.menuEntry("Kampf beenden",	"ship",		ownShip.getId(),
											"attack",	enemyShip.getId(),
											"ksaction",	"endbattle" );
		}
			
		// Kampf beenden weil die eigene Streitmacht deutlich groesser ist?
		if( this.isPossible(battle, new KSEndBattleEqualAction()) == RESULT_OK ) {
			this.menuEntryAsk("Kampf beenden (unentschieden)", 
								new Object[] {	"ship",		ownShip.getId(),
												"attack",	enemyShip.getId(),
												"ksaction",	"endbattleequal" },
								"Wollen sie den Kampf wirklich mit einem Unentschieden beenden?" );
		}
		
		// Kapern?
		if( this.isPossible(battle, new KSKapernAction()) == RESULT_OK ) {
			this.menuEntry("Kapern", 
									"ship",			ownShip.getId(),
									"attack",		enemyShip.getId(),
									"ksaction",		"kapern" );
		}
		
		// Die zweiten Reihe stuermen
		if( this.isPossible(battle, new KSSecondRowAttackAction()) == RESULT_OK ) {
			this.menuEntry("Sturmangriff", 
									"ship",			ownShip.getId(),
									"attack",		enemyShip.getId(),
									"ksaction",		"secondrowattack" );
		}
		
		// Zur zweiten Reihe vorruecken
		if( this.isPossible(battle, new KSSecondRowEngageAction()) == RESULT_OK ) {
			this.menuEntry("Vorr&uuml;cken", 
									"ship",			ownShip.getId(),
									"attack",		enemyShip.getId(),
									"ksaction",		"secondrowengage" );
		}
		
		// Zweite Reihe verlassen
		if( this.isPossible(battle, new KSLeaveSecondRowAction()) == RESULT_OK ) {
			this.menuEntry("Zweite Reihe verlassen", 
									"ship",			ownShip.getId(),
									"attack",		enemyShip.getId(),
									"ksaction",		"leavesecondrow" );
		}
		
		// Zweite Reihe
		if( this.isPossible(battle, new KSSecondRowAction()) == RESULT_OK ) {
			this.menuEntry("In zweite Reihe verlegen", 
									"ship",			ownShip.getId(),
									"attack",		enemyShip.getId(),
									"ksaction",		"secondrow" );
		}

		menuEntry("sonstiges",	"ship",		ownShip.getId(),
								"attack",	enemyShip.getId(),
								"ksaction",	"other" );
	
		return RESULT_OK;
	}
}
