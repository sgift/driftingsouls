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
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Laesst alle Schiffe einer Seite fliehen
 * @author Christopher Jung
 *
 */
public class KSFluchtAllAction extends BasicKSAction {
	
	/**
	 * Prueft, ob das Schiff fliehen kann oder nicht
	 * @param fluchtmode Der Zeitpunkt der Flucht (<code>current</code> oder <code>next</code>)
	 * @param ship Das Schiff
	 * @param shiptype Der Schiffstyp
	 * @return <code>true</code>, wenn das Schiff fliehen kann
	 */
	protected boolean validateShipExt( String fluchtmode, SQLResultRow ship, SQLResultRow shiptype ) {
		// Extension Point
		return true;
	}
	
	@Override
	final public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
		
		if( fluchtmode.equals("current") && (battle.getOwnSide() == 0) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_1) ) {
			battle.logme( "Die Flucht nach einem Sturmangriff ist nicht m&ouml;glich\n" );
			return RESULT_ERROR;
		}
		
		if( fluchtmode.equals("current") && (battle.getOwnSide() == 1) && battle.hasFlag(Battle.FLAG_DROP_SECONDROW_0) ) {
			battle.logme( "Die Flucht nach einem Sturmangriff ist nicht m&ouml;glich\n" );
			return RESULT_ERROR;
		}

		int shipcount = 0;
		StringBuilder efluchtlog = new StringBuilder();
		Boolean gotone = null;
		
		int fluchtflag = Battle.BS_FLUCHT;
		if( fluchtmode.equals("next") ) {
			fluchtflag = Battle.BS_FLUCHTNEXT;
		}		
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			if( (aship.getInt("action") & Battle.BS_DESTROYED) != 0 ) {
				continue;
			}
			
			if( (aship.getInt("action") & Battle.BS_FLUCHT) != 0 ) {
				continue;
			}
			
			if( fluchtmode.equals("next") && (aship.getInt("action") & Battle.BS_FLUCHTNEXT) != 0 ) {
				continue;
			}
			
			if( (aship.getInt("action") & Battle.BS_JOIN) != 0 ) {
				continue;
			}

			if( fluchtmode.equals("current") && aship.getBoolean("battleAction") ) {
				continue;
			}
			 
			if( aship.getInt("engine") == 0 ) {
				continue;
			}
			
			if( aship.getString("docked").length() > 0 ) {
				continue;
			}
			
			int curr_engines = db.first("SELECT engine FROM battles_ships WHERE shipid=",aship.getInt("id")).getInt("engine");
			
			if( curr_engines <= 0 ) {
				continue;
			} 
			
			SQLResultRow ashipType = ShipTypes.getShipType(aship);
			 
			if( (ashipType.getInt("crew") > 0) && (aship.getInt("crew") < (int)(ashipType.getInt("crew")/4d)) ) {
				continue;
			}
	
			if( (gotone == null) && ShipTypes.hasShipTypeFlag(ashipType, ShipTypes.SF_DROHNE) ) {
				gotone = Boolean.FALSE;
				for( int j=0; j < ownShips.size(); j++ ) {
					SQLResultRow as = ownShips.get(j);
					SQLResultRow ast = ShipTypes.getShipType(as);
					if( ShipTypes.hasShipTypeFlag(ast, ShipTypes.SF_DROHNEN_CONTROLLER) ) {
						gotone = Boolean.TRUE;
						break;	
					}
				}
			}
			if( (gotone == Boolean.FALSE) && ShipTypes.hasShipTypeFlag(ashipType, ShipTypes.SF_DROHNE) ) {
				continue;
			}
			
			if( !validateShipExt(fluchtmode, aship, ashipType) ) {
				continue;
			}

			if( fluchtmode.equals("current") ) {
				battle.logme( aship.getString("name")+" fl&uuml;chtet\n" );
				efluchtlog.append(Battle.log_shiplink(aship)+" flieht\n");
			
				aship.put("action", aship.getInt("action") | Battle.BS_FLUCHT);
			}
			else {
				battle.logme( aship.getString("name")+" flieht n&auml;chste Runde\n" );
			
				aship.put("action", aship.getInt("action") | Battle.BS_FLUCHTNEXT);
			}
			
			db.update("UPDATE battles_ships SET action=",aship.getInt("action")," WHERE shipid=",aship.getInt("id"));
					
			int remove = 1;
			for( int j=0; j < ownShips.size(); j++ ) {
				SQLResultRow s = ownShips.get(j);
				if( (s.getString("docked").length() > 0) && (s.getString("docked").equals(""+aship.getInt("id")) || s.getString("docked").equals("l "+aship.getInt("id")) ) ) {
					s.put("action", s.getInt("action") | fluchtflag);
					db.update("UPDATE battles_ships SET action=",s.getInt("action")," WHERE shipid=",s.getInt("id"));
					
					remove++;
				}
			}
			
			if( fluchtmode.equals("current") ) {
				if( remove > 1 ) {
					battle.logme( (remove-1)+" an "+aship.getString("name")+" gedockte Schiffe fliehen\n" );
					efluchtlog.append((remove-1)+" an "+aship.getString("name")+" gedockte Schiffe fliehen\n");
				}
			}
			else {
				if( remove > 1 ) {
					battle.logme( (remove-1)+" an "+aship.getString("name")+" gedockte Schiffe fliehen n&auml;chste Runde\n" );
				}
			}
			shipcount += remove;
		}
		
		if( fluchtmode.equals("current") && (shipcount > 0) ) {	
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(efluchtlog.toString());
			battle.logenemy("]]></action>\n");
		}
		
		if( shipcount > 0 ) {
			battle.save(false);
		}
		
		return RESULT_OK;	
	}
}
