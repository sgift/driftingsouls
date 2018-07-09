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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt das Menue fuer die verschiedenen Fluchtaktionen.
 * @author Christopher Jung
 *
 */
public class KSMenuFluchtAction extends BasicKSMenuAction {
	@Override
	public Result execute(TemplateEngine t, Battle battle) throws IOException {
		Result result = super.execute(t, battle);
		if( result != Result.OK ) {
			return result;
		}
		
		Context context = ContextMap.getContext();
		
		BattleShip ownShip = battle.getOwnShip();
		ShipTypeData ownShipType = ownShip.getTypeData();
		
		BattleShip enemyShip = battle.getEnemyShip();
		
		String fluchtmode = context.getRequest().getParameterString("fluchtmode");
		
		if(!fluchtmode.equals("next")) 
		{
			fluchtmode = "next";	
		}
		
		Map<String,String> fluchtmodes = new HashMap<>();
		fluchtmodes.put("next", "N&auml;chste Runde");
							
		Map<String,String> nextfluchtmode = new HashMap<>();
		nextfluchtmode.put("next", "current");
							  
		this.menuEntry(t, "<span style=\"font-size:3px\">&nbsp;<br /></span>Fluchtzeitpunkt: "+fluchtmodes.get(fluchtmode)+"<br />\n"+
						"<span style=\"font-size:12px\">&lt; Klicken um Fluchtzeitpunkt zu &auml;ndern &gt;</span><span style=\"font-size:4px\"><br />&nbsp;</span>",
						"ship",		ownShip.getId(),
						"attack",	enemyShip.getId(),
						"ksaction",	"flucht",
						"fluchtmode",	nextfluchtmode.get(fluchtmode) );

        boolean gotone = false;
        if( ownShipType.hasFlag(ShipTypeFlag.DROHNE) ) {
            List<BattleShip> ownShips = battle.getOwnShips();
            for (BattleShip aship : ownShips)
            {
                ShipTypeData ashiptype = aship.getTypeData();
                if (ashiptype.hasFlag(ShipTypeFlag.DROHNEN_CONTROLLER))
                {
                    gotone = true;
                    break;
                }
            }
        }
        else {
            gotone = true;
        }

        if( !ownShip.hasFlag(BattleShipFlag.JOIN) && !ownShip.hasFlag(BattleShipFlag.DESTROYED) &&
            !ownShip.hasFlag(BattleShipFlag.FLUCHT) && !ownShip.getShip().isLanded() && !ownShip.getShip().isDocked() &&
            (ownShip.getShip().getEngine() > 0) &&
            ( !ownShip.getShip().isBattleAction() || fluchtmode.equals("next") ) &&
            gotone && ( !ownShip.hasFlag(BattleShipFlag.FLUCHTNEXT) || fluchtmode.equals("current")) ) {

            if( ownShip.getEngine() > 0 ) {
                this.menuEntry(t, "Flucht",
                        "ship",		ownShip.getId(),
                        "attack",	enemyShip.getId(),
                        "ksaction",	"flucht_single",
                        "fluchtmode",	fluchtmode );
            }
        }

        int fluchtidlist = 0;
        Map<ShipClasses,Integer> fluchtclasslist = new HashMap<>();

        List<BattleShip> ownShips = battle.getOwnShips();
        for (BattleShip aship : ownShips)
        {
            ShipTypeData ashiptype = aship.getTypeData();

            if (!aship.hasFlag(BattleShipFlag.FLUCHT) && !aship.hasFlag(BattleShipFlag.DESTROYED) &&
                    !aship.hasFlag(BattleShipFlag.FLUCHT) && !ownShip.getShip().isLanded() && !ownShip.getShip().isDocked() && (aship.getShip().getEngine() > 0) &&
                    !aship.getShip().isBattleAction() && gotone)
            {

                fluchtidlist++;
                Common.safeIntInc(fluchtclasslist, ashiptype.getShipClass());
            }
        }

        if( fluchtidlist > 0 ) {
            this.menuEntryAsk(t, "Alle Fl&uuml;chten",
                    new Object[] {
                        "ship",		ownShip.getId(),
                        "attack",	enemyShip.getId(),
                        "ksaction",	"flucht_all",
                        "fluchtmode",	fluchtmode  },
                    "Wollen sie wirklich mit allen Schiffen fl&uuml;chten?" );
        }

        for( Map.Entry<ShipClasses, Integer> entry: fluchtclasslist.entrySet()) {
            ShipClasses classID = entry.getKey();
            int idlist = entry.getValue();
            if( idlist == 0 ) {
                continue;
            }
            this.menuEntryAsk(t, "Alle "+classID.getPlural()+" fl&uuml;chten lassen",
                    new Object[] {
                        "ship",		ownShip.getId(),
                        "attack",	enemyShip.getId(),
                        "ksaction",	"flucht_class",
                        "fluchtclass",	classID.ordinal(),
                        "fluchtmode",	fluchtmode },
                    "Wollen sie wirklich mit allen Schiffen der Klasse '"+classID.getSingular()+"' fl&uuml;chten?" );
        }
				
		this.menuEntry(t, "zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId() );
												
		return Result.OK;
	}
}
