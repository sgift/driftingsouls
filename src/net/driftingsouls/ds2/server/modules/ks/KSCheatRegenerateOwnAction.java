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
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;
import java.util.HashMap;

/**
 * Cheat eigenes Schiff regenerieren.
 * @author Christopher Jung
 *
 */
public class KSCheatRegenerateOwnAction extends BasicKSAction {
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException
	{
		Result result = super.execute(t, battle);
		if (result != Result.OK)
		{
			return result;
		}

		Context context = ContextMap.getContext();

		if (!new ConfigService().getValue(WellKnownConfigValue.ENABLE_CHEATS))
		{
			context.addError("Cheats sind deaktiviert!");
			return Result.HALT;
		}

		BattleShip ownShip = battle.getOwnShip();

		ShipTypeData ownShipType = ownShip.getTypeData();
		ownShip.getShip().setCrew(ownShipType.getCrew());
		ownShip.getShip().setHull(ownShipType.getHull());
		ownShip.getShip().setEnergy(ownShipType.getEps());
		ownShip.getShip().setShields(ownShipType.getShields());
		ownShip.getShip().setEngine(100);
		ownShip.getShip().setWeapons(100);
		ownShip.getShip().setSensors(100);
		ownShip.getShip().setComm(100);
		ownShip.getShip().setHeat(0);
		ownShip.getShip().setWeaponHeat(new HashMap<>());

		ownShip.setHull(ownShip.getShip().getHull());
		ownShip.setShields(ownShip.getShip().getShields());
		ownShip.setEngine(100);
		ownShip.setWeapons(100);
		ownShip.setSensors(100);
		ownShip.setComm(100);
		ownShip.removeAllFlags();

		battle.logme("CHEAT: Gegnerisches Schiff regeneriert\n");
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), "CHEAT: [color=green]" + ownShip.getName() + "[/color] regeneriert"));

		ownShip.getShip().recalculateShipStatus();

		return Result.OK;
	}
}
