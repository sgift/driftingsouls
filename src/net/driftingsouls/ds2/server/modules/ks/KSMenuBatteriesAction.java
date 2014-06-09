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
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zeigt das Menue fuer Batterieentladeaktionen an.
 * @author Christopher Jung
 *
 */
public class KSMenuBatteriesAction extends BasicKSMenuAction {
	@Override
	public Result validate(Battle battle) {
		int battships = 0;
		
		List<BattleShip> ownShips = battle.getOwnShips();
		for (BattleShip aship : ownShips)
		{
			ShipTypeData ashiptype = aship.getTypeData();
			if (aship.getShip().getEnergy() >= ashiptype.getEps())
			{
				continue;
			}

			Cargo mycargo = aship.getCargo();
			if (mycargo.hasResource(Resources.BATTERIEN))
			{
				battships++;
				break;
			}
		}

		//Schilde aufladen
		if(	battships > 0 ) {
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
	
		if( this.isPossible(battle, new KSDischargeBatteriesSingleAction() ) == Result.OK ) {
			this.menuEntry(t, "Batterien entladen",
								"ship",		ownShip.getId(),
								"attack",	enemyShip.getId(),
								"ksaction",	"batterien_single" );
		}
				
		int battsidlist = 0;
		Map<ShipClasses,Integer> battsclasslist = new HashMap<>();
				
		List<BattleShip> ownShips = battle.getOwnShips();
		for (BattleShip aship : ownShips)
		{
			ShipTypeData ashiptype = aship.getTypeData();
			if (aship.getShip().getEnergy() >= ashiptype.getEps())
			{
				continue;
			}

			Cargo mycargo = aship.getCargo();
			if (mycargo.hasResource(Resources.BATTERIEN))
			{
				battsidlist++;
				Common.safeIntInc(battsclasslist, ashiptype.getShipClass());
			}
		}
								
		if( battsidlist > 0 ) {
			this.menuEntryAsk(t, "Alle Batterien entladen",
								new Object[] {	"ship",		ownShip.getId(),
												"attack",	enemyShip.getId(),
												"ksaction",	"batterien_all" },
								"Wollen sie wirklich bei allen Schiffen die Batterien entladen?" );
		}
		
		for( Map.Entry<ShipClasses, Integer> entry: battsclasslist.entrySet() ) {
			ShipClasses classID = entry.getKey();
			int idlist = entry.getValue();
			
			if( idlist == 0 ) {
				continue;
			} 
			this.menuEntryAsk(t, "Bei allen "+classID.getPlural()+"n die Batterien entladen",
								new Object[] { 	"ship",			ownShip.getId(),
												"attack",		enemyShip.getId(),
												"ksaction",		"batterien_class",
												"battsclass",	classID.ordinal() },
								"Wollen sie wirklich bei allen Schiffen der Klasse '"+classID.getSingular()+"' die Batterien entladen?" );
		}
				
		this.menuEntry(t, "zur&uuml;ck",
				"ship",		ownShip.getId(),
				"attack",	enemyShip.getId(),
				"ksaction",	"other" );
												
		return Result.OK;
	}
}
