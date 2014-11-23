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

import java.io.IOException;

/**
 * Dockt das Schiff von seinem Mutterschiff ab.
 *
 */
public class KSUndockAction extends BasicKSAction {
    /**
     * Konstruktor.
     *
     */
    public KSUndockAction() {
    }

    @Override
    public Result validate(Battle battle) {
        BattleShip ownShip = battle.getOwnShip();

        if(ownShip.getShip().getBaseShip() != null)
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
            battle.logme( "Validation failed\n" );
            return Result.ERROR;
        }

        BattleShip ownShip = battle.getOwnShip();

        ownShip.getShip().setBattleAction(true);

        if(ownShip.getShip().isLanded())
        {
            ownShip.getShip().getBaseShip().start(ownShip.getShip());
        }
        else
        {
            ownShip.getShip().getBaseShip().undock(ownShip.getShip());
        }

        battle.logme("Die "+Battle.log_shiplink(ownShip.getShip())+" wurde abgedockt");
		battle.log(new SchlachtLogAktion(battle.getOwnSide(), "Die "+Battle.log_shiplink(ownShip.getShip())+" wurde abgedockt"));

        ownShip.getShip().recalculateShipStatus();

        return Result.OK;
    }
}
