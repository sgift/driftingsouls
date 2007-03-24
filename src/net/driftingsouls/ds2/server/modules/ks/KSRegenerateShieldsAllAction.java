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
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Laedt die Schilde aller eigener Schiffe auf
 * @author Christopher Jung
 *
 */
public class KSRegenerateShieldsAllAction extends BasicKSAction {
	/**
	 * Prueft, ob das Schiff seine Schilde aufladen soll oder nicht
	 * @param ship Das Schiff
	 * @param shiptype Der Schiffstyp
	 * @return <code>true</code>, wenn das Schiff seine Schilde aufladen soll
	 */
	protected boolean validateShipExt( SQLResultRow ship, SQLResultRow shiptype ) {
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
		
		int shipcount = 0;
		StringBuilder eshieldlog = new StringBuilder();
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow ownShip = ownShips.get(i);
						
			if( ownShip.getInt("e") < 1 ) {
				continue;
			}
			
			SQLResultRow ownShipType = ShipTypes.getShipType(ownShip);
			
			if( ownShipType.getInt("shields") < 1 ) {
				continue;
			}
			
			if( ownShip.getInt("shields") >= ownShipType.getInt("shields") ) {
				continue;
			}
			
			if( !this.validateShipExt(ownShip, ownShipType) ) {
				continue;
			}
			
			if( battle.getPoints(battle.getOwnSide()) < 1 ) {
				battle.logme("Nicht genug Aktionspunkte um weitere Schilde zu aufzuladen");
				break;
			}
	
			int shieldfactor = 10;
			if( ownShipType.getInt("shields") > 1000 ) {
				shieldfactor = 100;
			}
	
			int load = 0;
			if( ownShip.getInt("e") < Math.ceil((ownShipType.getInt("shields")-ownShip.getInt("shields"))/(double)shieldfactor) ) {
				load = ownShip.getInt("e");
			}
			else {
				load = (int)Math.ceil((ownShipType.getInt("shields")-ownShip.getInt("shields"))/(double)shieldfactor);
			}

			ownShip.put("e", ownShip.getInt("e") - load);
			ownShip.put("shields", ownShip.getInt("shields") + load*shieldfactor);
			if( ownShip.getInt("shields") > ownShipType.getInt("shields") ) {
				ownShip.put("shields", ownShipType.getInt("shields"));
			}
	
			battle.logme( ownShip.getString("name")+": Schilde bei "+ownShip.getInt("shields")+"/"+ownShipType.getInt("shields")+"\n" );
			eshieldlog.append(Battle.log_shiplink(ownShip)+": Schilde aufgeladen\n");
	
			db.update("UPDATE ships SET e=",ownShip.getInt("e"),", shields=",ownShip.getInt("shields"),",battleAction=1 WHERE id>0 AND id=",ownShip.getInt("id"));
	
			int curShields = db.first("SELECT shields FROM battles_ships WHERE shipid=",ownShip.getInt("id")).getInt("shields");
			curShields += load*shieldfactor;
			if( curShields > ownShipType.getInt("shields") ) {
				curShields = ownShipType.getInt("shields");
			}
			db.update("UPDATE battles_ships SET shields=",curShields," WHERE shipid=",ownShip.getInt("id"));
	
			battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide())-1);
			
			ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));

			shipcount++;
		}

		if( shipcount > 0 ) {	
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(eshieldlog.toString());
			battle.logenemy("]]></action>\n");
			
			battle.save(false);
		}
	
		return RESULT_OK;
	}
}
