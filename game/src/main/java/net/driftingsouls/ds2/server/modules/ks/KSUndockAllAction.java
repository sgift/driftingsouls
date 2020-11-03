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
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dockt alle Schiffe vom gerade ausgewaehlten Schiff ab.
 * @author Christopher Jung
 *
 */
@Component
public class KSUndockAllAction extends BasicKSAction {
	@PersistenceContext
	private EntityManager em;

	private final ShipService shipService;
	private final ShipActionService shipActionService;

	public KSUndockAllAction(BattleService battleService, ShipService shipService, ShipActionService shipActionService) {
		super(battleService, null);
		this.shipService = shipService;
		this.shipActionService = shipActionService;
	}

    /**
     * Prueft, ob das Schiff gestartet werden soll oder nicht.
     * @param ship Das Schiff
     * @param shiptype Der Schiffstyp
     * @return <code>true</code>, wenn das Schiff gestartet werden soll
     */
    protected boolean validateShipExt( BattleShip ship, ShipTypeData shiptype ) {
        // Extension Point
        return true;
    }

	@Override
	public Result validate(Battle battle) {
		BattleShip ownShip = battle.getOwnShip();

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

		if( this.validate(battle) != Result.OK ) {
			getBattleService().logme(battle,  "Validation failed\n" );
			return Result.ERROR;
		}
		BattleShip ownShip = battle.getOwnShip();

		ownShip.getShip().setBattleAction(true);

        List<Ship> startList = new ArrayList<>();
        List<Ship> undockList = new ArrayList<>();

        for(BattleShip aship : battle.getOwnShips())
        {
            if(!validateShipExt(aship, aship.getTypeData()))
            {
                continue;
            }

            if(shipService.getBaseShip(aship.getShip()) != null && shipService.getBaseShip(aship.getShip()).getId() == ownShip.getShip().getId())
            {
                if(aship.getShip().isLanded())
                {
                    startList.add(aship.getShip());
                }
                else
                {
                    undockList.add(aship.getShip());
                }
            }
        }

        Ship[] startArray = new Ship[startList.size()];
        Ship[] undockArray = new Ship[undockList.size()];
        startList.toArray(startArray);
        undockList.toArray(undockArray);

        shipService.start(ownShip.getShip(), startArray);
        shipService.undock(ownShip.getShip(), undockArray);

		List<BattleShip> ownShips = battle.getOwnShips();
		for (BattleShip s : ownShips)
		{
			if (s.hasFlag(BattleShipFlag.SECONDROW) && !s.getTypeData().hasFlag(ShipTypeFlag.SECONDROW))
			{
				s.removeFlag(BattleShipFlag.SECONDROW);
				s.addFlag(BattleShipFlag.SECONDROW_BLOCKED);
			}
		}

		getBattleService().logme(battle, (startList.size()+undockList.size())+" Schiffe wurden abgedockt");
		getBattleService().log(battle, new SchlachtLogAktion(battle.getOwnSide(), (startList.size()+undockList.size())+" Schiffe wurden von der "+Battle.log_shiplink(ownShip.getShip())+" abgedockt"));

		shipActionService.recalculateShipStatus(ownShip.getShip());

		return Result.OK;
	}
}
