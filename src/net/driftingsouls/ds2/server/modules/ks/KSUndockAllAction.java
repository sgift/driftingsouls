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
import java.util.Iterator;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;

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
	}
	
	@Override
	public int validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		org.hibernate.Session db = ContextMap.getContext().getDB();
		
		boolean dock = db.createQuery("from Ship where docked in (?,?)")
			.setString(0, "l "+ownShip.getId())
			.setString(1, Integer.toString(ownShip.getId()))
			.iterate().hasNext();
		
		if( dock ) {
			return RESULT_OK;
		}
		return RESULT_ERROR;	
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme( "Validation failed\n" );
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		BattleShip ownShip = battle.getOwnShip();
		
		battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

		ownShip.getShip().setBattleAction(true);
		
		int counter = db.createQuery("update Ship set battleaction=? where docked in (?,?)")
			.setParameter(0, true)
			.setString(1, "l " + ownShip.getId())
			.setString(2, "" + ownShip.getId())
			.executeUpdate();
		ownShip.getShip().start(new Ship[0]);
		ownShip.getShip().undock(new Ship[0]);

		battle.logme(counter+" Schiffe wurden abgedockt");
		battle.logenemy(counter+" Schiffe wurden von der "+Battle.log_shiplink(ownShip.getShip())+" abgedockt\n");

		battle.logenemy("]]></action>\n");

		battle.resetInactivity();
		
		ownShip.getShip().recalculateShipStatus();
		
		return RESULT_OK;
	}
}
