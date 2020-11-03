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
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.authentication.JavaSession;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt das Menue fuer die verschiedenen Undockaktionen.
 *
 */
@Component
public class KSMenuUndockAction extends BasicKSMenuAction {
    @PersistenceContext
    private EntityManager em;

    private final ShipService shipService;

    public KSMenuUndockAction(BattleService battleService, JavaSession javaSession, ShipService shipService) {
        super(battleService, (User)javaSession.getUser());
        this.shipService = shipService;
    }

    @Override
    public Result validate(Battle battle) {
        BattleShip ownShip = battle.getOwnShip();

        if(shipService.getBaseShip(ownShip.getShip()) != null)
        {
            return Result.OK;
        }

        boolean dock = !em.createQuery("from Ship where docked in (:docked,:landed)", Ship.class)
                .setParameter("landed", "l "+ownShip.getId())
                .setParameter("docked", Integer.toString(ownShip.getId()))
                .setMaxResults(1)
                .getResultList().isEmpty();

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

        if(shipService.getBaseShip(ownShip.getShip()) != null)
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

            Map<ShipClasses,Integer> undockclasslist = new EnumMap<>(ShipClasses.class);

            List<BattleShip> ownShips = battle.getOwnShips();
            for (BattleShip aship : ownShips)
            {
                if (shipService.getBaseShip(aship.getShip()) == null || shipService.getBaseShip(aship.getShip()).getId() != ownShip.getId())
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
