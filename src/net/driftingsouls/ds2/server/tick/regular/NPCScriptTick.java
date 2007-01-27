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

import java.sql.Blob;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.UserIterator;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Fuehrt NPC-Aktionsscripte aus
 * @author Christopher Jung
 *
 */
public class NPCScriptTick extends TickController {
	class TickLogger implements ScriptParser.Logger {
		public void log(String txt) {
			NPCScriptTick.this.slog(txt);		
		}

		public void start() {
			NPCScriptTick.this.log("###################Scriptparser [Debug]###################");
		}

		public void stop() {
			NPCScriptTick.this.log("#########################ENDE#############################");
		}
	}

	@Override
	protected void prepare() {
		// EMPTY
	}

	@Override
	protected void tick() {
		Database db = getContext().getDatabase();
		this.log("Fuehre automatische NPC-Aktionen durch");
		
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.ACTION);
		
		UserIterator iter = getContext().createUserIterator("SELECT * FROM users WHERE LOCATE('execnotes',flags)");
		for( User user : iter ) {
			if( !user.hasFlag( User.FLAG_SCRIPT_DEBUGGING ) ) {
				scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);
			}
			else {
				scriptparser.setLogFunction(new TickLogger());	
			}
			this.log("+++++++++ User: "+user.getID()+" +++++++++");
			SQLQuery ship = db.query("SELECT * FROM ships WHERE id>0 AND owner='",user.getID(),"' AND battle=0 AND script IS NOT NULL");
			while( ship.next() ) {			
				try {
					this.log("+++ Ship "+ship.getInt("id")+" +++");
					scriptparser.setShip( ship.getRow() );
					Blob scriptExecData = ship.getBlob("scriptexedata");
					if( (scriptExecData != null) && (scriptExecData.length() > 0) ) {
						scriptparser.setExecutionData(scriptExecData.getBinaryStream());
					}
					
					scriptparser.executeScript( db, ship.getString("script") );
					
					scriptparser.writeExecutionData(scriptExecData.setBinaryStream(1));
					db.prepare("UPDATE ships SET scriptexedata=? WHERE id=? ")
						.update(scriptExecData, ship.getInt("id"));
				}
				catch( Exception e ) {
					this.log("[FEHLER] Kann Script auf Schiff "+ship.getInt("id")+" nicht ausfuehren: "+e);
					e.printStackTrace();
				}
				scriptparser.cleanup();
			}
			ship.free();
		}
		iter.free();
	}

}
