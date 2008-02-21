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

import java.util.ArrayList;
import java.util.List;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
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
		Database db = getDatabase();
		
		/*
			Schlachten
		*/
		
		this.log("Schlachten: Aendere aktiven Kommandanten");
		long lastacttime = Common.time()-1800;
		
		db.update("UPDATE battles SET blockcount=blockcount-1 WHERE blockcount > 0 AND lastturn<=",lastacttime);
		
		getContext().commit();
		
		List<SQLResultRow> battleList = new ArrayList<SQLResultRow>();
		SQLQuery battleQuery = db.query("SELECT id,commander1 FROM battles WHERE blockcount<=0 OR lastaction<=",lastacttime);
		while( battleQuery.next() ) {
			battleList.add(battleQuery.getRow());
		}
		battleQuery.free();
		
		for( int i=0; i < battleList.size(); i++ ) {
			SQLResultRow battledata = battleList.get(i);
			try {
				this.log("+ Naechste Runde bei Schlacht "+battledata.getInt("id"));
			
				int comid = battledata.getInt("commander1");
			
				Battle battle = new Battle();
				battle.load( battledata.getInt("id"), comid, 0, 0, 0 );
			
				if( battle.endTurn(false) ) {
					// Daten nur aktuallisieren, wenn die Schlacht auch weiterhin existiert
					battle.logenemy("<endturn type=\"all\" side=\"-1\" time=\""+Common.time()+"\" tick=\""+getContext().get(ContextCommon.class).getTick()+"\" />\n");
				
					battle.save(true);
				
					battle.writeLog();
					
					battle.addComMessage(battle.getOwnSide(), "++++ Das Tickscript hat die Runde beendet ++++\n\n");
					battle.addComMessage(battle.getEnemySide(), "++++ Das Tickscript hat die Runde beendet ++++\n\n");
				}
				getContext().commit();
			}
			catch( Exception e ) {
				this.log("Battle "+battledata.getInt("id")+" failed: "+e);
				e.printStackTrace();
				Common.mailThrowable(e, "BattleTick Exception", "battle: "+battledata.getInt("id"));
			}
			
			getDB().clear();
		}
	}

}
