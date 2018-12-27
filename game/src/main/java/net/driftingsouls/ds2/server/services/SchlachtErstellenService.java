package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleFlag;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLog;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.SchlachtLogKommandantWechselt;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SchlachtErstellenService
{
	private static final Logger LOG = LogManager.getLogger(SchlachtErstellenService.class);

	/**
	 * Erstellt eine neue Schlacht.
	 * @param user Der Spieler, der die Schlacht beginnt
	 * @param ownShipID Die ID des Schiffes des Spielers, der angreift
	 * @param enemyShipID Die ID des angegriffenen Schiffes
	 * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte. Andernfalls <code>null</code>
	 */
	public Battle erstelle( User user, int ownShipID, int enemyShipID ) {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		return erstelle(user, (Ship)db.get(Ship.class, ownShipID), (Ship)db.get(Ship.class,enemyShipID), false);
	}

	/**
	 * Erstellt eine neue Schlacht.
	 * @param user Der Spieler, der die Schlacht beginnt
	 * @param ownShip Das Schiff des des Spielers, der angreift
	 * @param enemyShip Das angegriffene Schiffes
	 * @param startOwn <code>true</code>, falls eigene gelandete Schiffe starten sollen
	 * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte
	 * @throws java.lang.IllegalArgumentException Falls mit den uebergebenen Parametern keine Schlacht erstellt werden kann
	 */
	public Battle erstelle(@Nonnull User user, @Nonnull Ship ownShip, @Nonnull Ship enemyShip, final boolean startOwn ) throws IllegalArgumentException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		LOG.info("battle: "+user+" :: "+ownShip.getId()+" :: "+enemyShip.getId());

		Ship tmpEnemyShip = enemyShip;
		User enemyUser = tmpEnemyShip.getOwner();

		checkBattleConditions(user, enemyUser, ownShip, tmpEnemyShip);

		//
		// Schiffsliste zusammenstellen
		//
		BattleShip enemyBattleShip = null;
		BattleShip ownBattleShip = null;

		Set<User> ownUsers = new HashSet<>();
		Set<User> enemyUsers = new HashSet<>();

		Ally ownAlly = ownShip.getOwner().getAlly();
		Ally enemyAlly = tmpEnemyShip.getOwner().getAlly();

		Query shipQuery = db.createQuery("from Ship as s inner join fetch s.owner as u " +
				"where s.id>:minid and s.x=:x and s.y=:y and " +
				"s.system=:system and s.battle is null and (" +
				(ownAlly == null ? "u.ally is null" : "u.ally=:ally1")+" or "+
				(enemyAlly == null ? "u.ally is null" : "u.ally=:ally2")+
				") and locate('disable_iff',s.status)=0 and (u.vaccount=0 or u.wait4vac > 0)")
				.setInteger("minid", 0)
				.setInteger("x", ownShip.getX())
				.setInteger("y", ownShip.getY())
				.setInteger("system", ownShip.getSystem());
		if( ownAlly != null )
		{
			shipQuery.setParameter("ally1", ownAlly);
		}
		if( enemyAlly != null )
		{
			shipQuery.setParameter("ally2", enemyAlly);
		}

		List<Ship> shiplist = Common.cast(shipQuery.list());

		Set<BattleShip> ownShips = new HashSet<>();
		Set<BattleShip> enemyShips = new HashSet<>();
		Set<BattleShip> secondRowShips = new HashSet<>();
		boolean firstRowExists = false;
		boolean firstRowEnemyExists = false;

		for (Ship aShip : shiplist) {
			// Loot-Truemmer sollten in keine Schlacht wandern... (nicht schoen, gar nicht schoen geloest)
			if ((aShip.getOwner().getId() == -1) && (aShip.getType() == new ConfigService().getValue(WellKnownConfigValue.TRUEMMER_SHIPTYPE))) {
				continue;
			}
			User tmpUser = aShip.getOwner();

			if (tmpUser.isNoob()) {
				continue;
			}

			BattleShip battleShip = new BattleShip(null, aShip);

			ShipTypeData shiptype = aShip.getBaseType();


			boolean ownShipFound = false;
			if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ) && (aShip.isDocked() || aShip.isLanded())) {
				battleShip.addFlag(BattleShipFlag.DISABLE_WEAPONS);
			}

			if (((ownAlly != null) && (tmpUser.getAlly() == ownAlly)) || (user.getId() == tmpUser.getId())) {
				ownUsers.add(tmpUser);
				battleShip.setSide(0);
				ownShipFound = true;

				if (aShip == ownShip) {
					ownBattleShip = battleShip;
				}
				else
				{
					ownShips.add(battleShip);
				}
			}
			else if (((enemyAlly != null) && (tmpUser.getAlly() == enemyAlly)) || (tmpEnemyShip.getOwner().getId() == tmpUser.getId())) {
				enemyUsers.add(tmpUser);
				battleShip.setSide(1);

				if (aShip == enemyShip) {
					enemyBattleShip = battleShip;
				}
				else
				{
					enemyShips.add(battleShip);
				}
			}

			if (shiptype.hasFlag(ShipTypeFlag.SECONDROW) && aShip.getEinstellungen().gotoSecondrow()) {
				secondRowShips.add(battleShip);
			}
			else
			{
				if(ownShipFound)
				{
					firstRowExists = true;
				}
				else
				{
					firstRowEnemyExists = true;
				}
			}
		}


		//
		// Schauen wir mal ob wir was sinnvolles aus der DB gelesen haben
		// - Wenn nicht: Abbrechen
		//

		if( ownBattleShip == null ) {
			throw new IllegalArgumentException("Offenbar liegt ein Problem mit dem von ihnen angegebenen Schiff oder ihrem eigenen Schiff vor (wird es evt. bereits angegriffen?)");
		}

		Battle battle = new Battle(ownShip.getLocation());
		battle.getOwnShips().addAll(ownShips);
		battle.getEnemyShips().addAll(enemyShips);

		addToSecondRow(battle, secondRowShips, firstRowExists, firstRowEnemyExists);

		if( enemyBattleShip == null && (battle.getEnemyShips().size() == 0) ) {
			throw new IllegalArgumentException("Offenbar liegt ein Problem mit den feindlichen Schiffen vor. Es gibt nämlich keine die angegriffen werden könnten.");
		}
		else if( enemyBattleShip == null ) {
			throw new IllegalArgumentException("Offenbar liegt ein Problem mit den feindlichen Schiffen vor. Es gibt zwar welche, jedoch fehlt das Zielschiff.");
		}

		//
		// Schlacht in die DB einfuegen
		//
		battle.setAlly(0, ownBattleShip.getOwner().getAlly() != null ? ownBattleShip.getOwner().getAlly().getId() : 0);
		battle.setAlly(1, enemyBattleShip.getOwner().getAlly() != null ? enemyBattleShip.getOwner().getAlly().getId() : 0);
		battle.setCommander(0, ownBattleShip.getOwner());
		battle.setCommander(1, enemyBattleShip.getOwner());
		battle.setFlag(BattleFlag.FIRSTROUND);
		db.save(battle);

		//
		// Schiffe in die Schlacht einfuegen
		//

		int tick = context.get(ContextCommon.class).getTick();

		// * Gegnerische Schiffe in die Schlacht einfuegen
		List<Integer> idlist = new ArrayList<>();
		List<Integer> startlist = new ArrayList<>();
		enemyShip = enemyBattleShip.getShip();
		if( enemyBattleShip.getShip().isLanded() )
		{
			enemyShip.getBaseShip().start(enemyShip);
			startlist.add(enemyBattleShip.getId());
		}
		idlist.add(enemyBattleShip.getId());

		enemyBattleShip.setBattle(battle);
		db.persist(enemyBattleShip);

		enemyShip.setBattle(battle);

		insertShipsIntoDatabase(battle, battle.getEnemyShips(), startlist, idlist);
		if( startlist.size() > 0 ) {
			battle.logme(startlist.size() + " J&auml;ger sind automatisch gestartet\n");
			battle.log(new SchlachtLogAktion(1, startlist.size() + " J&auml;ger sind automatisch gestartet"));

			startlist.clear();
		}

		startlist = new ArrayList<>();
		battle.getEnemyShips().add(enemyBattleShip);
		battle.setEnemyShipIndex(battle.getEnemyShips().size()-1);

		// * Eigene Schiffe in die Schlacht einfuegen
		idlist.add(ownBattleShip.getId());

		ownBattleShip.setBattle(battle);
		db.persist(ownBattleShip);

		ownShip.setBattle(battle);
		if(ownBattleShip.getShip().isLanded())
		{
			//TODO: Maybe we could optimize this a little bit further with mass start?
			ownShip.getBaseShip().start(enemyShip);
			startlist.add(enemyBattleShip.getId());
		}
		ownShip.setDocked("");

		insertShipsIntoDatabase(battle, battle.getOwnShips(), startlist, idlist);
		if( startOwn && startlist.size() > 0 ) {
			battle.logme(startlist.size() + " J&auml;ger sind automatisch gestartet\n");
			battle.log(new SchlachtLogAktion(0, startlist.size() + " Jäger sind automatisch gestartet"));
		}
		battle.getOwnShips().add(ownBattleShip);
		battle.setFiringShip(ownBattleShip.getShip());

		//
		// Log erstellen
		//
		createBattleLog(db, battle, ownBattleShip, enemyBattleShip, tick);

		//
		// Beziehungen aktualisieren
		//

		// Zuerst schauen wir mal ob wir es mit Allys zu tun haben und
		// berechnen ggf die Userlisten neu
		Set<Integer> calcedallys = new HashSet<>();

		db.setFlushMode(FlushMode.COMMIT);

		for( User auser : new ArrayList<>(ownUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId()) ) {
				List<User> allyusers = Common.cast(db.createQuery("from User u where u.ally=:ally and (u not in (:ownUsers))")
						.setEntity("ally", auser.getAlly())
						.setParameterList("ownUsers", ownUsers)
						.list());

				ownUsers.addAll(allyusers);
				calcedallys.add(auser.getAlly().getId());
			}
		}

		for( User auser : new ArrayList<>(enemyUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId()) ) {
				List<User> allyusers = Common.cast(db.createQuery("from User u where ally=:ally and (u not in (:enemyUsers))")
						.setEntity("ally", auser.getAlly())
						.setParameterList("enemyUsers", enemyUsers)
						.list());
				enemyUsers.addAll(allyusers);
				calcedallys.add(auser.getAlly().getId());
			}
		}

		for( User auser : ownUsers ) {
			for( User euser : enemyUsers ) {
				auser.setRelation(euser.getId(), User.Relation.ENEMY);
				euser.setRelation(auser.getId(), User.Relation.ENEMY);
			}
		}

		// PM Wegen Schlachteröffnung schicken, sofern die Spieler dies wollen.
		String eparty;
		String eparty2;
		if (battle.getAlly(0) == 0)
		{
			final User commander1 = battle.getCommander(0);
			eparty = commander1.getNickname();
		}
		else
		{
			final Ally ally = (Ally) db.get(Ally.class, battle.getAlly(0));
			eparty = ally.getName();
		}

		if (battle.getAlly(1) == 0)
		{
			final User commander2 = battle.getCommander(1);
			eparty2 = commander2.getNickname();
		}
		else
		{
			final Ally ally = (Ally) db.get(Ally.class, battle.getAlly(1));
			eparty2 = ally.getName();
		}
		User niemand = (User)db.get(User.class, -1);
		String msg = "Es wurde eine Schlacht bei "+ownShip.getLocation().displayCoordinates(false)+" eröffnet.\n" +
				"Es kämpfen "+eparty+" und "+eparty2+" gegeneinander.";
		for(User auser : ownUsers)
		{
			if(auser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM))
			{
				PM.send(niemand, auser.getId(), "Schlacht eröffnet", msg);
			}
		}
		for(User auser : enemyUsers)
		{
			if(auser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM))
			{
				PM.send(niemand, auser.getId(), "Schlacht eröffnet", msg);
			}
		}
		db.setFlushMode(FlushMode.AUTO);

		return battle;
	}

	private void createBattleLog(Session db, Battle battle, BattleShip ownBattleShip, BattleShip enemyBattleShip, int tick)
	{
		SchlachtLog log = new SchlachtLog(battle, tick);
		db.persist(log);
		battle.setSchlachtLog(log);

		battle.log(new SchlachtLogKommandantWechselt(0, ownBattleShip.getOwner()));
		battle.log(new SchlachtLogKommandantWechselt(1, enemyBattleShip.getOwner()));
	}

	private void insertShipsIntoDatabase(Battle battle, List<BattleShip> ships, List<Integer> startlist, List<Integer> idlist)
	{
		if( ships.isEmpty() ) {
			return;
		}

		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		for (BattleShip ship : ships) {
			Ship baseShip = ship.getShip().getBaseShip();
			if (baseShip != null && ship.getShip().isLanded() && baseShip.getEinstellungen().startFighters() && ship.getShip().getTypeData().getShipClass() == ShipClasses.JAEGER ) {
				ship.getShip().setDocked("");
				startlist.add(ship.getId());
			}
			idlist.add(ship.getId());
			ship.getShip().setBattle(battle);
			ship.setBattle(battle);
			db.persist(ship);
		}
	}

	private void checkBattleConditions(User user, User enemyUser, Ship ownShip, Ship enemyShip) throws IllegalArgumentException
	{
		// Kann der Spieler ueberhaupt angreifen (Noob-Schutz?)
		if (user.isNoob())
		{
			throw new IllegalArgumentException("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keinen Gegner angreifen!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
		}

		if ((ownShip == null) || (ownShip.getId() < 0) || (ownShip.getOwner() != user))
		{
			throw new IllegalArgumentException("Das angreifende Schiff existiert nicht oder untersteht nicht ihrem Kommando!");
		}

		if ((enemyShip == null) || (enemyShip.getId() < 0))
		{
			throw new IllegalArgumentException("Das angegebene Zielschiff existiert nicht!");
		}

		if (!ownShip.getLocation().sameSector(0, enemyShip.getLocation(), 0))
		{
			throw new IllegalArgumentException("Die beiden Schiffe befinden sich nicht im selben Sektor");
		}

		//
		// Kann der Spieler angegriffen werden (NOOB-Schutz?/Vac-Mode?)
		//

		if (enemyUser.isNoob())
		{
			throw new IllegalArgumentException("Der Gegner steht unter GCP-Schutz und kann daher nicht angegriffen werden!");
		}

		if (enemyUser.getVacationCount() != 0 && enemyUser.getWait4VacationCount() == 0)
		{
			throw new IllegalArgumentException("Der Gegner befindet sich im Vacation-Modus und kann daher nicht angegriffen werden!");
		}

		//
		// IFF-Stoersender?
		//
		boolean disable_iff = enemyShip.getStatus().contains("disable_iff");
		if (disable_iff)
		{
			throw new IllegalArgumentException("Dieses Schiff kann nicht angegriffen werden (egal wieviel du mit der URL rumspielt!)");
		}
	}

	/**
	 * Adds ships which have the second row flag to the second row.
	 * If there is no first row no ship will be added to the second row.
	 *
	 * @param battle Die momentan in der Erstellung befindliche Schlacht
	 * @param secondRowShips Ships to add.
	 * @param firstRowExists True, if side one has a first row.
	 * @param firstRowEnemyExists True, if side two has a first row.
	 */
	private void addToSecondRow(Battle battle, Set<BattleShip> secondRowShips, boolean firstRowExists, boolean firstRowEnemyExists)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		db.setFlushMode(FlushMode.COMMIT);

		Map<Ship,BattleShip> battleShipMap = new HashMap<>();
		for( BattleShip ship : battle.getOwnShips() )
		{
			battleShipMap.put(ship.getShip(), ship);
		}
		for( BattleShip ship : battle.getEnemyShips() )
		{
			battleShipMap.put(ship.getShip(), ship);
		}

		for(BattleShip ship: secondRowShips)
		{
			if( (ship.getSide() == 0 && firstRowExists && ship.getShip().getEinstellungen().gotoSecondrow() ) || (ship.getSide() == 1 && firstRowEnemyExists && ship.getShip().getEinstellungen().gotoSecondrow() ) )
			{
				ship.addFlag(BattleShipFlag.SECONDROW);
				if(ship.getTypeData().getJDocks() == 0)
				{
					continue;
				}

				List<Ship> landedShips = ship.getShip().getLandedShips();
				for(Ship landedShip: landedShips)
				{
					if(!landedShip.getTypeData().hasFlag(ShipTypeFlag.SECONDROW))
					{
						continue;
					}

					BattleShip aship = battleShipMap.get(landedShip);
					if(aship != null)
					{
						aship.addFlag(BattleShipFlag.SECONDROW);
					}
				}
			}
		}
		db.setFlushMode(FlushMode.AUTO);
	}

}
