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
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.services.BattleService;
import net.driftingsouls.ds2.server.services.LocationService;
import net.driftingsouls.ds2.server.services.PmService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import net.driftingsouls.ds2.server.tick.TickController;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Berechnet sonstige Tick-Aktionen, welche keinen eigenen TickController haben.
 * @author Christopher Jung
 *
 */
@Service("regularRestTick")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestTick extends TickController {

	private final ConfigService configService;
	private final BattleService battleService;
	private final PmService pmService;
	private final BBCodeParser bbCodeParser;
	private final LocationService locationService;
	private final TaskManager taskManager;

	@PersistenceContext
	private EntityManager em;

	public RestTick(ConfigService configService, BattleService battleService, PmService pmService, BBCodeParser bbCodeParser, LocationService locationService, TaskManager taskManager) {
		this.configService = configService;
		this.battleService = battleService;
		this.pmService = pmService;
		this.bbCodeParser = bbCodeParser;
		this.locationService = locationService;
		this.taskManager = taskManager;
	}

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
		try
		{
			this.log("Sprungantrieb");
			List<Jump> jumps = em.createQuery("from Jump as j inner join fetch j.ship", Jump.class).getResultList();
			for (Jump jump: jumps)
			{
				this.log(jump.getShip().getId() + " springt nach " + jump.getSystem() + ":" + jump.getX() + "/" + jump.getY());

				jump.getShip().setLocation(jump);

				em.createQuery("update Ship set x= :x, y= :y, system= :system where docked in (:dock,:land)")
						.setParameter("x", jump.getX())
						.setParameter("y", jump.getY())
						.setParameter("system", jump.getSystem())
						.setParameter("dock", Integer.toString(jump.getShip().getId()))
						.setParameter("land", "l " + jump.getShip().getId())
						.executeUpdate();

				em.remove(jump);
			}
		}
		catch( RuntimeException e )
		{
			this.log("Fehler beim Verarbeiten der Sprungantriebe: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doJumps failed");
		}
	}

	/*
		Statistiken
	*/
	private void doStatistics()
	{
		try
		{
			this.log("");
			this.log("Erstelle Statistiken");

			long shipCount = em.createQuery("select count(*) from Ship where id>0 and owner.id>0", Long.class).getSingleResult();
			long crewCount = em.createQuery("select sum(crew) from Ship where id>0 and owner.id>0", Long.class).getSingleResult();

			StatShips stat = new StatShips(configService.getValue(WellKnownConfigValue.TICKS), shipCount, crewCount);
			em.persist(stat);
		}
		catch( RuntimeException e )
		{
			this.log("Fehler beim Anlegen der Statistiken: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doStatistics failed");
		}
	}

	/*
		Vac-Modus
	*/
	private void doVacation()
	{
		try
		{
			this.log("");
			this.log("Bearbeite Vacation-Modus");

			List<User> vacLeaveUsers = em.createQuery("from User where vaccount=1", User.class).getResultList();
			for (User user : vacLeaveUsers)
			{
				user.setName(user.getName().replace(" [VAC]", ""));
				user.setPlainname(bbCodeParser.parse(user.getName().replace(" [VAC]", ""), new String[] {"all"}));
				user.setNickname(user.getNickname().replace(" [VAC]", ""));

				this.log("\t" + user.getPlainname() + " (" + user.getId() + ") verlaesst den VAC-Modus");
			}

			em.createQuery("update User set vaccount=vaccount-1 where vaccount>0 and wait4vac=0")
				.executeUpdate();

			List<User> users = em.createQuery("from User where wait4vac=1", User.class).getResultList();
			for( User user : users )
			{
				User newcommander = null;
				if( user.getAlly() != null )
				{
					newcommander = em.createQuery("from User where ally= :ally  and inakt <= 7 and vaccount=0 and (wait4vac>6 or wait4vac=0)", User.class)
						.setParameter("ally", user.getAlly())
						.setMaxResults(1)
						.getSingleResult();
				}

				List<Battle> battles = em.createQuery("from Battle where commander1= :user or commander2= :user", Battle.class)
					.setParameter("user", user)
					.getResultList();
				for (Battle battle: battles)
				{
					battleService.load(battle, user, null, null, 0);

					if (newcommander != null)
					{
						this.log("\t\tUser" + user.getId() + ": Die Leitung der Schlacht " + battle.getId() + " wurde an " + newcommander.getName() + " (" + newcommander.getId() + ") uebergeben");

						pmService.send(user, newcommander.getId(), "Schlacht &uuml;bernommen", "Die Leitung der Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " wurde dir automatisch &uuml;bergeben, da der bisherige Kommandant in den Vacationmodus gewechselt ist");

						battleService.log(battle, new SchlachtLogAktion(battle.getOwnSide(), Common._titleNoFormat(bbCodeParser, newcommander.getName()) + " kommandiert nun die Truppen"));

						battle.setCommander(battle.getOwnSide(), newcommander);

						battleService.log(battle, new SchlachtLogKommandantWechselt(battle.getOwnSide(), battle.getCommander(battle.getOwnSide())));

						battle.setTakeCommand(battle.getOwnSide(), 0);
					}
					else
					{
						this.log("\t\tUser" + user.getId() + ": Die Schlacht " + battle.getId() + " wurde beendet");

						battleService.endBattle(battle,0, 0);
						pmService.send(battle.getCommander(battle.getOwnSide()), battle.getCommander(battle.getEnemySide()).getId(), "Schlacht beendet", "Die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " wurde automatisch beim wechseln in den Vacation-Modus beendet, da kein Ersatzkommandant ermittelt werden konnte!");
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
				user.setPlainname(bbCodeParser.parse(name+" [VAC]", new String[] {"all"}));

				this.log("\t"+user.getPlainname()+" ("+user.getId()+") ist in den VAC-Modus gewechselt");
			}


			em.createQuery("update User set wait4vac=wait4vac-1 where wait4vac>0")
				.executeUpdate();
		}
		catch( RuntimeException e )
		{
			this.log("Fehler beim Verarbeiten der Vacationdaten: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doVacation failed");
		}
	}

	/*
	 * Unset Noob Protection for experienced players
	 */
	private void doNoobProtection()
	{
		try
		{
			this.log("");
			this.log("Bearbeite Noob-Protection");

			List<User> noobUsers = em.createQuery("from User where id>0 and flags LIKE '%" + UserFlag.NOOB.getFlag()+"%'", User.class).getResultList();
			long noobDays = 30;
			long noobTime = TimeUnit.DAYS.toSeconds(noobDays);
			for (User user: noobUsers)
			{
				if (!user.hasFlag(UserFlag.NOOB))
				{
					continue;
				}

				if (user.getSignup() <= Common.time() - noobTime)
				{
					user.setFlag(UserFlag.NOOB, false);
					this.log("Entferne Noob-Schutz bei " + user.getId());

					User nullUser = em.find(User.class, 0);
					pmService.send(nullUser, user.getId(), "GCP-Schutz aufgehoben",
							"Ihr GCP-Schutz wurde durch das System aufgehoben. " +
									"Dies passiert automatisch " + noobDays + " Tage nach der Registrierung. " +
									"Sie sind nun angreifbar, können aber auch selbst angreifen.",
							PM.FLAGS_AUTOMATIC | PM.FLAGS_IMPORTANT
					);
				}
			}
		}
		catch(RuntimeException e)
		{
			this.log("Fehler beim Aufheben des Noobschutzes: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doNoobProtecttion failed");
		}
	}

	/*

		Neue Felsbrocken spawnen lassen

	*/
	private void doFelsbrocken()
	{
		try
		{
			User owner = em.find(User.class, -1);
			String currentTime = Common.getIngameTime(new ConfigService().getValue(WellKnownConfigValue.TICKS));

			this.log("");
			this.log("Fuege Felsbrocken ein");

			int shouldId = 9999;

			List<ConfigFelsbrockenSystem> systemList = em.createQuery("from ConfigFelsbrockenSystem cfs", ConfigFelsbrockenSystem.class)
				.getResultList();
			for( ConfigFelsbrockenSystem cfs: systemList )
			{
				long shipcount = em.createQuery("select count(*) " +
						"from Ship s " +
						"where s.system=:system and " +
							"s.shiptype in (select shiptype from ConfigFelsbrocken where system=:cfs)", Long.class)
					.setParameter("system", cfs.getSystem().getID())
					.setParameter("cfs", cfs)
					.getSingleResult();

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

						shouldId = (Integer)em.createNativeQuery("select newIntelliShipId(:shouldId)", Integer.class)
							.setParameter("shouldId", ++shouldId)
							.getSingleResult();

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
						em.persist(brocken);

						this.log("");

						shipcount++;

						break;
					}
				}
			}
		}
		catch( RuntimeException e )
		{
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
		try
		{
			this.log("Bearbeite Tasks [tick_timeout]");

			Task[] tasklist = taskManager.getTasksByTimeout(1);
			for (Task aTasklist : tasklist)
			{
				this.log("* " + aTasklist.getTaskID() + " (" + aTasklist.getType() + ") -> sending tick_timeout");
				taskManager.handleTask(aTasklist.getTaskID(), "tick_timeout");
			}

			taskManager.reduceTimeout(1);
		}
		catch( RuntimeException e )
		{
			this.log("Fehler beim Bearbeiten der Tasks: "+e);
			e.printStackTrace();
			Common.mailThrowable(e, "RestTick Exception", "doTasks failed");
		}
	}

	@Override
	protected void tick()
	{
		this.log("Transmissionen - gelesen+1");
		em.createQuery("UPDATE PM SET gelesen = gelesen+1 WHERE gelesen>=2").executeUpdate();

		this.log("Loesche alte Transmissionen");
		em.createQuery("DELETE FROM PM WHERE gelesen>=10").executeUpdate();

		this.log("Erhoehe Inaktivitaet der Spieler");
		em.createQuery("update User set inakt=inakt+1 where vaccount=0")
			.executeUpdate();

		this.log("Erhoehe Tickzahl");
		ConfigValue value = configService.get(WellKnownConfigValue.TICKS);
		int ticks = Integer.parseInt(value.getValue()) + 1;
		value.setValue(Integer.toString(ticks));

		this.doJumps();
		this.doStatistics();
		this.doVacation();
		this.doNoobProtection();
		this.doFelsbrocken();
		this.doTasks();
	}
}
