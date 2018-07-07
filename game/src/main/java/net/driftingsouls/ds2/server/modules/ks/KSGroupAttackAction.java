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
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Berechnet das Waffenfeuer für eine ganze Gruppe an Schiffen im KS.
 *
 */
public class KSGroupAttackAction extends BasicKSAction {

    /**
     * Konstruktor.
     *
     */
    public KSGroupAttackAction()
    {
        this(null);
    }

    public KSGroupAttackAction(User user)
    {
        super(user);

        this.requireOwnShipReady(true);

    }

    @Override
    public Result execute(TemplateEngine t, Battle battle) throws IOException
    {
        int typeid = battle.getOwnShip().getTypeData().getTypeId();
        int enemytypeid = battle.getEnemyShip().getTypeData().getTypeId();

        List<BattleShip> togoShips = battle.getOwnShips().stream().filter(bship -> bship.getTypeData().getTypeId() == typeid).collect(Collectors.toList());
        battle.logme(togoShips.size() + " Schiffe zu feuern.\n");

        for(BattleShip aship : togoShips)
        {
            BattleShip enemyShip = battle.getEnemyShip();
            battle.setFiringShip(aship.getShip());
            battle.logme("Schiff: "+Battle.log_shiplink(aship.getShip())+"\n");

            if(enemyShip.getTypeData().getTypeId() != enemytypeid)
            {
                break;
            }
            KSAttackAction act = new KSAttackAction();
            act.setController(getController());
            Result result = act.execute(t, battle);

            if(result == Result.HALT)
            {
                return result;
            }
        }
        
        return Result.OK;
    }

    private static final Log log = LogFactory.getLog(KSGroupAttackAction.class);
}
