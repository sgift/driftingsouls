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

import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Laesst das gerade ausgewaehlte Schiff fliehen
 * @author Christopher Jung
 *
 */
public class KSFluchtSingleAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSFluchtSingleAction() {
		this.requireOwnShipReady(true);
	}
	
	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		SQLResultRow ownShip = battle.getOwnShip();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
	
		if( fluchtmode.equals("current") && (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
			battle.logme( "Die Flucht nach einem Sturmangriff ist nicht m&ouml;glich\n" );
			return RESULT_ERROR;
		}
		
		if( fluchtmode.equals("current") && (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
			battle.logme( "Die Flucht nach einem Sturmangriff ist nicht m&ouml;glich\n" );
			return RESULT_ERROR;
		}
	
		if( (ownShip.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
			battle.logme( "Dieses Schiff explodiert am Ende der Runde\n" );
			return RESULT_ERROR;
		}
	
		if( ownShip.getInt("engine") == 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return RESULT_ERROR;
		}
		
		if( ownShip.getString("docked").length() > 0 ) {
			battle.logme( "Sie k&ouml;nnen nicht mit gedockten/gelandeten Schiffen fliehen\n" );
			return RESULT_ERROR;
		}
	
		int curr_engines = db.first("SELECT engine FROM battles_ships WHERE shipid=",ownShip.getInt("id")).getInt("engine");
		
		if( curr_engines <= 0 ) {
			battle.logme( "Das Schiff kann nicht fliehen, da der Antrieb zerst&ouml;rt ist\n" );
			return RESULT_ERROR;
		}
		
		SQLResultRow ownShipType = Ships.getShipType( ownShip );
		 
		if( (ownShipType.getInt("crew") > 0) && (ownShip.getInt("crew") < (int)(ownShipType.getInt("crew")/4d)) ) {
			battle.logme( "Nicht genug Crew um zu fliehen\n" );
			return RESULT_ERROR;
		}		
		
		if( fluchtmode.equals("current") && ownShip.getBoolean("battleAction") ) {
			battle.logme( "Das Schiff kann nicht fliehen, da es bereits diese Runde eingesetzt wurde\n" );
			return RESULT_ERROR;
		}
		
		boolean gotone = false;
		if( Ships.hasShipTypeFlag(ownShipType, Ships.SF_DROHNE) ) {
			List<SQLResultRow> ownShips = battle.getOwnShips();
			for( int i=0; i < ownShips.size(); i++ ) {
				SQLResultRow aship = ownShips.get(i);
				SQLResultRow ashiptype = Ships.getShipType(aship);
				if( Ships.hasShipTypeFlag(ashiptype, Ships.SF_DROHNEN_CONTROLLER) ) {
					gotone = true;
					break;	
				}
			}
		}
		else {
			gotone = true;	
		}
		
		if( !gotone ) {
			battle.logme( "Sie ben&ouml;tigen ein Drohnen-Kontrollschiff um fliehen zu k&ouml;nnen\n" );
			return RESULT_ERROR;
		}
		
		int fluchtflag = 0;
		if( fluchtmode.equals("current") ) {
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
		
			battle.logme( ownShip.getString("name")+" fl&uuml;chtet\n" );
			battle.logenemy( Battle.log_shiplink(ownShip)+" flieht\n" );
			
			ownShip.put("action", ownShip.getInt("action") | Battle.BS_FLUCHT);
			fluchtflag = Battle.BS_FLUCHT;
		}
		else {
			battle.logme( ownShip.getString("name")+" wird n&auml;chste Runde fl&uuml;chten\n" );
			
			ownShip.put("action", ownShip.getInt("action") | Battle.BS_FLUCHTNEXT);
			fluchtflag = Battle.BS_FLUCHTNEXT;
		}
		
		db.update("UPDATE battles_ships SET action=",ownShip.getInt("action")," WHERE shipid=",ownShip.getInt("id"));

		int remove = 1;
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int j=0; j < ownShips.size(); j++ ) {
			SQLResultRow s = ownShips.get(j);
			if( (s.getString("docked").length() > 0) && (s.getString("docked").equals(""+ownShip.getInt("id")) || s.getString("docked").equals("l "+ownShip.getInt("id")) ) ) {
				s.put("action", s.getInt("action") | fluchtflag);
				db.update("UPDATE battles_ships SET action=",s.getInt("action")," WHERE shipid=",s.getInt("id"));
				
				remove++;
			}
		}
		
		if( fluchtmode.equals("current") && (remove > 1) ) {
			battle.logme( (remove-1)+" an "+ownShip.getString("name")+" gedockte Schiffe fliehen\n" );
			battle.logenemy( (remove-1)+" an "+ownShip.getString("name")+" gedockte Schiffe fliehen\n" );
		}
		else if( fluchtmode.equals("next") && (remove > 1) ) {
			battle.logme( (remove-1)+" an "+ownShip.getString("name")+" gedockte Schiffe werden n&auml;chste Runde fliehen\n" );
		}
		
		if( fluchtmode.equals("current") ) {
			battle.logenemy("]]></action>\n");
		}
	
		return RESULT_OK;
	}
}
