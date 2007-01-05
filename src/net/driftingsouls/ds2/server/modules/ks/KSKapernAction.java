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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserFlagschiffLocation;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.ships.ShipClasses;

public class KSKapernAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSKapernAction() {
		this.requireAP(5);
		this.requireOwnShipReady(true);
	}
	
	@Override
	public int validate(Battle battle) {
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow enemyShip = battle.getEnemyShip();
		
		if( (ownShip.getInt("action") & Battle.BS_SECONDROW) != 0 ||
			(enemyShip.getInt("action") & Battle.BS_SECONDROW) != 0 ) {
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		if( (ownShip.getInt("weapons") == 0) || (ownShip.getInt("engine") == 0) || 
			(ownShip.getInt("crew") <= 0) || (ownShip.getInt("action") & Battle.BS_FLUCHT) != 0 ||
			(ownShip.getInt("action") & Battle.BS_JOIN) != 0 || (enemyShip.getInt("action") & Battle.BS_FLUCHT) != 0 ||
			(enemyShip.getInt("action") & Battle.BS_JOIN) != 0 || (enemyShip.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
			return RESULT_ERROR;
		}
		
		SQLResultRow enemyShipType = Ships.getShipType( enemyShip );
	
		// Geschuetze sind nicht kaperbar
		if( (enemyShipType.getInt("class") == ShipClasses.GESCHUETZ.ordinal() ) || 
			((enemyShipType.getInt("cost") != 0) && (enemyShip.getInt("engine") != 0) && (enemyShip.getInt("crew") != 0)) ||
			(ownShip.getInt("crew") == 0) || Ships.hasShipTypeFlag(enemyShipType, Ships.SF_NICHT_KAPERBAR) ) {
			return RESULT_ERROR;
		}
		
		if( enemyShipType.getInt("crew") == 0 ) {
			return RESULT_ERROR;
		}
	
		if( enemyShip.getString("docked").length() > 0 ) {
			if( enemyShip.getString("docked").charAt(0) == 'l' ) {
				return RESULT_ERROR;
			} 

			SQLResultRow mastership = db.first("SELECT engine,crew FROM ships WHERE id>0 AND id=",enemyShip.getString("docked"));
			if( (mastership.getInt("engine") != 0) && (mastership.getInt("crew") != 0) ) {
				return RESULT_ERROR;
			}
		}
	
		// IFF-Stoersender
		boolean disableIFF = enemyShip.getString("status").indexOf("disable_iff") > -1;	
		
		if( disableIFF ) {
			return RESULT_ERROR;
		}
	
		//Flagschiff?
		User ownuser = context.getActiveUser();
		User enemyuser = context.createUserObject(enemyShip.getInt("owner"));
	
		UserFlagschiffLocation flagschiffstatus = enemyuser.getFlagschiff();
		
		if( !ownuser.hasFlagschiffSpace() && (flagschiffstatus.getID() == enemyShip.getInt("id")) ) {
			return RESULT_ERROR;
		}
		
		return RESULT_OK;
	}

	@Override
	public int execute(Battle battle) {
		// TODO
		Common.stub();
		return RESULT_OK;
	}
}
