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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.HibernateFacade;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Fuehrt den Tick fuer Schlachten aus
 * @author Christopher Jung
 *
 */
public class BattleTick extends TickController {

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		org.hibernate.Session db = getDB();
		
		/*
			Schlachten
		*/
		
		this.log("Schlachten: Aendere aktiven Kommandanten");
		long lastacttime = Common.time()-1800;
		
		db.createQuery("update Battle set blockcount=blockcount-1 where blockcount > 0 and lastturn<= ?")
			.setLong(0, lastacttime)
			.executeUpdate();
		
		List<?> battles = db.createQuery("from Battle where blockcount<=0 or lastaction<= ?")
			.setLong(0, lastacttime)
			.list();
		
		for( Iterator<?> iter=battles.iterator(); iter.hasNext(); ) {
			Battle battle = (Battle)iter.next();
			
			try {
				this.log("+ Naechste Runde bei Schlacht "+battle.getId());
			
				battle.load( battle.getCommander(0), null, null, 0 );
				
				//In der ersten Runde verzoegern wir grundsaetzlich - aufgehoben
				//maximal jedoch einmal, damit Schlachten nicht unendlich lange im System
				//vorhanden sind
				/*
				if( battle.hasFlag(Battle.FLAG_FIRSTROUND) ) {
					battle.setFlag(Battle.FLAG_FIRSTROUND, false);
					getContext().commit();
					
					continue;
				}
				*/
			
				if( battle.endTurn(false) ) {
					// Daten nur aktualisieren, wenn die Schlacht auch weiterhin existiert
					battle.logenemy("<endturn type=\"all\" side=\"-1\" time=\""+Common.time()+"\" tick=\""+getContext().get(ContextCommon.class).getTick()+"\" />\n");
				
					battle.writeLog();
					
					battle.addComMessage(battle.getOwnSide(), "++++ Das Tickscript hat die Runde beendet ++++\n\n");
					battle.addComMessage(battle.getEnemySide(), "++++ Das Tickscript hat die Runde beendet ++++\n\n");
				}
				getContext().commit();
			}
			catch( RuntimeException e ) {
				getContext().rollback();
				
				this.log("Battle "+battle.getId()+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "BattleTick Exception", "battle: "+battle.getId());
			}
			finally {
				db.evict(battle);
				HibernateFacade.evictAll(db, Ship.class, BattleShip.class);
			}
		}
	}

}
