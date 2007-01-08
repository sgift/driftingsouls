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
 * Dockt alle Schiffe vom gerade ausgewaehlten Schiff ab
 * @author Christopher Jung
 *
 */
public class KSUndockAllAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSUndockAllAction() {
		this.requireAP(1);
	}
	
	@Override
	public int validate(Battle battle) {
		SQLResultRow ownShip = battle.getOwnShip();
		Database db = ContextMap.getContext().getDatabase();
		
		SQLResultRow dock = db.first("SELECT id FROM ships WHERE docked IN ('l ",ownShip.getInt("id"),"','",ownShip.getInt("id"),"')");
		if( !dock.isEmpty() ) {
			return RESULT_OK;
		}
		return RESULT_ERROR;	
	}

	@Override
	public int execute(Battle battle) {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Validation failed\n" );
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		SQLResultRow ownShip = battle.getOwnShip();
		
		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

		db.update("UPDATE ships SET battleAction=1 WHERE id=",ownShip.getInt("id"));

		db.update("UPDATE ships SET docked='',battleAction=1 WHERE docked IN ('l ",ownShip.getInt("id"),"','",ownShip.getInt("id"),"')");

		battle.logme(db.affectedRows()+" Schiffe wurden abgedockt");
		battle.logenemy(db.affectedRows()+" Schiffe wurden von der "+Battle.log_shiplink(ownShip)+" abgedockt\n");

		battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide()) - 1);

		battle.logenemy("]]></action>\n");

		battle.save(false);
		
		ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));
		
		return RESULT_OK;
	}
}
