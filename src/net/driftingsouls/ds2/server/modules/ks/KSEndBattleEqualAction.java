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

import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ermoeglicht das Beenden der Schlacht im Falle vom einer klaren militaerischen Uebermacht gegenueber dem Gegner.
 * @author Christopher Jung
 *
 */
public class KSEndBattleEqualAction extends BasicKSAction {
	@Override
	public Result validate(Battle battle) {
		/*
		//Check ob man nicht der Angreifer ist
		if( battle.getOwnSide() == 0 ) {
			return Result.ERROR;
		}
		*/
		int endTieModifier = new ConfigService().getValue(WellKnownConfigValue.END_TIE_MODIFIER);
		if((battle.getBattleValue(Side.ENEMY) == 0) || (battle.getBattleValue(Side.OWN) > (battle.getBattleValue(Side.ENEMY) * endTieModifier)))
		{
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
		
		List<BattleShip> shiplist = battle.getShips(Side.OWN);
		StringBuilder msg = new StringBuilder();
		for (BattleShip aship : new ArrayList<>(shiplist))
		{
			ShipTypeData ashiptype = aship.getShip().getTypeData();

			if (this.validate(battle) == Result.OK)
			{
				if (battle.getBattleValue(Side.OWN) - aship.getBattleValue() > 0)
				{
					if (!aship.hasFlag(BattleShipFlag.SECONDROW_BLOCKED) &&
						!aship.hasFlag(BattleShipFlag.SHOT) &&
						!aship.hasFlag(BattleShipFlag.SECONDROW) &&
						(aship.getEngine() > 0) &&
						!aship.hasFlag(BattleShipFlag.DESTROYED) &&
						!aship.getShip().isLanded() &&
						!aship.getShip().isDocked() &&
						!aship.hasFlag(BattleShipFlag.JOIN) &&
						!aship.hasFlag(BattleShipFlag.FLUCHT) &&
						((ashiptype.getMinCrew() == 0) || (aship.getCrew() >= ashiptype.getMinCrew() / 2d)))
					{
						battle.removeShip(aship, false);
						battle.logme(Battle.log_shiplink(aship.getShip()) + "ist durchgebrochen\n");
						msg.append(Battle.log_shiplink(aship.getShip())).append("ist durchgebrochen\n");
					}
				}
			}
			else
			{
				battle.log(new SchlachtLogAktion(battle.getOwnSide(), msg.toString()));
				return Result.OK;
			}
		}
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), msg.toString()));
		return Result.OK;
		
	}
}
