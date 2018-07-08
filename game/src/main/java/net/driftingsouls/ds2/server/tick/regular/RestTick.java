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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.SchlachtLogKommandantWechselt;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.ConfigFelsbrocken;
import net.driftingsouls.ds2.server.config.ConfigFelsbrockenSystem;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.statistik.StatShips;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.ConfigValue;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.Taskmanager;
import net.driftingsouls.ds2.server.tick.TickController;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 * @author Christopher Jung
 *
 */
@Service("regularRestTick")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
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
			for (Object jump1 : jumps)
			{
				Jump jump = (Jump) jump1;

				this.log(jump.getShip().getId() + " springt nach " + jump.getSystem() + ":" + jump.getX() + "/" + jump.getY());

				jump.getShip().setLocation(jump);

				db.createQuery("update Ship set x= :x, y= :y, system= :system where docked in (:dock,:land)")
						.setInteger("x", jump.getX())
						.setInteger("y", jump.getY())
						.setInteger("system", jump.getSystem())
						.setString("dock", Integer.toString(jump.getShip().getId()))
						.setString("land", "l " + jump.getShip().getId())
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

			Long shipcount = (Long)db.createQuery("select count(*) from Ship where id>0 and owner.id>0").iterate().next();
			Long crewcount = (Long)db.createQuery("select sum(crew) from Ship where id>0 and owner.id>0").iterate().next();

			StatShips stat = new StatShips(shipcount != null ? shipcount : 0, crewcount != null ? crewcount : 0);
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
			for (Object vacLeaveUser : vacLeaveUsers)
			{
				User user = (User) vacLeaveUser;
				user.setName(user.getName().replace(" [VAC]", ""));
				user.setNickname(user.getNickname().replace(" [VAC]", ""));

				this.log("\t" + user.getPlainname() + " (" + user.getId() + ") verlaesst den VAC-Modus");
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
				for (Object battle1 : battles)
				{
					Battle battle = (Battle) battle1;
					battle.load(user, null, null, 0);

					if (newcommander != null)
					{
						this.log("\t\tUser" + user.getId() + ": Die Leitung der Schlacht " + battle.getId() + " wurde an " + newcommander.getName() + " (" + newcommander.getId() + ") uebergeben");

						PM.send(user, newcommander.getId(), "Schlacht &uuml;bernommen", "Die Leitung der Schlacht bei " + battle.getLocation().displayCoordinates(false) + " wurde dir automatisch &uuml;bergeben, da der bisherige Kommandant in den Vacationmodus gewechselt ist");

						battle.log(new SchlachtLogAktion(battle.getOwnSide(), Common._titleNoFormat(newcommander.getName()) + " kommandiert nun die gegnerischen Truppen"));

						battle.setCommander(battle.getOwnSide(), newcommander);

						battle.log(new SchlachtLogKommandantWechselt(battle.getOwnSide(), battle.getCommander(battle.getOwnSide())));

						battle.setTakeCommand(battle.getOwnSide(), 0);
					}
					else
					{
						this.log("\t\tUser" + user.getId() + ": Die Schlacht " + battle.getId() + " wurde beendet");

						battle.endBattle(0, 0);
						PM.send(battle.getCommander(battle.getOwnSide()), battle.getCommander(battle.getEnemySide()).getId(), "Schlacht beendet", "Die Schlacht bei " + battle.getLocation().displayCoordinates(false) + " wurde automatisch beim wechseln in den Vacation-Modus beendet, da kein Ersatzkommandant ermittelt werden konnte!");
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

			List<?> noobUsers = db.createQuery("from User where id>0 and flags LIKE '%" + UserFlag.NOOB.getFlag()+"%'").list();
			int noobDays = 30;
			int noobTime = 24*60*60*noobDays;
			for (Object noobUser : noobUsers)
			{
				User user = (User) noobUser;

				if (!user.hasFlag(UserFlag.NOOB))
				{
					continue;
				}

				if (user.getSignup() <= Common.time() - noobTime)
				{
					user.setFlag(UserFlag.NOOB, false);
					this.log("Entferne Noob-Schutz bei " + user.getId());

					User nullUser = (User) db.get(User.class, 0);
					PM.send(nullUser, user.getId(), "GCP-Schutz aufgehoben",
							"Ihr GCP-Schutz wurde durch das System aufgehoben. " +
									"Dies passiert automatisch " + noobDays + " Tage nach der Registrierung. " +
									"Sie sind nun angreifbar, koennen aber auch selbst angreifen.",
							PM.FLAGS_AUTOMATIC | PM.FLAGS_IMPORTANT
					);
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
		org.hibernate.Session db = getDB();
		Transaction transaction = db.beginTransaction();
		try
		{
			User owner = (User)db.get(User.class, -1);
			String currentTime = Common.getIngameTime(getContext().get(ContextCommon.class).getTick());

			this.log("");
			this.log("Fuege Felsbrocken ein");

			int shouldId = 9999;

			List<?> systemList = db
				.createQuery("from ConfigFelsbrockenSystem cfs")
				.list();
			for( Object obj : systemList )
			{
				ConfigFelsbrockenSystem cfs = (ConfigFelsbrockenSystem)obj;

				long shipcount = (Long)db.createQuery("select count(*) " +
						"from Ship s " +
						"where s.system=:system and " +
							"s.shiptype in (select shiptype from ConfigFelsbrocken where system=:cfs)")
					.setInteger("system", cfs.getSystem().getID())
					.setEntity("cfs", cfs)
					.iterate().next();

				this.log("\tSystem "+cfs.getSystem().getID()+"("+cfs.getName()+"): "+shipcount+" / "+cfs.getCount()+" Felsbrocken");

				if( cfs.getCount() < shipcount )
				{
					continue;
				}

				Set<ConfigFelsbrocken> loadout = cfs.getFelsbrocken();

				int maxchance = loadout.stream().mapToInt(ConfigFelsbrocken::getChance).sum();

				while( shipcount < cfs.getCount() && maxchance > 0)
				{
					int rnd = ThreadLocalRandom.current().nextInt(1, maxchance);
					int currnd = 0;
					for( ConfigFelsbrocken aloadout : loadout )
					{
						currnd += aloadout.getChance();

						if( currnd < rnd )
						{
							continue;
						}

						StarSystem thissystem = cfs.getSystem();

						// Koords ermitteln
						int x = ThreadLocalRandom.current().nextInt(1, thissystem.getWidth()+1);
						int y = ThreadLocalRandom.current().nextInt(1, thissystem.getHeight()+1);

						shouldId = (Integer)db.createSQLQuery("select newIntelliShipId( ? )")
							.setInteger(0, ++shouldId)
							.uniqueResult();

						this.log("\t*System "+cfs.getSystem().getID()+": Fuege "+cfs.getName()+" "+shouldId+" ein");

						// Ladung einfuegen
						this.log("\t- Loadout: ");
						Cargo cargo = aloadout.getCargo();
						ResourceList reslist = cargo.getResourceList();
						for( ResourceEntry res : reslist )
						{
							this.log("\t   *"+res.getPlainName()+" => "+res.getCount1());
						}

						ShipType shiptype = aloadout.getShiptype();

						Ship brocken = new Ship(owner, shiptype, cfs.getSystem().getID(), x, y);
						brocken.getHistory().addHistory("Indienststellung als "+cfs.getName()+" am "+currentTime+" durch den Tick");
						brocken.setName(cfs.getName() == null ? "Felsbrocken" : cfs.getName());
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
						db.save(brocken);

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
			for (Task aTasklist : tasklist)
			{
				this.log("* " + aTasklist.getTaskID() + " (" + aTasklist.getType() + ") -> sending tick_timeout");
				taskmanager.handleTask(aTasklist.getTaskID(), "tick_timeout");
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
			ConfigValue value = new ConfigService().get(WellKnownConfigValue.TICKS);
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
		this.doTasks();
	}
}
