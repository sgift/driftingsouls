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
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.TickController;

import org.apache.commons.lang.math.RandomUtils;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 * @author Christopher Jung
 *
 */
public class RestTick extends TickController {

	@Override
	protected void prepare()
	{
		// EMPTY
	}

	/*
		Sprungantrieb
	*/
	private void doJumps()
	{
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();
		try
		{
			this.log("Sprungantrieb");
			List<?> jumps = db.createQuery("from Jump as j inner join fetch j.ship").list();
			for( Iterator<?> iter=jumps.iterator(); iter.hasNext(); )
			{
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
			transaction.commit();
		}
		catch( RuntimeException e )
		{
			transaction.rollback();
			this.log("Fehler beim Verarbeiten der Sprungantriebe: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doJumps failed");

			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				db.evict(db.get(sose.getEntityName(), sose.getIdentifier()));
			}
		}
	}

	/*
		Statistiken
	*/
	private void doStatistics()
	{
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();
		try
		{
			this.log("");
			this.log("Erstelle Statistiken");

			long shipcount = (Long)db.createQuery("select count(*) from Ship where id>0 and owner>0").iterate().next();
			long crewcount = (Long)db.createQuery("select sum(crew) from Ship where id>0 and owner>0").iterate().next();

			StatShips stat = new StatShips(shipcount, crewcount);
			db.persist(stat);
			transaction.commit();
		}
		catch( RuntimeException e )
		{
			transaction.rollback();
			this.log("Fehler beim Anlegen der Statistiken: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doStatistics failed");

			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				db.evict(db.get(sose.getEntityName(), sose.getIdentifier()));
			}
		}
	}

	/*
		Vac-Modus
	*/
	private void doVacation()
	{
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();
		try
		{
			this.log("");
			this.log("Bearbeite Vacation-Modus");

			List<?> vacLeaveUsers = db.createQuery("from User where vaccount=1").list();
			for( Iterator<?> iter=vacLeaveUsers.iterator(); iter.hasNext(); )
			{
				User user = (User)iter.next();
				user.setName(user.getName().replace(" [VAC]", ""));
				user.setNickname(user.getNickname().replace(" [VAC]", ""));

				this.log("\t"+user.getPlainname()+" ("+user.getId()+") verlaesst den VAC-Modus");
			}

			db.createQuery("update User set vaccount=vaccount-1 where vaccount>0 and wait4vac=0")
				.executeUpdate();

			List<User> users = Common.cast(db.createQuery("from User where wait4vac=1").list());
			for( User user : users )
			{
				User newcommander = null;
				if( user.getAlly() != null )
				{
					newcommander = (User)db.createQuery("from User where ally= :ally  and inakt <= 7 and vaccount=0 and (wait4vac>6 or wait4vac=0)")
						.setEntity("ally", user.getAlly())
						.setMaxResults(1)
						.uniqueResult();
				}

				List<?> battles = db.createQuery("from Battle where commander1= :user or commander2= :user")
					.setEntity("user", user)
					.list();
				for( Iterator<?> iter=battles.iterator(); iter.hasNext(); )
				{
					Battle battle = (Battle)iter.next();
					battle.load(user, null, null, 0 );

					if( newcommander != null )
					{
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
					else
					{
						this.log("\t\tUser"+user.getId()+": Die Schlacht "+battle.getId()+" wurde beendet");

						battle.endBattle(0, 0, true);
						PM.send(battle.getCommander(battle.getOwnSide()), battle.getCommander(battle.getEnemySide()).getId(), "Schlacht beendet", "Die Schlacht bei "+battle.getLocation()+" wurde automatisch beim wechseln in den Vacation-Modus beendet, da kein Ersatzkommandant ermittelt werden konnte!");
					}
				}

				// TODO: Eine bessere Loesung fuer den Fall finden, wenn der Name mehr als 249 Zeichen lang ist
				String name = user.getName();
				String nickname = user.getNickname();

				if( name.length() > 249 )
				{
					name = name.substring(0, 249);
				}
				if( nickname.length() > 249 )
				{
					nickname = nickname.substring(0, 249);
				}

				user.setName(name+" [VAC]");
				user.setNickname(nickname+" [VAC]");

				this.log("\t"+user.getPlainname()+" ("+user.getId()+") ist in den VAC-Modus gewechselt");
			}


			db.createQuery("update User set wait4vac=wait4vac-1 where wait4vac>0")
				.executeUpdate();
			transaction.commit();
		}
		catch( RuntimeException e )
		{
			transaction.rollback();
			this.log("Fehler beim Verarbeiten der Vacationdaten: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doVacation failed");

			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				db.evict(db.get(sose.getEntityName(), sose.getIdentifier()));
			}
		}
	}

	/*
	 * Unset Noob Protection for experienced players
	 */
	private void doNoobProtection()
	{
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();

		try
		{
			this.log("");
			this.log("Bearbeite Noob-Protection");

			List<?> noobUsers = db.createQuery("from User where id>0 and flags LIKE '%" + User.FLAG_NOOB+"%'").list();
			int noobDays = 30;
			int noobTime = 24*60*60*noobDays;
			for( Iterator<?> iter=noobUsers.iterator(); iter.hasNext(); )
			{
				User user = (User)iter.next();

				if( !user.hasFlag(User.FLAG_NOOB) )
				{
					continue;
				}

				if (user.getSignup() <= Common.time() - noobTime)
				{
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
			transaction.commit();
		}
		catch(RuntimeException e)
		{
			transaction.rollback();
			this.log("Fehler beim Aufheben des Noobschutzes: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doNoobProtecttion failed");

			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				db.evict(db.get(sose.getEntityName(), sose.getIdentifier()));
			}
		}
	}

	/*

		Neue Felsbrocken spawnen lassen

	*/
	private void doFelsbrocken()
	{
		Database db = getDatabase();
		org.hibernate.Session database = getDB();
		Transaction transaction = database.beginTransaction();
		try
		{
			User owner = (User)database.get(User.class, -1);
			String currentTime = Common.getIngameTime(getContext().get(ContextCommon.class).getTick());

			this.log("");
			this.log("Fuege Felsbrocken ein");

			int shouldId = 9999;

			SQLQuery system = db.query("SELECT system,count," +
					"(SELECT count(*) FROM ships WHERE system=config_felsbrocken_systems.system AND type IN " +
					"	(SELECT shiptype FROM config_felsbrocken WHERE system=config_felsbrocken_systems.system)" +
					") present " +
					"FROM config_felsbrocken_systems ORDER BY system");
			while( system.next() )
			{
				int shipcount = system.getInt("present");

				this.log("\tSystem "+system.getInt("system")+": "+shipcount+" / "+system.getInt("count")+" Felsbrocken");

				if( system.getInt("count") < shipcount )
				{
					continue;
				}

				List<SQLResultRow> loadout = new ArrayList<SQLResultRow>();
				SQLQuery aLoadOut = db.query("SELECT * FROM config_felsbrocken WHERE system=",system.getInt("system"));
				while( aLoadOut.next() )
				{
					loadout.add(aLoadOut.getRow());
				}
				aLoadOut.free();

				while( shipcount < system.getInt("count") )
				{
					int rnd = RandomUtils.nextInt(100)+1;
					int currnd = 0;
					for( int i=0; i < loadout.size(); i++ )
					{
						SQLResultRow aloadout = loadout.get(i);
						currnd += aloadout.getInt("chance");

						if( currnd < rnd )
						{
							continue;
						}

						StarSystem thissystem = (StarSystem)database.get(StarSystem.class, system.getInt("system"));

						// Koords ermitteln
						int x = RandomUtils.nextInt(thissystem.getWidth())+1;
						int y = RandomUtils.nextInt(thissystem.getHeight())+1;

						shouldId = (Integer)database.createSQLQuery("select newIntelliShipId( ? )")
							.setInteger(0, ++shouldId)
							.uniqueResult();

						this.log("\t*System "+system.getInt("system")+": Fuege Felsbrocken "+shouldId+" ein");

						// Ladung einfuegen
						this.log("\t- Loadout: ");
						Cargo cargo = new Cargo(Cargo.Type.STRING, aloadout.getString("cargo"));
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist )
						{
							this.log("\t   *"+res.getPlainName()+" => "+res.getCount1());
						}

						ShipType shiptype = (ShipType)database.get(ShipType.class, aloadout.getInt("shiptype"));

						Ship brocken = new Ship(owner, shiptype, system.getInt("system"), x, y);
						brocken.getHistory().addHistory("Indienststellung als Felsbrocken am "+currentTime+" durch den Tick");
						brocken.setName("Felsbrocken");
						brocken.setId(shouldId);
						brocken.setHull(shiptype.getHull());
						brocken.setCrew(shiptype.getCrew());
						brocken.setCargo(cargo);
						brocken.setHeat(0);
						brocken.setEngine(100);
						brocken.setWeapons(100);
						brocken.setComm(100);
						brocken.setSensors(100);
						brocken.setAblativeArmor(shiptype.getAblativeArmor());
						brocken.setEnergy(shiptype.getEps());

						// Schiffseintrag einfuegen
						database.save(brocken);
						database.save(brocken.getHistory());

						this.log("");

						shipcount++;

						break;
					}
				}
			}
			transaction.commit();
		}
		catch( RuntimeException e )
		{
			transaction.rollback();
			this.log("Fehler beim Erstellen der Felsbrocken: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doFelsbrocken failed");
		}
	}

	/*
		Quests bearbeiten
	*/
	private void doQuests()
	{
		try
		{
			Database db = getDatabase();

			this.log("Bearbeite Quests [ontick]");
			SQLQuery rquest = db.query("SELECT * FROM quests_running WHERE ontick IS NOT NULL ORDER BY questid");
			if( rquest.numRows() == 0 )
			{
				rquest.free();
				return;
			}
			ScriptEngine scriptparser = getContext().get(ContextCommon.class).getScriptParser("DSQuestScript");
			scriptparser.getContext().setErrorWriter(new NullLogger());

			while( rquest.next() )
			{
				try
				{
					Blob execdata = rquest.getBlob("execdata");
					if( (execdata != null) && (execdata.length() > 0) )
					{
						scriptparser.setContext(ScriptParserContext.fromStream(execdata.getBinaryStream()));
					}
					else
					{
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

					if( usequest != 0 )
					{
						ScriptParserContext.toStream(scriptparser.getContext(), execdata.setBinaryStream(1));
						db.prepare("UPDATE quests_running SET execdata=? WHERE id=? ")
							.update(execdata, rquest.getInt("id"));
					}
				}
				catch( Exception e )
				{
					this.log("[FEHLER] Konnte Quest-Tick fuehr Quest "+rquest.getInt("questid")+" (Running-ID: "+rquest.getInt("id")+") nicht ausfuehren."+e);
					e.printStackTrace();
					Common.mailThrowable(e, "RestTick Exception", "Quest failed: "+rquest.getInt("questid")+"\nRunning-ID: "+rquest.getInt("id"));
				}
			}
			rquest.free();
		}
		catch( RuntimeException e )
		{
			this.log("Fehler beim Verarbeiten der Quests: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doQuests failed");
		}
	}

	/*
	 *
 	 * Tasks bearbeiteten (Timeouts)
	 *
	 */
	private void doTasks()
	{
		Transaction transaction = getDB().beginTransaction();
		try
		{
			this.log("Bearbeite Tasks [tick_timeout]");

			Taskmanager taskmanager = Taskmanager.getInstance();
			Task[] tasklist = taskmanager.getTasksByTimeout(1);
			for( int i=0; i < tasklist.length; i++ )
			{
				this.log("* "+tasklist[i].getTaskID()+" ("+tasklist[i].getType()+") -> sending tick_timeout");
				taskmanager.handleTask( tasklist[i].getTaskID(), "tick_timeout" );
			}

			taskmanager.reduceTimeout(1);
			transaction.commit();
		}
		catch( RuntimeException e )
		{
			transaction.rollback();
			this.log("Fehler beim Bearbeiten der Tasks: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doTasks failed");

			if( e instanceof StaleObjectStateException ) {
				StaleObjectStateException sose = (StaleObjectStateException)e;
				getDB().evict(getDB().get(sose.getEntityName(), sose.getIdentifier()));
			}
		}
	}

	@Override
	protected void tick()
	{
		org.hibernate.Session db = getDB();

		Transaction transaction = db.beginTransaction();
		try
		{
			this.log("Transmissionen - gelesen+1");
			db.createQuery("UPDATE PM SET gelesen = gelesen+1 WHERE gelesen>=2").executeUpdate();

			this.log("Loesche alte Transmissionen");
			db.createQuery("DELETE FROM PM WHERE gelesen>=10").executeUpdate();

			this.log("Erhoehe Inaktivitaet der Spieler");
			db.createQuery("update User set inakt=inakt+1 where vaccount=0")
				.executeUpdate();

			this.log("Erhoehe Tickzahl");
			ConfigValue value = (ConfigValue)getDB().get(ConfigValue.class, "ticks");
			int ticks = Integer.valueOf(value.getValue()) + 1;
			value.setValue(Integer.toString(ticks));
			transaction.commit();
		}
		catch(RuntimeException e)
		{
			transaction.rollback();
			throw e;
		}

		this.doJumps();
		this.doStatistics();
		this.doVacation();
		this.doNoobProtection();
		this.doFelsbrocken();
		this.doQuests();
		this.doTasks();
	}
}
