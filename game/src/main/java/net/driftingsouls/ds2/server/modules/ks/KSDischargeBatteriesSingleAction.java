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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;

/**
 * Entlaedt die Reservebatterien auf dem gerade ausgewaehlten Schiff.
 * @author Christopher Jung
 *
 */
public class KSDischargeBatteriesSingleAction extends BasicKSAction {
	/**
	 * Konstruktor.
	 *
	 */
	public KSDischargeBatteriesSingleAction() {
	}
	
	@Override
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		Cargo mycargo = ownShip.getCargo();
		if( mycargo.hasResource( Resources.BATTERIEN ) && (ownShip.getShip().getEnergy() < ownShipType.getEps()) ) {
			return Result.OK;
		}
		return Result.ERROR;	
	}

	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		if( this.validate(battle) != Result.OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return Result.ERROR;
		}

		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		Cargo mycargo = ownShip.getCargo();
		
		long batterien = mycargo.getResourceCount( Resources.BATTERIEN );

		if( batterien > ownShipType.getEps()-ownShip.getShip().getEnergy() ) {
			batterien = ownShipType.getEps()-ownShip.getShip().getEnergy();
		}	

		ownShip.getShip().setEnergy((int)(ownShip.getShip().getEnergy()+batterien));
		ownShip.getShip().setBattleAction(true);
	
		mycargo.substractResource( Resources.BATTERIEN, batterien );
		mycargo.addResource( Resources.LBATTERIEN, batterien );
		
		ownShip.getShip().setCargo(mycargo);

		battle.logme( ownShip.getName()+": "+batterien+" Reservebatterien entladen\n" );
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), Battle.log_shiplink(ownShip.getShip()) + ": Reservebatterien entladen"));
		
		ownShip.getShip().recalculateShipStatus();

		return Result.OK;
	}
}
