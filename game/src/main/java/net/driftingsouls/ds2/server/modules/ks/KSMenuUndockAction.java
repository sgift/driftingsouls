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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt das Menue fuer die verschiedenen Undockaktionen.
 *
 */
public class KSMenuUndockAction extends BasicKSMenuAction {
    @Override
    public Result validate(Battle battle) {
        BattleShip ownShip = battle.getOwnShip();
        org.hibernate.Session db = ContextMap.getContext().getDB();

        if(ownShip.getShip().getBaseShip() != null)
        {
            return Result.OK;
        }

        boolean dock = db.createQuery("from Ship where docked in (:docked,:landed)")
                .setString("landed", "l "+ownShip.getId())
                .setString("docked", Integer.toString(ownShip.getId()))
                .iterate().hasNext();

        if( dock ) {
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

        BattleShip ownShip = battle.getOwnShip();

        BattleShip enemyShip = battle.getEnemyShip();

        if(ownShip.getShip().getBaseShip() != null)
        {
            this.menuEntry(t, "Dieses Schiff abdocken.",
                    "ship", ownShip.getId(),
                    "attack", enemyShip.getId(),
                    "ksaction", "undock_single");
        }
        else
        {
            this.menuEntry(t, "Alle Schiffe abdocken",
                    "ship", ownShip.getId(),
                    "attack", enemyShip.getId(),
                    "ksaction", "undock_all"
            );

            Map<ShipClasses,Integer> undockclasslist = new HashMap<>();

            List<BattleShip> ownShips = battle.getOwnShips();
            for (BattleShip aship : ownShips)
            {
                if (aship.getShip().getBaseShip() == null || aship.getShip().getBaseShip().getId() != ownShip.getId())
                {
                    continue;
                }

                Common.safeIntInc(undockclasslist, aship.getTypeData().getShipClass());
            }

            for( Map.Entry<ShipClasses, Integer> entry: undockclasslist.entrySet() ) {
                ShipClasses classID = entry.getKey();
                int idlist = entry.getValue();

                if( idlist == 0 ) {
                    continue;
                }
                this.menuEntryAsk(t, "Alle "+classID.getPlural()+" starten",
                        new Object[] { 	"ship",			ownShip.getId(),
                                "attack",		enemyShip.getId(),
                                "ksaction",		"undock_class",
                                "undockclass",	classID.ordinal() },
                        "Wollen sie wirklich alle '"+classID.getSingular()+"' starten?" );
            }

        }

        this.menuEntry(t, "zur&uuml;ck",
                "ship",		ownShip.getId(),
                "attack",	enemyShip.getId() );

        return Result.OK;
    }
}
