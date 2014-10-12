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
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;

/**
 * Laedt die Schilde des gerade ausgewaehlten Schiffes wieder auf.
 * @author Christopher Jung
 *
 */
public class KSRegenerateShieldsSingleAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSRegenerateShieldsSingleAction() {
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		if( ownShip.getShip().getEnergy() < 1 ) {
			battle.logme( "Keine Energie um die Schilde zu laden\n" );
			return Result.ERROR;
		}
		
		if( ownShipType.getShields() < 1 ) {
			battle.logme( "Das Schiff besitzt keine Schilde\n" );
			return Result.ERROR;
		}
		
		if( ownShip.getShip().getShields() >= ownShipType.getShields() ) {
			battle.logme( "Die Schilde sind bereits vollst&auml;ndig aufgeladen\n" );
			return Result.ERROR;
		}

		int shieldfactor = 10;
		if( ownShipType.getShields() > 1000 ) {
			shieldfactor = 100;
		}

		int load = (int)Math.ceil((ownShipType.getShields()-ownShip.getShip().getShields())/(double)shieldfactor);
		if( ownShip.getShip().getEnergy() < load) {
			battle.logme( "Nicht genug Energie um die Schilde vollst&auml;ndig aufzuladen\n\n" );
			load = ownShip.getShip().getEnergy();
		}

		ownShip.getShip().setEnergy(ownShip.getShip().getEnergy() - load);
		ownShip.getShip().setShields(ownShip.getShip().getShields() + load*shieldfactor);
		if( ownShip.getShip().getShields() > ownShipType.getShields() ) {
			ownShip.getShip().setShields(ownShipType.getShields());
		}

		battle.logme( "Schilde nun bei "+ownShip.getShip().getShields()+"/"+ownShipType.getShields()+"\n" );
		battle.log(new SchlachtLogAktion(battle.getOwnSide(),"Die "+Battle.log_shiplink(ownShip.getShip())+" hat ihre Schilde aufgeladen"));

		ownShip.getShip().setBattleAction(true);

		int curShields = ownShip.getShields();
		curShields += load*shieldfactor;
		if( curShields > ownShipType.getShields() ) {
			curShields = ownShipType.getShields();
		}
		ownShip.setShields(curShields);

		ownShip.getShip().recalculateShipStatus();
			
		return Result.OK;
	}
}
