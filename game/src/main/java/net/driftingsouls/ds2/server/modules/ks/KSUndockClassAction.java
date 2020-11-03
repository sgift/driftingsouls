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

import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.ShipActionService;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Dockt alle Schiffe vom gerade ausgewaehlten Schiff ab.
 * @author Christopher Jung
 *
 */
@Component
@Lazy
public class KSUndockClassAction extends KSUndockAllAction {
    private final ShipClasses undockclass;

    /**
     * Konstruktor.
     *
     */
    public KSUndockClassAction(BattleService battleService, ShipService shipService, ShipActionService shipActionService) {
        super(battleService, shipService, shipActionService);
        this.undockclass  = ShipClasses.values()[ContextMap.getContext().getRequest().getParameterInt("undockclass")];
    }

    /**
     * Prueft, ob das Schiff gestartet werden soll oder nicht.
     * @param ship Das Schiff
     * @param shiptype Der Schiffstyp
     * @return <code>true</code>, wenn das Schiff gestartet werden soll
     */
    protected boolean validateShipExt( BattleShip ship, ShipTypeData shiptype ) {
        return shiptype.getShipClass() == this.undockclass;
    }
}
