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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Laedt die Schilde des gerade ausgewaehlten Schiffes wieder auf
 * @author Christopher Jung
 *
 */
public class KSRegenerateShieldsSingleAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSRegenerateShieldsSingleAction() {
		this.requireAP(1);
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
		SQLResultRow ownShipType = Ships.getShipType(ownShip);
		
		if( ownShip.getInt("id") < 1 ) {
			battle.logme( "Keine Energie um die Schilde zu laden\n" );
			return RESULT_ERROR;
		}
		
		if( ownShipType.getInt("shields") < 1 ) {
			battle.logme( "Das Schiff besitzt keine Schilde\n" );
			return RESULT_ERROR;
		}
		
		if( ownShip.getInt("shields") >= ownShipType.getInt("shields") ) {
			battle.logme( "Die Schilde sind bereits vollst&auml;ndig aufgeladen\n" );
			return RESULT_ERROR;
		}

		int shieldfactor = 10;
		if( ownShipType.getInt("shields") > 1000 ) {
			shieldfactor = 100;
		}

		int load = 0;
		if( ownShip.getInt("e") < Math.ceil((ownShipType.getInt("shields")-ownShip.getInt("shields"))/(double)shieldfactor) ) {
			battle.logme( "Nicht genug Energie um die Schilde vollst&auml;ndig aufzuladen\n\n" );
			load = ownShip.getInt("e");
		}
		else {
			load = (int)Math.ceil((ownShipType.getInt("shields")-ownShip.getInt("shields"))/(double)shieldfactor);
		}

		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

		ownShip.put("e", ownShip.getInt("e") - load);
		ownShip.put("shields", ownShip.getInt("shields") + load*shieldfactor);
		if( ownShip.getInt("shields") > ownShipType.getInt("shields") ) {
			ownShip.put("shields", ownShipType.getInt("shields"));
		}

		battle.logme( "Schilde nun bei "+ownShip.getInt("shields")+"/"+ownShipType.getInt("shields")+"\n" );
		battle.logenemy("Die "+Battle.log_shiplink(ownShip)+" hat ihre Schilde aufgeladen\n");

		db.update("UPDATE ships SET e=",ownShip.getInt("e"),", shields=",ownShip.getInt("shields"),",battleAction=1 WHERE id>0 AND id=",ownShip.getInt("id"));

		int curShields = db.first("SELECT shields FROM battles_ships WHERE shipid=",ownShip.getInt("id")).getInt("shields");
		curShields += load*shieldfactor;
		if( curShields > ownShipType.getInt("shields") ) {
			curShields = ownShipType.getInt("shields");
		}
		db.query("UPDATE battles_ships SET shields=",curShields," WHERE shipid=",ownShip.getInt("id"));

		battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide())-1);

		battle.logenemy("]]></action>\n");

		battle.save(false);
		
		ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));
			
		return RESULT_OK;
	}
}
