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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.ShipTypes;
import net.driftingsouls.ds2.server.ships.Ships;

/**
 * Entlaedt alle Batterien auf Schiffen der eigenen Seite
 * @author Christopher Jung
 *
 */
public class KSDischargeBatteriesAllAction extends BasicKSAction {
	/**
	 * Konstruktor
	 *
	 */
	public KSDischargeBatteriesAllAction() {
		this.requireAP(1);
	}
	
	/**
	 * Prueft, ob das Schiff seine Battieren entladen soll oder nicht
	 * @param ship Das Schiff
	 * @param shiptype Der Schiffstyp
	 * @return <code>true</code>, wenn das Schiff seine Batterien entladen soll
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
		StringBuilder ebattslog = new StringBuilder();
		
		List<SQLResultRow> ownShips = battle.getOwnShips();
		for( int i=0; i < ownShips.size(); i++ ) {
			SQLResultRow aship = ownShips.get(i);
			
			SQLResultRow ownShipType = ShipTypes.getShipType(aship);
			
			if( aship.getInt("e") >= ownShipType.getInt("eps") ) {
				continue;
			}
			
			if( !validateShipExt(aship, ownShipType) ) {
				continue;
			}
			
			Cargo mycargo = new Cargo( Cargo.Type.STRING, aship.getString("cargo") );
			if( !mycargo.hasResource( Resources.BATTERIEN ) ) {
				continue;
			}
			
			if( battle.getPoints(battle.getOwnSide()) < 3 ) {
				battle.logme("Nicht genug Aktionspunkte um weitere Batterien zu entladen");
				break;
			}
	
			int oldE = aship.getInt("e");
			long batterien = mycargo.getResourceCount( Resources.BATTERIEN );

			if( batterien > ownShipType.getInt("eps")-aship.getInt("e") ) {
				batterien = ownShipType.getInt("eps")-aship.getInt("e");
			}	

			aship.put("e", aship.getInt("e")+batterien);
		
			mycargo.substractResource( Resources.BATTERIEN, batterien );
			mycargo.addResource( Resources.LBATTERIEN, batterien );

			db.update("UPDATE ships SET e="+aship.getInt("e")+",battleAction=1,cargo='"+mycargo.save()+"' " +
					"WHERE id="+aship.getInt("e")+" AND cargo='"+mycargo.save(true)+"' AND e="+oldE);
			
			if( db.affectedRows() > 0 ) {
				aship.put("cargo", mycargo.save());
				aship.put("battleAction", true);
				
				battle.logme( aship.getString("name")+": "+batterien+" Reservebatterien entladen\n" );
				ebattslog.append(Battle.log_shiplink(aship)+": Reservebatterien entladen\n");
				
				battle.setPoints(battle.getOwnSide(), battle.getPoints(battle.getOwnSide())-3);
			
				aship.put("status", Ships.recalculateShipStatus(aship.getInt("id")));
				shipcount++;
			}
			else {
				battle.logme( aship.getString("name")+": Konnte Reservebatterien nicht entladen\n" );
				aship.put("e", oldE);
			}
		}

		if( shipcount > 0 ) {	
			battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+context.get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			battle.logenemy(ebattslog.toString());
			battle.logenemy("]]></action>\n");
			
			battle.save(false);
		}

		return RESULT_OK;
	}
}
