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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.StatShips;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.scripting.NullLogger;
import net.driftingsouls.ds2.server.scripting.ScriptParserContext;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.TickController;

import org.apache.commons.lang.math.RandomUtils;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
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
		try {
			org.hibernate.Session db = getContext().getDB();
			
			this.log("Sprungantrieb");
			List<?> jumps = db.createQuery("from Jump as j inner join fetch j.ship").list();
			for( Iterator<?> iter=jumps.iterator(); iter.hasNext(); ) {
				Jump jump = (Jump)iter.next();
				
				this.log( jump.getShip().getId()+" springt nach "+jump.getLocation());
				
				jump.getShip().setSystem(jump.getSystem());
				jump.getShip().setX(jump.getX());
				jump.getShip().setY(jump.getY());
				
				db.createQuery("update Ship set x= :x, y= :y, system= :system where docked in (:dock,:land)")
					.setInteger("x", jump.getX())
					.setInteger("y", jump.getY())
					.setInteger("system", jump.getSystem())
					.setString("dock", Integer.toString(jump.getShip().getId()))
					.setString("land", "l "+jump.getShip().getId())
					.executeUpdate();
				
				db.delete(jump);
			}
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Verarbeiten der Sprungantriebe: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doJumps failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}
	
	/*
		Statistiken
	*/
	private void doStatistics() {
		try {
			org.hibernate.Session db = getDB();
		
			this.log("");
			this.log("Erstelle Statistiken");
		
			long shipcount = (Long)db.createQuery("select count(*) from Ship where id>0 and owner>0").iterate().next();
			long crewcount = (Long)db.createQuery("select sum(crew) from Ship where id>0 and owner>0").iterate().next();
			
			StatShips stat = new StatShips(shipcount, crewcount);
			db.persist(stat);
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Anlegen der Statistiken: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doStatistics failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}

	/*
		Vac-Modus
	*/
	private void doVacation() {
		try {
			org.hibernate.Session db = getDB();
			
			this.log("");
			this.log("Bearbeite Vacation-Modus");
			
			List<?> vacLeaveUsers = db.createQuery("from User where vaccount=1").list();
			for( Iterator<?> iter=vacLeaveUsers.iterator(); iter.hasNext(); ) {
				User user = (User)iter.next();
				user.setName(user.getName().replace(" [VAC]", ""));
				user.setNickname(user.getNickname().replace(" [VAC]", ""));
				
				this.log("\t"+user.getPlainname()+" ("+user.getId()+") verlaesst den VAC-Modus");
			}
			
			db.createQuery("update User set vaccount=vaccount-1 where vaccount>0 and wait4vac=0")
				.executeUpdate();

			List<User> users = Common.cast(db.createQuery("from User where wait4vac=1").list());
			for( User user : users ) {
				User newcommander = null;
				if( user.getAlly() != null ) {
					newcommander = (User)db.createQuery("from User where ally= :ally  and inakt <= 7 and vaccount=0 and (wait4vac>6 or wait4vac=0)")
						.setEntity("ally", user.getAlly())
						.setMaxResults(1)
						.uniqueResult();
				}
				
				List<?> battles = db.createQuery("from Battle where commander1= :user or commander2= :user")
					.setEntity("user", user)
					.list();
				for( Iterator<?> iter=battles.iterator(); iter.hasNext(); ) {
					Battle battle = (Battle)iter.next();
					battle.load(user, null, null, 0 );
					
					if( newcommander != null ) {
						this.log("\t\tUser"+user.getId()+": Die Leitung der Schlacht "+battle.getId()+" wurde an "+newcommander.getName()+" ("+newcommander.getId()+") uebergeben");
						
						battle.logenemy("<action side=\""+battle.getOwnSide()+"\" time=\""+Common.time()+"\" tick=\""+getContext().get(ContextCommon.class).getTick()+"\"><![CDATA[\n");
			
						PM.send(user, newcommander.getId(), "Schlacht &uuml;bernommen", "Die Leitung der Schlacht bei "+battle.getLocation()+" wurde dir automatisch &uuml;bergeben, da der bisherige Kommandant in den Vacationmodus gewechselt ist");
				
						battle.logenemy(Common._titleNoFormat(newcommander.getName())+" kommandiert nun die gegnerischen Truppen\n\n");
				
						battle.setCommander(battle.getOwnSide(), newcommander);
				
						battle.logenemy("]]></action>\n");
				
						battle.logenemy("<side"+(battle.getOwnSide()+1)+" commander=\""+battle.getCommander(battle.getOwnSide()).getId()+"\" ally=\""+battle.getAlly(battle.getOwnSide())+"\" />\n");
				
						battle.setTakeCommand(battle.getOwnSide(), 0);

						battle.writeLog();
					}
					else {
						this.log("\t\tUser"+user.getId()+": Die Schlacht "+battle.getId()+" wurde beendet");
					
						battle.endBattle(0, 0, true);
						PM.send(battle.getCommander(battle.getOwnSide()), battle.getCommander(battle.getEnemySide()).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation()+" wurde automatisch beim wechseln in den Vacation-Modus beendet, da kein Ersatzkommandant ermittelt werden konnte!");
					}
				}
				
				// TODO: Eine bessere Loesung fuer den Fall finden, wenn der Name mehr als 249 Zeichen lang ist
				String name = user.getName();
				String nickname = user.getNickname();
				
				if( name.length() > 249 ) {
					name = name.substring(0, 249);
				}
				if( nickname.length() > 249 ) {
					nickname = nickname.substring(0, 249);
				}
				
				user.setName(name+" [VAC]");
				user.setNickname(nickname+" [VAC]");
				
				this.log("\t"+user.getPlainname()+" ("+user.getId()+") ist in den VAC-Modus gewechselt");
			}
			
			
			db.createQuery("update User set wait4vac=wait4vac-1 where wait4vac>0")
				.executeUpdate();
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Verarbeiten der Vacationdaten: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doVacation failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}
	
	/*
	 * Unset Noob Protection for experienced players
	 */
	private void doNoobProtection(){
		org.hibernate.Session db = getDB();
		
		this.log("");
		this.log("Bearbeite Noob-Protection");
		
		List<?> noobUsers = db.createQuery("from User where id>0 and flags LIKE '%" + User.FLAG_NOOB+"%'").list();
		int noobDays = 30;
		int noobTime = 24*60*60*noobDays;
		for( Iterator<?> iter=noobUsers.iterator(); iter.hasNext(); ) {
			User user = (User)iter.next();
			
			if( !user.hasFlag(User.FLAG_NOOB) ) {
				continue;
			}
			
			if (user.getSignup() <= Common.time() - noobTime){
				user.setFlag(User.FLAG_NOOB, false);
				this.log("Entferne Noob-Schutz bei "+user.getId());
				
				User nullUser = (User)db.get(User.class, 0);
				PM.send(nullUser, user.getId(), "GCP-Schutz aufgehoben", 
						"Ihr GCP-Schutz wurde durch das System aufgehoben. " +
						"Dies passiert automatisch "+noobDays+" Tage nach der Registrierung. " +
						"Sie sind nun angreifbar, koennen aber auch selbst angreifen.", 
						PM.FLAGS_AUTOMATIC | PM.FLAGS_IMPORTANT);
			}
		}
	}
	
	/*
	
		Neue Felsbrocken spawnen lassen
			
	*/	
	private void doFelsbrocken() {
		try {
			Database db = getDatabase();
			org.hibernate.Session database = getDB();
			
			this.log("");
			this.log("Fuege Felsbrocken ein");
				
			int shouldId = 9999;
			
			SQLQuery system = db.query("SELECT system,count," +
					"(SELECT count(*) FROM ships WHERE system=config_felsbrocken_systems.system AND type IN " +
					"	(SELECT shiptype FROM config_felsbrocken WHERE system=config_felsbrocken_systems.system)" +
					") present " +
					"FROM config_felsbrocken_systems ORDER BY system");
			while( system.next() ) {
				int shipcount = system.getInt("present");
				
				this.log("\tSystem "+system.getInt("system")+": "+shipcount+" / "+system.getInt("count")+" Felsbrocken");
				
				if( system.getInt("count") < shipcount ) {
					continue;
				}
				
				List<SQLResultRow> loadout = new ArrayList<SQLResultRow>();
				SQLQuery aLoadOut = db.query("SELECT * FROM config_felsbrocken WHERE system=",system.getInt("system"));
				while( aLoadOut.next() ) {
					loadout.add(aLoadOut.getRow());
				}
				aLoadOut.free();
				
				while( shipcount < system.getInt("count") ) {
					int rnd = RandomUtils.nextInt(100)+1;
					int currnd = 0;
					for( int i=0; i < loadout.size(); i++ ) {
						SQLResultRow aloadout = loadout.get(i);
						currnd += aloadout.getInt("chance");
		
						if( currnd < rnd ) {
							continue;
						}
						
						// ID ermitteln
						shouldId++;
						shouldId = db.first("SELECT newIntelliShipID( "+shouldId+" ) AS sid").getInt("sid");
						
						StarSystem thissystem = (StarSystem)database.get(StarSystem.class, system.getInt("system"));
						
						// Koords ermitteln
						int x = RandomUtils.nextInt(thissystem.getWidth())+1;
						int y = RandomUtils.nextInt(thissystem.getHeight())+1;
						
						this.log("\t*System "+system.getInt("system")+": Fuege Felsbrocken "+shouldId+" ein");
						
						// Ladung einfuegen
						this.log("\t- Loadout: ");					
						Cargo cargo = new Cargo(Cargo.Type.STRING, aloadout.getString("cargo"));
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist ) {
							this.log("\t   *"+res.getName()+" => "+res.getCount1());
						}
						
						ShipTypeData shiptype = Ship.getShipType(aloadout.getInt("shiptype"));
						
						// Schiffseintrag einfuegen
						db.update("INSERT INTO ships (id,name,type,owner,x,y,system,hull,crew,cargo,heat,docked,destcom,jumptarget,history,status) ",
									"VALUES (",shouldId,",'Felsbrocken',",aloadout.getInt("shiptype"),",-1,",x,",",y,",",system.getInt("system"),",",shiptype.getHull(),",",shiptype.getCrew(),",'",cargo.save(),"','','','','','','')");
						this.log("");
						
						shipcount++;
						
						break;
					}
				}
			}
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Erstellen der Felsbrocken: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doFelsbrocken failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}
	
	/*
		Quests bearbeiten
	*/
	private void doQuests() {
		try {
			Database db = getContext().getDatabase();
			
			this.log("Bearbeite Quests [ontick]");
			SQLQuery rquest = db.query("SELECT * FROM quests_running WHERE ontick IS NOT NULL ORDER BY questid");
			if( rquest.numRows() == 0 ) { 
				rquest.free();
				return;
			}
			ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
			scriptparser.getContext().setErrorWriter(new NullLogger());	
			
			while( rquest.next() ) {
				try {
					Blob execdata = rquest.getBlob("execdata");
					if( (execdata != null) && (execdata.length() > 0) ) { 
						scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
					}
					else {
						scriptparser.setContext(new ScriptParserContext());
					}
						
					this.log("* quest: "+rquest.getInt("questid")+" - user:"+rquest.getInt("userid")+" - script: "+rquest.getInt("ontick"));
						
					String script = db.first("SELECT script FROM scripts WHERE id='"+rquest.getInt("ontick")+"'").getString("script");
					
					final Bindings engineBindings = scriptparser.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
					
					engineBindings.put("USER", Integer.toString(rquest.getInt("userid")) );
					engineBindings.put("QUEST", "r"+rquest.getInt("id"));
					engineBindings.put("SCRIPT", Integer.toString(rquest.getInt("ontick")) );
					engineBindings.put("_PARAMETERS", "0");
					scriptparser.eval(script);
						
					int usequest = Integer.parseInt((String)engineBindings.get("QUEST"));
						
					if( usequest != 0 ) {
						ScriptParserContext.toStream(scriptparser.getContext(), execdata.setBinaryStream(1));
						db.prepare("UPDATE quests_running SET execdata=? WHERE id=? ")
							.update(execdata, rquest.getInt("id"));
					}
				}
				catch( Exception e ) {
					this.log("[FEHLER] Konnte Quest-Tick fuehr Quest "+rquest.getInt("questid")+" (Running-ID: "+rquest.getInt("id")+") nicht ausfuehren."+e);
					e.printStackTrace();
					Common.mailThrowable(e, "RestTick Exception", "Quest failed: "+rquest.getInt("questid")+"\nRunning-ID: "+rquest.getInt("id"));
				}
			}
			rquest.free();
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Verarbeiten der Quests: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doQuests failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}
	
	/*
	 * 
 	 * Tasks bearbeiteten (Timeouts)
	 * 
	 */
	private void doTasks() {
		try {
			this.log("Bearbeite Tasks [tick_timeout]");
			
			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasklist = taskmanager.getTasksByTimeout(1);
			for( int i=0; i < tasklist.length; i++ ) {
				this.log("* "+tasklist[i].getTaskID()+" ("+tasklist[i].getType()+") -> sending tick_timeout");
				taskmanager.handleTask( tasklist[i].getTaskID(), "tick_timeout" );	
			}
			
			taskmanager.reduceTimeout(1);
		}
		catch( RuntimeException e ) {
			this.log("Fehler beim Bearbeiten der Tasks: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doTasks failed");
			
			getContext().rollback();
			getDB().clear();
		}
	}
	
	@Override
	protected void tick() {
		Database database = getContext().getDatabase();
		org.hibernate.Session db = getDB();
		
		this.log("Transmissionen - gelesen+1");
		database.update("UPDATE transmissionen SET gelesen=gelesen+1 WHERE gelesen>=2");
		
		this.log("Loesche alte Transmissionen");
		database.update("DELETE FROM transmissionen WHERE gelesen>=10");
		
		this.log("Erhoehe Inaktivitaet der Spieler");
		db.createQuery("update User set inakt=inakt+1 where vaccount=0")
			.executeUpdate();
		
		this.log("Erhoehe Tickzahl");
		ConfigValue value = (ConfigValue)getDB().get(ConfigValue.class, "ticks");
		int ticks = Integer.valueOf(value.getValue()) + 1;
		value.setValue(Integer.toString(ticks));
		getContext().commit();
				
		this.doJumps();
		getContext().commit();
		
		this.doStatistics();
		getContext().commit();
		
		this.doVacation();
		getContext().commit();
		
		this.doNoobProtection();
		getContext().commit();
		
		this.doFelsbrocken();
		getContext().commit();
		
		this.doQuests();
		getContext().commit();
		
		this.doTasks();
		getContext().commit();
		
		this.log("Zaehle Timeout bei Umfragen runter");
		database.update("UPDATE surveys SET timeout=timeout-1 WHERE timeout>0");
	}

}
