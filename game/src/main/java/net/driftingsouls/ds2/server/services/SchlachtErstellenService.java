package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.battles.*;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.*;

@Service
@RequestScope
public class SchlachtErstellenService
{
	private static final Logger LOG = LogManager.getLogger(SchlachtErstellenService.class);

	private final EntityManager db;
	private final ConfigService configService;

    public SchlachtErstellenService(EntityManager db, ConfigService configService) {
        this.db = db;
		this.configService = configService;
    }

    /**
	 * Erstellt eine neue Schlacht.
	 * @param user Der Spieler, der die Schlacht beginnt
	 * @param ownShipID Die ID des Schiffes des Spielers, der angreift
	 * @param enemyShipID Die ID des angegriffenen Schiffes
	 * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte. Andernfalls <code>null</code>
	 */
	public Battle erstelle( User user, int ownShipID, int enemyShipID ) {
		return erstelle(user, db.find(Ship.class, ownShipID), db.find(Ship.class,enemyShipID), false);
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

		var shipQuery = db.createQuery("from Ship as s inner join fetch s.owner as u " +
				"where s.id>:minid and s.x=:x and s.y=:y and " +
				"s.system=:system and s.battle is null and (" +
				(ownAlly == null ? "u.ally is null" : "u.ally=:ally1")+" or "+
				(enemyAlly == null ? "u.ally is null" : "u.ally=:ally2")+
				") and locate('disable_iff',s.status)=0 and (u.vaccount=0 or u.wait4vac > 0)", Ship.class)
				.setParameter("minid", 0)
				.setParameter("x", ownShip.getX())
				.setParameter("y", ownShip.getY())
				.setParameter("system", ownShip.getSystem());
		if( ownAlly != null )
		{
			shipQuery.setParameter("ally1", ownAlly);
		}
		if( enemyAlly != null )
		{
			shipQuery.setParameter("ally2", enemyAlly);
		}

		List<Ship> shiplist = shipQuery.getResultList();

		Set<BattleShip> ownShips = new HashSet<>();
		Set<BattleShip> enemyShips = new HashSet<>();
		Set<BattleShip> secondRowShips = new HashSet<>();
		boolean firstRowExists = false;
		boolean firstRowEnemyExists = false;

		for (Ship aShip : shiplist) {
			// Loot-Truemmer sollten in keine Schlacht wandern... (nicht schoen, gar nicht schoen geloest)
			if ((aShip.getOwner().getId() == -1) && (aShip.getType() == configService.getValue(WellKnownConfigValue.TRUEMMER_SHIPTYPE))) {
				continue;
			}
			User tmpUser = aShip.getOwner();

			if (tmpUser.isNoob()) {
				continue;
			}

			//gedockte + gelandete Schiffe werden ueber ihr BaseShip in die Schlacht gejoint
			//andernfalls koennen ueber Flotten Schiffe der Schlacht joinen, welche auf einem Traeger ausserhalb sind.
			if(aShip.isDocked() || aShip.isLanded()){
				continue;
			}

			BattleShip battleShip = new BattleShip(null, aShip);

			ShipTypeData shiptype = aShip.getBaseType();


			boolean ownShipFound = false;


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

			//sollten auf dem Schiff Schiffe gedockt / gelandet sein, joinen diese nun ebenfalls der Schlacht
			for(Ship lShip : aShip.getGedockteUndGelandeteSchiffe()){
				BattleShip blShip = new BattleShip(null, lShip);
				ShipTypeData lshiptype = lShip.getBaseType();

				//natuerlich kommt das gedockte Schiff auf die selbe Seite und in die selbe Reihe wie sein Traeger
				blShip.setSide(battleShip.getSide());
				if(ownShips.contains(battleShip)){
					ownShips.add(blShip);
				}
				else{
					enemyShips.add(blShip);
				}
				if(secondRowShips.contains(battleShip)){
					secondRowShips.add(blShip);
				}
				//Geschuetze deaktivieren
				if ((lshiptype.getShipClass() == ShipClasses.GESCHUETZ) ) {
					blShip.addFlag(BattleShipFlag.DISABLE_WEAPONS);
				}

			}
		}


		//
		// Schauen wir mal ob wir was sinnvolles aus der DB gelesen haben
		// - Wenn nicht: Abbrechen
		//

		if( ownBattleShip == null ) {
			throw new IllegalArgumentException("Offenbar liegt ein Problem mit dem von Ihnen angegebenen Schiff oder Ihrem eigenen Schiff vor (wird es evtl. bereits angegriffen?).");
		}

		Battle battle = new Battle(ownShip.getLocation());
		battle.getOwnShips().addAll(ownShips);
		battle.getEnemyShips().addAll(enemyShips);

		addToSecondRow(battle, secondRowShips, firstRowExists, firstRowEnemyExists);

		if( enemyBattleShip == null && battle.getEnemyShips().isEmpty() ) {
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
		db.persist(battle);

		//
		// Schiffe in die Schlacht einfuegen
		//

		int tick = ContextMap.getContext().get(ContextCommon.class).getTick();

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
		if(!startlist.isEmpty()) {
			battle.logme(startlist.size() + " Jäger sind automatisch gestartet\n");
			battle.log(new SchlachtLogAktion(1, startlist.size() + " Jäger sind automatisch gestartet"));

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
		if( startOwn && !startlist.isEmpty()) {
			battle.logme(startlist.size() + " Jäger sind automatisch gestartet\n");
			battle.log(new SchlachtLogAktion(0, startlist.size() + " Jäger sind automatisch gestartet"));
		}
		battle.getOwnShips().add(ownBattleShip);
		battle.setFiringShip(ownBattleShip.getShip());

		//
		// Log erstellen
		//
		createBattleLog(battle, ownBattleShip, enemyBattleShip, tick);

		//
		// Beziehungen aktualisieren
		//

		// Zuerst schauen wir mal ob wir es mit Allys zu tun haben und
		// berechnen ggf die Userlisten neu
		Set<Integer> calcedallys = new HashSet<>();

		for( User auser : new ArrayList<>(ownUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId()) ) {
				List<User> allyusers = db.createQuery("from User u where u.ally=:ally and (u not in (:ownUsers))", User.class)
						.setParameter("ally", auser.getAlly())
						.setParameter("ownUsers", ownUsers)
						.getResultList();

				ownUsers.addAll(allyusers);
				calcedallys.add(auser.getAlly().getId());
			}
		}

		for( User auser : new ArrayList<>(enemyUsers) ) {
			if( (auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId()) ) {
				List<User> allyusers = db.createQuery("from User u where ally=:ally and (u not in (:enemyUsers))", User.class)
						.setParameter("ally", auser.getAlly())
						.setParameter("enemyUsers", enemyUsers)
						.getResultList();
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
			final Ally ally = db.find(Ally.class, battle.getAlly(0));
			eparty = ally.getName();
		}

		if (battle.getAlly(1) == 0)
		{
			final User commander2 = battle.getCommander(1);
			eparty2 = commander2.getNickname();
		}
		else
		{
			final Ally ally = db.find(Ally.class, battle.getAlly(1));
			eparty2 = ally.getName();
		}
		User niemand = db.find(User.class, -1);
		String msg = "Es wurde eine Schlacht bei "+ownShip.getLocation().displayCoordinates(false)+" eröffnet.\n" +
				"Es kämpfen "+eparty+" ("+ battle.getOwnShips().size() +" Schiffe) und "+eparty2+" ("+ battle.getEnemyShips().size() +" Schiffe) gegeneinander. "+
				"Deine 2. Reihe ist ";
		String msg1 = "";
		String msg2 = "";
		if (battle.isSecondRowStable(0))
		{
			msg1 +="stabil.";
		}
		else
		{
			msg1 += "instabil. Vorsicht!";
		}
		if (battle.isSecondRowStable(1))
		{
			msg2 +="stabil.";
		}
		else
		{
			msg2 += "instabil. Vorsicht!";
		}
		for(User auser : ownUsers)
		{
			if(auser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM))
			{
				PM.send(niemand, auser.getId(), "Schlacht eröffnet", msg+msg1, db);
			/*	if(auser.getApiKey()!="")
				{
					new Notifier (auser.getApiKey()).sendMessage("Schlacht bei "+ownShip.getLocation().displayCoordinates(false)+" eröffnet", msg);
				}
			*/
			}

		}
		for(User auser : enemyUsers)
		{
			if(auser.getUserValue(WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM))
			{
				PM.send(niemand, auser.getId(), "Schlacht eröffnet", msg+msg2, db);
			/*	if(auser.getApiKey()!="")
				{
					new Notifier (auser.getApiKey()).sendMessage("Schlacht bei "+ownShip.getLocation().displayCoordinates(false)+" eröffnet", msg);
				}
			*/
			}

		}

		return battle;
	}

	private void createBattleLog(Battle battle, BattleShip ownBattleShip, BattleShip enemyBattleShip, int tick)
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

		for (BattleShip ship : ships) {
			Ship baseShip = ship.getShip().getBaseShip();
			if (baseShip != null && ship.getShip().isLanded() && baseShip.getEinstellungen().startFighters() && ship.getShip().getTypeData().getShipClass() == ShipClasses.JAEGER ) {
				ship.getShip().setDocked("");
				if(!ship.getShip().getTypeData().hasFlag(ShipTypeFlag.SECONDROW))
				{
					ship.removeFlag(BattleShipFlag.SECONDROW);
				}
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
			throw new IllegalArgumentException("Sie stehen unter GCP-Schutz (Neulingsschutz) und k&ouml;nnen daher keinen Gegner angreifen!<br />Hinweis: Der GCP-Schutz kann in den Optionen vorzeitig beendet werden");
		}

		if ((ownShip == null) || (ownShip.getId() < 0) || (ownShip.getOwner() != user))
		{
			throw new IllegalArgumentException("Das angreifende Schiff existiert nicht oder untersteht nicht Ihrem Kommando!");
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
			throw new IllegalArgumentException("Der Gegner steht unter GCP-Schutz (Neulingsschutz) und kann daher nicht angegriffen werden!");
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
			throw new IllegalArgumentException("Dieses Schiff kann nicht angegriffen werden (egal wieviel Du mit der URL rumspielst!)");
		}
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		StarSystem system = (StarSystem) db.get(StarSystem.class, ownShip.getSystem());
		if(!system.isBattleAllowed()){
			throw new IllegalArgumentException("In diesem System sind Kämpfe untersagt.");
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
			if( (ship.getSide() == 0 && firstRowExists && ship.getShip().getEinstellungen().gotoSecondrow() ) || (ship.getSide() == 1 && firstRowEnemyExists && ship.getShip().getEinstellungen().gotoSecondrow() ))
			{
				ship.addFlag(BattleShipFlag.SECONDROW);
				if(ship.getTypeData().getJDocks() > 0)
				{
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
				if( ship.getTypeData().getADocks() == 0)
				{
					List<Ship> dockedShips = ship.getShip().getDockedShips();
					for(Ship dockedShip: dockedShips)
					{

						BattleShip aship = battleShipMap.get(dockedShip);
						if(aship != null)
						{
							aship.addFlag(BattleShipFlag.SECONDROW);
						}
					}
				}
			}
		}
	}

}
