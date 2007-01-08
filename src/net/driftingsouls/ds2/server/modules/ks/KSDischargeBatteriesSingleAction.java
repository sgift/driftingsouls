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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Entlaedt die Reservebatterien auf dem gerade ausgewaehlten Schiff
 * @author Christopher Jung
 *
 */
public class KSDischargeBatteriesSingleAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSDischargeBatteriesSingleAction() {
		this.requireAP(3);
	}
	
	@Override
	public int validate(Battle battle) {
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow ownShipType = Ships.getShipType(ownShip);
		
		Cargo mycargo = new Cargo( Cargo.Type.STRING, ownShip.getString("cargo") );
		if( mycargo.hasResource( Resources.BATTERIEN ) && (ownShip.getInt("e") < ownShipType.getInt("eps")) ) {
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
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return RESULT_ERROR;
		}
		
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		SQLResultRow ownShip = battle.getOwnShip();
		SQLResultRow ownShipType = Ships.getShipType(ownShip);
		
		Cargo mycargo = new Cargo( Cargo.Type.STRING, ownShip.getString("cargo") );
		
		
		int oldE = ownShip.getInt("e");
		long batterien = mycargo.getResourceCount( Resources.BATTERIEN );

		if( batterien > ownShipType.getInt("eps")-ownShip.getInt("e") ) {
			batterien = ownShipType.getInt("eps")-ownShip.getInt("e");
		}	

		ownShip.put("e", ownShip.getInt("e")+batterien);
	
		mycargo.substractResource( Resources.BATTERIEN, batterien );
		mycargo.addResource( Resources.LBATTERIEN, batterien );

		db.update("UPDATE ships SET e=",ownShip.getInt("e"),",battleAction=1,cargo='",mycargo.save(),"' WHERE id=",ownShip.getInt("e")," cargo='",mycargo.save(true),"' AND e=",oldE);
		
		if( db.affectedRows() > 0 ) {
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");

			ownShip.put("cargo", mycargo.save());
			ownShip.put("battleAction", true);
			
			battle.logme( ownShip.getString("name")+": "+batterien+" Reservebatterien entladen\n" );
			battle.logenemy(Battle.log_shiplink(ownShip)+": Reservebatterien entladen\n");
			
			battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide())-3);
		
			ownShip.put("status", Ships.recalculateShipStatus(ownShip.getInt("id")));
			
			battle.logenemy("]]></action>\n");
			battle.save(false);
		}
		else {
			battle.logme( ownShip.getString("name")+": Konnte Reservebatterien nicht entladen\n" );
			ownShip.put("e", oldE);
		}
		
		return RESULT_OK;
	}
}
