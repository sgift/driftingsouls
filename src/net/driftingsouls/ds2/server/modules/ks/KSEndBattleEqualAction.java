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

import java.io.IOException;
import java.util.List;

import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.Side;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.ContextMap;

import org.hibernate.Session;

/**
 * Ermoeglicht das Beenden der Schlacht im Falle vom einer klaren militaerischen Uebermacht gegenueber dem Gegner.
 * @author Christopher Jung
 *
 */
public class KSEndBattleEqualAction extends BasicKSAction {
	@Override
	public int validate(Battle battle) {
		/*
		//Check ob man nicht der Angreifer ist
		if( battle.getOwnSide() == 0 ) {
			return RESULT_ERROR;
		}
		*/
		ConfigValue endTieModifier = (ConfigValue)getDB().get(ConfigValue.class, "endtiemodifier");
		if((battle.getBattleValue(Side.ENEMY) == 0) || (battle.getBattleValue(Side.OWN) > (battle.getBattleValue(Side.ENEMY) * Integer.valueOf(endTieModifier.getValue())))) 
		{
			return RESULT_OK;
		}

		return RESULT_ERROR;
	}

	@Override
	public int execute(Battle battle) throws IOException {
		int result = super.execute(battle);
		if( result != RESULT_OK ) {
			return result;
		}
		
		if( this.validate(battle) != RESULT_OK ) {
			battle.logme("Die Aktion kann nicht ausgef&uuml;hrt werden");
			return RESULT_ERROR;
		}
		
		List<BattleShip> shiplist = battle.getShips(Side.OWN);
		for( int key=0; key < shiplist.size(); key++ ) {
			BattleShip aship = shiplist.get(key);
			
			if( this.validate(battle) == RESULT_OK )
			{
				if(battle.getBattleValue(Side.OWN) - aship.getBattleValue() > 0)
				{	
					if (((aship.getAction() & Battle.BS_SECONDROW_BLOCKED) == 0) && ((aship.getAction() & Battle.BS_SHOT) == 0) && ((aship.getAction() & Battle.BS_SECONDROW) == 0) && (aship.getEngine() > 0 ) && ((aship.getAction() & Battle.BS_DESTROYED) == 0 ) && !aship.getShip().isLanded() && !aship.getShip().isDocked() && ((aship.getAction() & Battle.BS_JOIN) == 0) && ((aship.getAction() & Battle.BS_FLUCHT) == 0) )  
					{
						battle.removeShip(aship, false);
						battle.logme(Battle.log_shiplink(aship.getShip()) + "ist durchgebrochen\n");
						battle.logenemy(Battle.log_shiplink(aship.getShip()) + "ist durchgebrochen\n");
					}
				}
			}
			else
			{
				return RESULT_OK;
			}
		}
		return RESULT_OK;
		
	}
	
	private Session getDB()
	{
		return ContextMap.getContext().getDB();
	}
}
