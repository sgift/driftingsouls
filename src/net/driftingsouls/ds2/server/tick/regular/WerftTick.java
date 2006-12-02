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
package net.driftingsouls.ds2.server.tick.regular;

import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.Items;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.Ships;
import net.driftingsouls.ds2.server.tick.TickController;
import net.driftingsouls.ds2.server.werften.BaseWerft;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;

/**
 * Berechnung des Ticks fuer Werften
 * @author Christopher Jung
 *
 */
public class WerftTick extends TickController {

	@Override
	protected void prepare() {	
		// EMPTY
	}

	@Override
	protected void tick() {
		Database db = getDatabase();
		
		SQLQuery werftRow = db.query("SELECT * FROM werften WHERE building!=0 ORDER BY id");
		while( werftRow.next() ) {		
			int id = werftRow.getInt("id");
			WerftObject werftd = null;
			
			// Werft auf einer Basis
			if( werftRow.getInt("col") > 0 ) {
				SQLResultRow base = db.first("SELECT * FROM bases WHERE id=",werftRow.getInt("col"));
				
				User owner = getContext().createUserObject(base.getInt("owner"));
				if( (owner.getVacationCount() > 0) && (owner.getWait4VacationCount() == 0) ) {
					this.log("xxx Ignoriere planetare Werft $id (Basis "+werftRow.getInt("col")+") [VAC]");
					continue;
				}
				
				this.log("+++ Planetare Werft "+id+" (Basis "+werftRow.getInt("col")+"):");
				werftd = new BaseWerft(werftRow.getRow(),"pwerft",base.getInt("system"),base.getInt("owner"),base.getInt("id"),-1);
			}
			// Werft auf einem Schiff
			else if( werftRow.getInt("shipid") > 0 ) {
				SQLResultRow ship = db.first("SELECT * FROM ships WHERE id>0 AND id=",werftRow.getInt("shipid"));
				
				User owner = getContext().createUserObject(ship.getInt("owner"));
				if( (owner.getVacationCount() > 0) && (owner.getWait4VacationCount() == 0) ) {
					this.log("xxx  Ignoriere Werft "+id+" (Schiff "+werftRow.getInt("shipid")+") [VAC]");
					continue;
				}
				this.log("+++ Werft "+id+" (Schiff "+werftRow.getInt("shipid")+"):");
				
				SQLResultRow shiptype = Ships.getShipType(ship);
				
				werftd = new ShipWerft(werftRow.getRow(),shiptype.getString("werft"),ship.getInt("system"),ship.getInt("owner"),ship.getInt("id"));
				werftd.setOneWayFlag(shiptype.getInt("ow_werft"));
			}
			// Werft auf einem spawnbaren Schiff
			else if( werftRow.getInt("shipid") < 0 ) {
				continue;	
			}
			else {
				this.log("+++ Unbekannte Werft $id:");
				continue;
			}
			
			
			if( werftd.isBuilding() ){	
				SQLResultRow shipd = werftd.getBuildShipType();
				
				this.log("\tAktueller Auftrag: "+shipd.getInt("id")+"; dauer: "+werftd.getRemainingTime());
				
				if( werftd.getRequiredItem() > -1 ) {
					this.log("\tItem benoetigt: "+Items.get().item(werftd.getRequiredItem()).getName()+" ("+werftd.getRequiredItem()+")");
				}
				if( werftd.isBuildContPossible() ) {
					werftd.decRemainingTime();
					this.log("\tVoraussetzungen erfuellt - bau geht weiter");
				}
				
				if( werftd.getRemainingTime() <= 0 ) {
					this.log("\tSchiff "+shipd.getInt("id")+" gebaut");

					int shipid = werftd.finishBuildProcess();
					this.slog(werftd.MESSAGE.getMessage());
					
					if( shipid > 0 ) {
						// MSG
						String msg = "Auf "+werftd.getName()+" wurde eine "+shipd.getString("nickname")+" gebaut. Sie steht bei "+werftd.getSystem()+" : "+werftd.getX()+"/"+werftd.getY()+".";
					
						PM.send(getContext(), -1, werftd.getOwner(), "Schiff gebaut", msg);
					}
				}
			}
		}
		werftRow.free();
	}

}
