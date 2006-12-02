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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.scripting.ScriptParser;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.TickController;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben
 * @author Christopher Jung
 *
 */
public class RestTick extends TickController {

	@Override
	protected void prepare() {
		// EMPTY
	}
	
	/*
		Sprungantrieb
	*/
	private void doJumps() {
		Database db = getContext().getDatabase();
		
		this.log("Sprungantrieb");
		SQLQuery jump = db.query("SELECT id,x,y,system,shipid FROM jumps");
		while( jump.next() ) {
			this.log( jump.getInt("shipid")+" springt nach "+jump.getInt("system")+":"+jump.getInt("x")+"/"+jump.getInt("y"));
			
			db.update("UPDATE ships SET x=",jump.getInt("x"),",y=",jump.getInt("y"),",system=",jump.getInt("system")," WHERE id>0 AND id=",jump.getInt("shipid")," OR docked='",jump.getInt("shipid"),"' OR docked='l ",jump.getInt("shipid"),"'");
			db.update("DELETE FROM jumps WHERE id=",jump.getInt("id"));
		}
		jump.free();
	}
	
	/*
		Statistiken
	*/
	private void doStatistics() {
		Database db = getContext().getDatabase();
		
		this.log("");
		this.log("Erstelle Statistiken");
		
		int shipcount = db.first("SELECT count(*) count FROM ships WHERE id>0 AND owner>1").getInt("count");
		long crewcount = db.first("SELECT sum(crew) totalcrew FROM ships WHERE id>0 AND owner > 0").getLong("totalcrew");
		int tick = getContext().get(ContextCommon.class).getTick();
		
		db.update("INSERT INTO stats_ships (tick,shipcount,crewcount) VALUES (",tick,",",shipcount,",",crewcount,")");
	}

	/*
		Vac-Modus
	*/
	private void doVacation() {
		// TODO
		Common.stub();
	}
	
	/*
	
		Neue Felsbrocken spawnen lassen
			
	*/	
	private void doFelsbrocken() {
		// TODO
		Common.stub();
	}
	
	/*
		Quests bearbeiten
	*/
	private void doQuests() {
		Database db = getContext().getDatabase();
		
		this.log("Bearbeite Quests [ontick]");
		SQLQuery rquest = db.query("SELECT * FROM quests_running WHERE ontick IS NOT NULL ORDER BY questid");
		if( rquest.numRows() == 0 ) { 
			rquest.free();
			return;
		}
		ScriptParser scriptparser = getContext().get(ContextCommon.class).getScriptParser(ScriptParser.NameSpace.QUEST);
		scriptparser.setShip(null);
		scriptparser.setLogFunction(ScriptParser.LOGGER_NULL);	
		
		while( rquest.next() ) {
			scriptparser.cleanup();
			try {
				Blob execdata = rquest.getBlob("execdata");
				if( (execdata != null) && (execdata.length() > 0) ) { 
					scriptparser.setExecutionData( execdata.getBinaryStream() );
				}
					
				this.log("* quest: "+rquest.getInt("questid")+" - user:"+rquest.getInt("userid")+" - script: "+rquest.getInt("ontick"));
					
				String script = db.first("SELECT script FROM scripts WHERE id='"+rquest.getInt("ontick")+"'").getString("script");
				scriptparser.setRegister("USER", Integer.toString(rquest.getInt("userid")) );
				scriptparser.setRegister("QUEST", "r"+rquest.getInt("id"));
				scriptparser.setRegister("SCRIPT", Integer.toString(rquest.getInt("ontick")) );		
				scriptparser.executeScript(db, script, "0");
					
				int usequest = Integer.parseInt(scriptparser.getRegister("QUEST"));
					
				if( usequest != 0 ) {
					scriptparser.writeExecutionData(execdata.setBinaryStream(1));	
				}
			}
			catch( Exception e ) {
				this.log("[FEHLER] Konnte Quest-Tick fuehr Quest "+rquest.getInt("questid")+" (Running-ID: "+rquest.getInt("id")+") nicht ausfuehren."+e);
				e.printStackTrace();
			}
		}
		rquest.free();
	}
	
	/*
	 * 
 	 * Tasks bearbeiteten (Timeouts)
	 * 
	 */
	private void doTasks() {
		this.log("Bearbeite Tasks [tick_timeout]");
		
		Taskmanager taskmanager = Taskmanager.getInstance();
		Task[] tasklist = taskmanager.getTasksByTimeout(1);
		for( int i=0; i < tasklist.length; i++ ) {
			this.log("* "+tasklist[i].getTaskID()+" ("+tasklist[i].getType()+") -> sending tick_timeout");
			taskmanager.handleTask( tasklist[i].getTaskID(), "tick_timeout" );	
		}
		
		taskmanager.reduceTimeout(1);
	}
	
	@Override
	protected void tick() {
		Database db = getContext().getDatabase();
		
		this.log("Transmissionen - gelesen+1");
		db.update("UPDATE transmissionen SET gelesen=gelesen+1 WHERE gelesen>=2");
		
		this.log("Loesche alte Transmissionen");
		db.update("DELETE FROM transmissionen WHERE gelesen>=10");
		
		this.log("Erhoehe Inaktivitaet der Spieler");
		db.update("UPDATE users SET inakt=inakt+1 WHERE vaccount=0");
		
		this.log("Erhoehe Tickzahl");
		db.update("UPDATE config SET ticks=ticks+1");
		
		
		this.doJumps();
		this.doStatistics();
		this.doVacation();
		this.doFelsbrocken();
		this.doQuests();
		this.doTasks();
		
		this.log("Zaehle Timeout bei Umfragen runter");
		db.update("UPDATE surveys SET timeout=timeout-1 WHERE timeout>0");
		
		this.log("Entsperre alle evt noch durch den Tick gesperrten Accounts");
		this.unblock(0);
	}

}
