package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.battles.BattleFlag;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.battles.BattleShipFlag;
import net.driftingsouls.ds2.server.battles.SchlachtLog;
import net.driftingsouls.ds2.server.battles.SchlachtLogAktion;
import net.driftingsouls.ds2.server.battles.SchlachtLogEintrag;
import net.driftingsouls.ds2.server.battles.SchlachtLogKommandantWechselt;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.WellKnownUserValue;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipLost;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BattleService {
    private static final Logger LOG = LogManager.getLogger(BattleService.class);
    private final UserService userService;
    private final ShipService shipService;
    private final PmService pmService;
    private final BBCodeParser bbCodeParser;
    private final UserValueService userValueService;
    private final ConfigService configService;
    private final LocationService locationService;
    private final DismantlingService dismantlingService;
    private final ShipActionService shipActionService;

    @PersistenceContext
    private EntityManager em;


    public BattleService(UserService userService, ShipService shipService, PmService pmService, BBCodeParser bbCodeParser, UserValueService userValueService, ConfigService configService, LocationService locationService, DismantlingService dismantlingService, ShipActionService shipActionService) {
        this.userService = userService;
        this.shipService = shipService;
        this.pmService = pmService;
        this.bbCodeParser = bbCodeParser;
        this.userValueService = userValueService;
        this.configService = configService;
        this.locationService = locationService;
        this.dismantlingService = dismantlingService;
        this.shipActionService = shipActionService;
    }

    /**
     * Erstellt eine neue Schlacht.
     *
     * @param user        Der Spieler, der die Schlacht beginnt
     * @param ownShipID   Die ID des Schiffes des Spielers, der angreift
     * @param enemyShipID Die ID des angegriffenen Schiffes
     * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte. Andernfalls <code>null</code>
     */
    public Battle erstelle(User user, int ownShipID, int enemyShipID) {
        return erstelle(user, em.find(Ship.class, ownShipID), em.find(Ship.class, enemyShipID), false);
    }

    /**
     * Erstellt eine neue Schlacht.
     *
     * @param user      Der Spieler, der die Schlacht beginnt
     * @param ownShip   Das Schiff des des Spielers, der angreift
     * @param enemyShip Das angegriffene Schiffes
     * @param startOwn  <code>true</code>, falls eigene gelandete Schiffe starten sollen
     * @return Die Schlacht, falls sie erfolgreich erstellt werden konnte
     * @throws java.lang.IllegalArgumentException Falls mit den uebergebenen Parametern keine Schlacht erstellt werden kann
     */
    public Battle erstelle(@NonNull User user, @NonNull Ship ownShip, @NonNull Ship enemyShip, final boolean startOwn) {
        LOG.info("battle: " + user + " :: " + ownShip.getId() + " :: " + enemyShip.getId());

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

        TypedQuery<Ship> shipQuery = em.createQuery("from Ship as s inner join fetch s.owner as u " +
            "where s.id>:minid and s.x=:x and s.y=:y and " +
            "s.system=:system and s.battle is null and (" +
            (ownAlly == null ? "u.ally is null" : "u.ally=:ally1") + " or " +
            (enemyAlly == null ? "u.ally is null" : "u.ally=:ally2") +
            ") and locate('disable_iff',s.status)=0 and (u.vaccount=0 or u.wait4vac > 0)", Ship.class)
            .setParameter("minid", 0)
            .setParameter("x", ownShip.getX())
            .setParameter("y", ownShip.getY())
            .setParameter("system", ownShip.getSystem());
        if (ownAlly != null) {
            shipQuery.setParameter("ally1", ownAlly);
        }
        if (enemyAlly != null) {
            shipQuery.setParameter("ally2", enemyAlly);
        }

        List<Ship> ships = shipQuery.getResultList();

        Set<BattleShip> ownShips = new HashSet<>();
        Set<BattleShip> enemyShips = new HashSet<>();
        Set<BattleShip> secondRowShips = new HashSet<>();
        boolean firstRowExists = false;
        boolean firstRowEnemyExists = false;

        for (Ship aShip : ships) {
            // Loot-Truemmer sollten in keine Schlacht wandern... (nicht schoen, gar nicht schoen geloest)
            if ((aShip.getOwner().getId() == -1) && (aShip.getType() == new ConfigService().getValue(WellKnownConfigValue.TRUEMMER_SHIPTYPE))) {
                continue;
            }
            User tmpUser = aShip.getOwner();

            if (userService.isNoob(tmpUser)) {
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
                } else {
                    ownShips.add(battleShip);
                }
            } else if (((enemyAlly != null) && (tmpUser.getAlly() == enemyAlly)) || (tmpEnemyShip.getOwner().getId() == tmpUser.getId())) {
                enemyUsers.add(tmpUser);
                battleShip.setSide(1);

                if (aShip == enemyShip) {
                    enemyBattleShip = battleShip;
                } else {
                    enemyShips.add(battleShip);
                }
            }

            if (shiptype.hasFlag(ShipTypeFlag.SECONDROW) && aShip.getEinstellungen().gotoSecondrow()) {
                secondRowShips.add(battleShip);
            }
            //ist Schiff gedockt und Traeger Reihe 2, dann Schiff auch Reihe 2
            else if (aShip.isDocked()) {
                Ship traeger = shipService.getBaseShip(aShip);
                //Traegertschiff ebenso abfragen wie oben
                if (traeger.getEinstellungen().gotoSecondrow() && traeger.getBaseType().hasFlag(ShipTypeFlag.SECONDROW)) {
                    secondRowShips.add(battleShip);
                }
            } else {
                if (ownShipFound) {
                    firstRowExists = true;
                } else {
                    firstRowEnemyExists = true;
                }
            }
        }


        //
        // Schauen wir mal ob wir was sinnvolles aus der DB gelesen haben
        // - Wenn nicht: Abbrechen
        //

        if (ownBattleShip == null) {
            throw new IllegalArgumentException("Offenbar liegt ein Problem mit dem von Ihnen angegebenen Schiff oder Ihrem eigenen Schiff vor (wird es evtl. bereits angegriffen?).");
        }

        Battle battle = new Battle(ownShip.getLocation());
        battle.getOwnShips().addAll(ownShips);
        battle.getEnemyShips().addAll(enemyShips);

        addToSecondRow(battle, secondRowShips, firstRowExists, firstRowEnemyExists);

        if (enemyBattleShip == null && battle.getEnemyShips().isEmpty()) {
            throw new IllegalArgumentException("Offenbar liegt ein Problem mit den feindlichen Schiffen vor. Es gibt nämlich keine die angegriffen werden könnten.");
        } else if (enemyBattleShip == null) {
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
        em.persist(battle);

        //
        // Schiffe in die Schlacht einfuegen
        //

        int tick = configService.getValue(WellKnownConfigValue.TICKS);

        // * Gegnerische Schiffe in die Schlacht einfuegen
        List<Integer> idlist = new ArrayList<>();
        List<Integer> startlist = new ArrayList<>();
        enemyShip = enemyBattleShip.getShip();
        if (enemyBattleShip.getShip().isLanded()) {
            shipService.start(shipService.getBaseShip(enemyShip), enemyShip);
            startlist.add(enemyBattleShip.getId());
        }
        idlist.add(enemyBattleShip.getId());

        enemyBattleShip.setBattle(battle);
        em.persist(enemyBattleShip);

        enemyShip.setBattle(battle);

        insertShipsIntoDatabase(battle, battle.getEnemyShips(), startlist, idlist);
        if (!startlist.isEmpty()) {
            logme(battle, startlist.size() + " Jäger sind automatisch gestartet\n");
            log(battle, new SchlachtLogAktion(1, startlist.size() + " Jäger sind automatisch gestartet"));

            startlist.clear();
        }

        startlist = new ArrayList<>();
        battle.getEnemyShips().add(enemyBattleShip);
        battle.setEnemyShipIndex(battle.getEnemyShips().size() - 1);

        // * Eigene Schiffe in die Schlacht einfuegen
        idlist.add(ownBattleShip.getId());

        ownBattleShip.setBattle(battle);
        em.persist(ownBattleShip);

        ownShip.setBattle(battle);
        if (ownBattleShip.getShip().isLanded()) {
            shipService.start(shipService.getBaseShip(ownShip), enemyShip);
            startlist.add(enemyBattleShip.getId());
        }
        ownShip.setDocked("");

        insertShipsIntoDatabase(battle, battle.getOwnShips(), startlist, idlist);
        if (startOwn && !startlist.isEmpty()) {
            logme(battle, startlist.size() + " Jäger sind automatisch gestartet\n");
            log(battle, new SchlachtLogAktion(0, startlist.size() + " Jäger sind automatisch gestartet"));
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

        for (User auser : new ArrayList<>(ownUsers)) {
            if ((auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId())) {
                List<User> allyusers = em.createQuery("from User u where u.ally=:ally and (u not in :ownUsers)", User.class)
                    .setParameter("ally", auser.getAlly())
                    .setParameter("ownUsers", ownUsers)
                    .getResultList();

                ownUsers.addAll(allyusers);
                calcedallys.add(auser.getAlly().getId());
            }
        }

        for (User auser : new ArrayList<>(enemyUsers)) {
            if ((auser.getAlly() != null) && !calcedallys.contains(auser.getAlly().getId())) {
                List<User> allyusers = em.createQuery("from User u where ally=:ally and (u not in :enemyUsers)", User.class)
                    .setParameter("ally", auser.getAlly())
                    .setParameter("enemyUsers", enemyUsers)
                    .getResultList();
                enemyUsers.addAll(allyusers);
                calcedallys.add(auser.getAlly().getId());
            }
        }

        for (User auser : ownUsers) {
            for (User euser : enemyUsers) {
                userService.setRelation(auser, euser, User.Relation.ENEMY);
                userService.setRelation(euser, auser, User.Relation.ENEMY);
            }
        }

        // PM Wegen Schlachteröffnung schicken, sofern die Spieler dies wollen.
        String eparty;
        String eparty2;
        if (battle.getAlly(0) == 0) {
            final User commander1 = battle.getCommander(0);
            eparty = commander1.getNickname();
        } else {
            final Ally ally = em.find(Ally.class, battle.getAlly(0));
            eparty = ally.getName();
        }

        if (battle.getAlly(1) == 0) {
            final User commander2 = battle.getCommander(1);
            eparty2 = commander2.getNickname();
        } else {
            final Ally ally = em.find(Ally.class, battle.getAlly(1));
            eparty2 = ally.getName();
        }
        User niemand = em.find(User.class, -1);
        String msg = "Es wurde eine Schlacht bei " + locationService.displayCoordinates(ownShip.getLocation(), false) + " eröffnet.\n" +
            "Es kämpfen " + eparty + " (" + battle.getOwnShips().size() + " Schiffe) und " + eparty2 + " (" + battle.getEnemyShips().size() + " Schiffe) gegeneinander. " +
            "Deine 2. Reihe ist ";
        String msg1 = "";
        String msg2 = "";
        if (battle.isSecondRowStable(0)) {
            msg1 += "stabil.";
        } else {
            msg1 += "instabil. Vorsicht!";
        }
        if (battle.isSecondRowStable(1)) {
            msg2 += "stabil.";
        } else {
            msg2 += "instabil. Vorsicht!";
        }
        for (User auser : ownUsers) {
            if (userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM)) {
                pmService.send(niemand, auser.getId(), "Schlacht eröffnet", msg + msg1);
            }

        }
        for (User auser : enemyUsers) {
            if (userValueService.getUserValue(user, WellKnownUserValue.GAMEPLAY_USER_BATTLE_PM)) {
                pmService.send(niemand, auser.getId(), "Schlacht eröffnet", msg + msg2);
            }

        }

        return battle;
    }

    private void createBattleLog(Battle battle, BattleShip ownBattleShip, BattleShip enemyBattleShip, int tick) {
        SchlachtLog log = new SchlachtLog(battle, tick);
        em.persist(log);
        battle.setSchlachtLog(log);

        log(battle, new SchlachtLogKommandantWechselt(0, ownBattleShip.getOwner()));
        log(battle, new SchlachtLogKommandantWechselt(1, enemyBattleShip.getOwner()));
    }

    private void insertShipsIntoDatabase(Battle battle, List<BattleShip> ships, List<Integer> startlist, List<Integer> idlist) {
        if (ships.isEmpty()) {
            return;
        }

        for (BattleShip ship : ships) {
            Ship baseShip = shipService.getBaseShip(ship.getShip());
            if (baseShip != null && ship.getShip().isLanded() && baseShip.getEinstellungen().startFighters() && ship.getShip().getTypeData().getShipClass() == ShipClasses.JAEGER) {
                ship.getShip().setDocked("");
                startlist.add(ship.getId());
            }
            idlist.add(ship.getId());
            ship.getShip().setBattle(battle);
            ship.setBattle(battle);
            em.persist(ship);
        }
    }

    private void checkBattleConditions(User user, User enemyUser, Ship ownShip, Ship enemyShip) {
        // Kann der Spieler ueberhaupt angreifen (Noob-Schutz?)
        if (userService.isNoob(user)) {
            throw new IllegalArgumentException("Sie stehen unter GCP-Schutz (Neulingsschutz) und k&ouml;nnen daher keinen Gegner angreifen!<br />Hinweis: Der GCP-Schutz kann in den Optionen vorzeitig beendet werden");
        }

        if ((ownShip == null) || (ownShip.getId() < 0) || (ownShip.getOwner() != user)) {
            throw new IllegalArgumentException("Das angreifende Schiff existiert nicht oder untersteht nicht Ihrem Kommando!");
        }

        if ((enemyShip == null) || (enemyShip.getId() < 0)) {
            throw new IllegalArgumentException("Das angegebene Zielschiff existiert nicht!");
        }

        if (!ownShip.getLocation().sameSector(0, enemyShip.getLocation(), 0)) {
            throw new IllegalArgumentException("Die beiden Schiffe befinden sich nicht im selben Sektor");
        }

        //
        // Kann der Spieler angegriffen werden (NOOB-Schutz?/Vac-Mode?)
        //

        if (userService.isNoob(enemyUser)) {
            throw new IllegalArgumentException("Der Gegner steht unter GCP-Schutz (Neulingsschutz) und kann daher nicht angegriffen werden!");
        }

        if (enemyUser.getVacationCount() != 0 && enemyUser.getWait4VacationCount() == 0) {
            throw new IllegalArgumentException("Der Gegner befindet sich im Vacation-Modus und kann daher nicht angegriffen werden!");
        }

        //
        // IFF-Stoersender?
        //
        boolean disableIff = enemyShip.getStatus().contains("disable_iff");
        if (disableIff) {
            throw new IllegalArgumentException("Dieses Schiff kann nicht angegriffen werden (egal wieviel Du mit der URL rumspielst!)");
        }
    }

    /**
     * Adds ships which have the second row flag to the second row.
     * If there is no first row no ship will be added to the second row.
     *
     * @param battle              Die momentan in der Erstellung befindliche Schlacht
     * @param secondRowShips      Ships to add.
     * @param firstRowExists      True, if side one has a first row.
     * @param firstRowEnemyExists True, if side two has a first row.
     */
    private void addToSecondRow(Battle battle, Set<BattleShip> secondRowShips, boolean firstRowExists, boolean firstRowEnemyExists) {
        Map<Ship, BattleShip> battleShipMap = new HashMap<>();
        for (BattleShip ship : battle.getOwnShips()) {
            battleShipMap.put(ship.getShip(), ship);
        }
        for (BattleShip ship : battle.getEnemyShips()) {
            battleShipMap.put(ship.getShip(), ship);
        }

        for (BattleShip ship : secondRowShips) {
            if ((ship.getSide() == 0 && firstRowExists && ship.getShip().getEinstellungen().gotoSecondrow()) || (ship.getSide() == 1 && firstRowEnemyExists && ship.getShip().getEinstellungen().gotoSecondrow())) {

                ship.addFlag(BattleShipFlag.SECONDROW);
                if (ship.getTypeData().getJDocks() > 0) {
                    List<Ship> landedShips = shipService.getLandedShips(ship.getShip());
                    for (Ship landedShip : landedShips) {
                        if (!landedShip.getTypeData().hasFlag(ShipTypeFlag.SECONDROW)) {
                            continue;
                        }

                        BattleShip aship = battleShipMap.get(landedShip);
                        if (aship != null) {
                            aship.addFlag(BattleShipFlag.SECONDROW);
                        }
                    }
                }
                if (ship.getTypeData().getADocks() == 0) {
                    List<Ship> dockedShips = shipService.getDockedShips(ship.getShip());
                    for (Ship dockedShip : dockedShips) {

                        BattleShip aship = battleShipMap.get(dockedShip);
                        if (aship != null) {
                            aship.addFlag(BattleShipFlag.SECONDROW);
                        }
                    }
                }
            }
        }
    }

    /**
     * Laesst eines oder mehrere Schiffe (in einer Flotte) der Schlacht beitreten.
     *
     * @param id     Die ID des Besitzers der Schiffe
     * @param shipid Die ID eines der Schiffe, welche beitreten sollen
     * @return <code>true</code>, falls der Beitritt erfolgreich war
     */
    public boolean addShip(Battle battle, int id, int shipid) {
        Context context = ContextMap.getContext();

        Ship shipd = em.find(Ship.class, shipid);

        if ((shipd == null) || (shipd.getId() < 0)) {
            context.addError("Das angegebene Schiff existiert nicht!");
            return false;
        }
        if (shipd.getOwner().getId() != id) {
            context.addError("Das angegebene Schiff geh&ouml;rt nicht ihnen!");
            return false;
        }
        if (!new Location(battle.getSystem(), battle.getX(), battle.getY()).sameSector(0, shipd.getLocation(), 0)) {
            context.addError("Das angegebene Schiff befindet sich nicht im selben Sektor wie die Schlacht!");
            return false;
        }
        if (shipd.getBattle() != null) {
            context.addError("Das angegebene Schiff befindet sich bereits in einer Schlacht!");
            return false;
        }

        User userobj = em.find(User.class, id);
        if (userService.isNoob(userobj)) {
            context.addError("Sie stehen unter GCP-Schutz und k&ouml;nnen daher keine Schiffe in diese Schlacht schicken!<br />Hinweis: der GCP-Schutz kann unter Optionen vorzeitig beendet werden");
            return false;
        }

        ShipTypeData shiptype = shipd.getTypeData();
        if ((shiptype.getShipClass() == ShipClasses.GESCHUETZ)) {
            context.addError("<span style=\"color:red\">Gesch&uuml;tze k&ouml;nnen einer Schlacht nicht beitreten!<br />Diese m&uuml;ssen von Frachtern mitgenommen werden!</span>");
            return false;
        }

        Map<Integer, Integer> shipcounts = new HashMap<>();

        int side = battle.getOwnSide();

        // Beziehungen aktualisieren
        Set<Integer> calcedallys = new HashSet<>();

        List<User> ownUsers = new ArrayList<>();
        ownUsers.add(userobj);
        Set<User> enemyUsers = new HashSet<>();

        if (userobj.getAlly() != null) {
            List<User> users = userobj.getAlly().getMembers();
            for (User auser : users) {
                if (auser.getId() == userobj.getId()) {
                    continue;
                }
                ownUsers.add(auser);
            }
        }

        List<User> users = em.createQuery("select distinct bs.ship.owner " +
            "from BattleShip bs " +
            "where bs.battle= :battleId and bs.side= :sideId", User.class)
            .setParameter("battleId", battle.getId())
            .setParameter("sideId", battle.getEnemySide())
            .getResultList();

        for (User euser : users) {
            enemyUsers.add(euser);

            if ((euser.getAlly() != null) && !calcedallys.contains(euser.getAlly().getId())) {
                List<User> allyusers = euser.getAlly().getMembers();
                for (User auser : allyusers) {
                    if (auser.getId() == euser.getId()) {
                        continue;
                    }
                    enemyUsers.add(auser);
                }

                calcedallys.add(euser.getAlly().getId());
            }
        }

        for (User auser : ownUsers) {
            for (User euser : enemyUsers) {
                userService.setRelation(auser, euser, User.Relation.ENEMY);
                userService.setRelation(euser, auser, User.Relation.ENEMY);
            }
        }

        List<Integer> shiplist = new ArrayList<>();

        List<Ship> sid;
        // Handelt es sich um eine Flotte?
        if (shipd.getFleet() != null) {
            sid = em.createQuery("from Ship as s where s.id>0 and s.fleet=:fleet and s.battle is null and s.x=:x and s.y=:y and s.system=:sys", Ship.class)
                .setParameter("fleet", shipd.getFleet())
                .setParameter("x", shipd.getX())
                .setParameter("y", shipd.getY())
                .setParameter("sys", shipd.getSystem())
                .getResultList();
        } else {
            sid = em.createQuery("from Ship as s where s.id>0 and s.id=:id and s.battle is null and s.x=:x and s.y=:y and s.system=:sys", Ship.class)
                .setParameter("id", shipd.getId())
                .setParameter("x", shipd.getX())
                .setParameter("y", shipd.getY())
                .setParameter("sys", shipd.getSystem())
                .getResultList();
        }

        for (Ship aship : sid) {
            if (em.find(BattleShip.class, aship.getId()) != null) {
                continue;
            }

            shiptype = aship.getTypeData();
            if (shiptype.getShipClass() == ShipClasses.GESCHUETZ) {
                continue;
            }

            shiplist.add(aship.getId());

            // ggf. gedockte Schiffe auch beruecksichtigen
            List<Ship> docked = em.createQuery("from Ship where id>0 and battle is null and docked in (:docked,:landed)", Ship.class)
                .setParameter("docked", Integer.toString(aship.getId()))
                .setParameter("landed", "l " + aship.getId())
                .getResultList();

            for (Ship dockedShip : docked) {
                if (em.find(BattleShip.class, dockedShip.getId()) != null) {
                    continue;
                }

                BattleShip sid2bs = new BattleShip(battle, dockedShip);

                ShipTypeData stype = dockedShip.getTypeData();
                if (stype.getShipClass() == ShipClasses.GESCHUETZ) {
                    sid2bs.addFlag(BattleShipFlag.BLOCK_WEAPONS);
                }

                shiplist.add(dockedShip.getId());


                // Das neue Schiff in die Liste der eigenen Schiffe eintragen
                if (!shiptype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER) &&
                    !stype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER)) {
                    sid2bs.addFlag(BattleShipFlag.JOIN);
                }

                sid2bs.setSide(battle.getOwnSide());

                battle.getOwnShips().add(sid2bs);

                Common.safeIntInc(shipcounts, dockedShip.getType());

                em.persist(sid2bs);

                dockedShip.setBattle(battle);
            }

            BattleShip aBattleShip = new BattleShip(battle, aship);

            // Das neue Schiff in die Liste der eigenen Schiffe eintragen
            if (!shiptype.hasFlag(ShipTypeFlag.INSTANT_BATTLE_ENTER)) {
                aBattleShip.addFlag(BattleShipFlag.JOIN);
            }

            aBattleShip.setSide(side);

            battle.getOwnShips().add(aBattleShip);

            Common.safeIntInc(shipcounts, aship.getType());

            em.persist(aBattleShip);

            aship.setBattle(battle);
        }

        if (shiplist.size() > 1) {
            int addedShips = shiplist.size() - 1;
            log(battle, new SchlachtLogAktion(battle.getOwnSide(), "Die " + Battle.log_shiplink(shipd) + " ist zusammen mit " + addedShips + " weiteren Schiffen der Schlacht beigetreten"));
            logme(battle, "Die " + Battle.log_shiplink(shipd) + " ist zusammen mit " + addedShips + " weiteren Schiffen der Schlacht beigetreten\n\n");
        } else {
            log(battle, new SchlachtLogAktion(battle.getOwnSide(), "Die " + Battle.log_shiplink(shipd) + " ist der Schlacht beigetreten"));
            logme(battle, "Die " + Battle.log_shiplink(shipd) + " ist der Schlacht beigetreten\n\n");

            shipd.setBattle(battle);
        }

        return true;
    }

    /**
     * Gibt zurueck, auf welcher Seite ein Spieler Teil der Schlacht ist.
     * Falls ein Spieler nicht Teil der Schlacht ist wird <code>-1</code>
     * zurueckgegeben.
     *
     * @param user Der Spieler
     * @return Die Seite oder <code>-1</code>
     */
    public int getSchlachtMitglied(Battle battle, User user) {
        for (int i = 0; i <= 1; i++) {
            if (user.getAlly() != null && user.getAlly().getId() == battle.getAlly(i)) {
                return i;
            }
            if (battle.getCommander(i).getId() == user.getId()) {
                return i;
            }
        }

        // Hat der Spieler ein Schiff in der Schlacht
        BattleShip aship = em.createQuery("from BattleShip where id>0 and ship.owner=:user and battle=:battle", BattleShip.class)
            .setParameter("user", user)
            .setParameter("battle", battle)
            .setMaxResults(1)
            .getSingleResult();

        if (aship != null) {
            return aship.getSide();
        }
        return -1;
    }

    /**
     * Laedt weitere Schlachtdaten aus der Datenbank.
     *
     * @param user      Der aktive Spieler
     * @param ownShip   Das auszuwaehlende eigene Schiff (oder <code>null</code>)
     * @param enemyShip Das auszuwaehlende gegnerische Schiff (oder <code>null</code>)
     * @param forcejoin Die ID einer Seite (1 oder 2), welche als die eigene zu waehlen ist. Falls 0 wird automatisch eine gewaehlt
     * @return <code>true</code>, falls die Schlacht erfolgreich geladen wurde
     */
    public boolean load(Battle battle, User user, Ship ownShip, Ship enemyShip, int forcejoin) {
        Context context = ContextMap.getContext();
        //
        // Darf der Spieler (evt als Gast) zusehen?
        //

        int forceSide;

        if (forcejoin == 0) {
            forceSide = this.getSchlachtMitglied(battle, user);
            if (forceSide == -1) {
                //Mehr ueber den Spieler herausfinden
                if (context.hasPermission(WellKnownPermission.SCHLACHT_ALLE_AUFRUFBAR)) {
                    battle.setGuest(true);
                } else {
                    long shipcount = em.createQuery("select count(*) from Ship " +
                        "where owner= :user and x= :x and y= :y and system= :sys and " +
                        "battle is null and shiptype.shipClass in (:shipClasses)", Long.class)
                        .setParameter("user", user)
                        .setParameter("x", battle.getX())
                        .setParameter("y", battle.getY())
                        .setParameter("sys", battle.getSystem())
                        .setParameter("shipClasses", ShipClasses.darfSchlachtenAnsehen())
                        .getSingleResult();
                    if (shipcount > 0) {
                        battle.setGuest(true);
                    } else {
                        context.addError("Sie verf&uuml;gen &uuml;ber kein geeignetes Schiff im Sektor um die Schlacht zu verfolgen");
                        return false;
                    }
                }
            }
        } else {
            forceSide = forcejoin - 1;
        }

        //
        // Eigene Seite feststellen
        //

        if ((user.getAlly() != null && (user.getAlly().getId() == battle.getAlly(0))) || battle.isCommander(user, 0) || battle.isGuest() || forceSide == 0) {
            battle.setOwnSide(0);
            battle.setEnemySide(1);
        } else if ((user.getAlly() != null && (user.getAlly().getId() == battle.getAlly(1))) || battle.isCommander(user, 1) || forceSide == 1) {
            battle.setOwnSide(1);
            battle.setEnemySide(0);
        }

        //
        // Liste aller Schiffe in der Schlacht erstellen
        //

        battle.getOwnShips().clear();
        battle.getEnemyShips().clear();

        battle.getOwnShipTypeCount().clear();
        battle.getEnemyShipTypeCount().clear();

        List<BattleShip> ships = em.createQuery("from BattleShip bs inner join fetch bs.ship as s " +
            "where s.id>0 and bs.battle=:battle " +
            "order by s.shiptype.id, s.id", BattleShip.class)
            .setParameter("battle", this)
            .getResultList();

        for (BattleShip ship : ships) {

            if (ship.getSide() == battle.getOwnSide()) {
                battle.getOwnShips().add(ship);
                if (!battle.isGuest() || !ship.getShip().isLanded()) {
                    if (battle.getOwnShipTypeCount().containsKey(ship.getShip().getType())) {
                        battle.getOwnShipTypeCount().put(ship.getShip().getType(), battle.getOwnShipTypeCount().get(ship.getShip().getType()) + 1);
                    } else {
                        battle.getOwnShipTypeCount().put(ship.getShip().getType(), 1);
                    }
                }
            } else if (ship.getSide() == battle.getEnemySide()) {
                battle.getEnemyShips().add(ship);
                if (!ship.getShip().isLanded()) {
                    if (battle.getEnemyShipTypeCount().containsKey(ship.getShip().getType())) {
                        battle.getEnemyShipTypeCount().put(ship.getShip().getType(), battle.getEnemyShipTypeCount().get(ship.getShip().getType()) + 1);
                    } else {
                        battle.getEnemyShipTypeCount().put(ship.getShip().getType(), 1);
                    }
                }
            }
        }

        if (battle.getOwnShips().isEmpty() || battle.getEnemyShips().isEmpty()) {
            return false;
        }

        //
        // aktive Schiffe heraussuchen
        //

        battle.setActiveSEnemy(0);
        battle.setActiveSOwn(0);

        battle.setFiringShip(ownShip);
        battle.setAttackedShip(enemyShip);

        // Falls die gewaehlten Schiffe gelandet (oder zerstoert) sind -> neue Schiffe suchen
        while (battle.getActiveSEnemy() < battle.getEnemyShips().size() &&
            (battle.getEnemyShips().get(battle.getActiveSEnemy()).hasFlag(BattleShipFlag.DESTROYED) ||
                battle.getEnemyShips().get(battle.getActiveSEnemy()).getShip().isLanded())) {
            battle.setActiveSEnemy(battle.getActiveSEnemy() + 1);
        }

        if (battle.getActiveSEnemy() >= battle.getEnemyShips().size()) {
            battle.setActiveSEnemy(0);
        }

        if (battle.isGuest()) {
            while (battle.getActiveSOwn() < battle.getOwnShips().size() && battle.getOwnShips().get(battle.getActiveSOwn()).getShip().isLanded()) {
                battle.setActiveSOwn(battle.getActiveSOwn() + 1);
            }

            if (battle.getActiveSOwn() >= battle.getOwnShips().size()) {
                battle.setActiveSOwn(0);
            }
        }

        return true;
    }

    /**
     * Entfernt ein Schiff aus einer Schlacht und platziert es falls gewuenscht in einem zufaelligen Sektor
     * um die Schlacht herum. Evt gedockte Schiffe werden mitentfernt und im selben Sektor platziert.
     *
     * @param ship     Das fliehende Schiff
     * @param relocate Soll ein zufaelliger Sektor um die Schlacht herum gewaehlt werden? (<code>true</code>)
     */
    public void removeShip(Battle battle, BattleShip ship, boolean relocate) {
        Location loc = ship.getShip().getLocation();

        if (relocate && !ship.getShip().isLanded() && !ship.getShip().isDocked()) {
            StarSystem sys = em.find(StarSystem.class, battle.getSystem());
            int maxRetries = 100;

            while (((loc.getX() == battle.getX()) && (loc.getY() == battle.getY())) ||
                (loc.getX() < 1) || (loc.getY() < 1) ||
                (loc.getX() > sys.getWidth()) ||
                (loc.getY() > sys.getHeight())) {
                loc = loc.setX(battle.getX() + ThreadLocalRandom.current().nextInt(3) - 1);
                loc = loc.setY(battle.getY() + ThreadLocalRandom.current().nextInt(3) - 1);

                maxRetries--;
                if (maxRetries == 0) {
                    break;
                }
            }
        }

        if (ship.getShip().getBattle() == null) {
            // Es kann vorkommen, dass das Schiff bereits entfernt wurde (wegen einer dock-Beziehung)
            return;
        }

        // Falls das Schiff an einem anderen Schiff gedockt ist, dann das
        // Elternschiff fliehen lassen. Dieses kuemmert sich dann um die
        // gedockten Schiffe
        if (ship.getShip().isDocked() || ship.getShip().isLanded()) {
            int masterid = shipService.getBaseShip(ship.getShip()).getId();

            List<BattleShip> shiplist = battle.getOwnShips();
            if (ship.getSide() != battle.getOwnSide()) {
                shiplist = battle.getEnemyShips();
            }

            for (BattleShip aship : shiplist) {
                if (aship.getId() == masterid) {
                    removeShip(battle, aship, relocate);
                    return;
                }
            }
        }

        long dockcount = shipService.getAnzahlGedockterUndGelandeterSchiffe(ship.getShip());

        ship.getShip().setBattle(null);
        ship.getShip().setX(loc.getX());
        ship.getShip().setY(loc.getY());

        em.remove(ship);
        shipActionService.recalculateShipStatus(ship.getShip());

        //
        // Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
        //

        boolean found = false;
        List<BattleShip> shiplist = battle.getOwnShips();

        if (ship.getSide() != battle.getOwnSide()) {
            shiplist = battle.getEnemyShips();
        }

        ListIterator<BattleShip> shipsIterator = shiplist.listIterator();
        while (shipsIterator.hasNext()) {
            BattleShip aShip = shipsIterator.next();

            if (aShip == ship) {
                shipsIterator.remove();
                found = true;
            }
            // Evt ist das Schiff an das gerade fliehende gedockt
            // In diesem Fall muss es ebenfalls entfernt werden
            else if (dockcount > 0 && shipService.getBaseShip(aShip.getShip()) != null && shipService.getBaseShip(aShip.getShip()).getId() == ship.getId()) {
                shipsIterator.remove();

                dockcount--;

                aShip.getShip().setBattle(null);
                aShip.getShip().setBattleAction(false);
                aShip.getShip().setX(loc.getX());
                aShip.getShip().setY(loc.getY());

                em.remove(aShip);
                shipActionService.recalculateShipStatus(aShip.getShip());
            }

            if (found && (dockcount == 0)) {
                break;
            }
        }

        if ((ship.getSide() == battle.getEnemySide()) && (battle.getActiveSEnemy() >= shiplist.size())) {
            battle.setActiveSEnemy(0);
        } else if ((ship.getSide() == battle.getOwnSide()) && (battle.getActiveSOwn() >= shiplist.size())) {
            battle.setActiveSOwn(0);
        }
    }

    /**
     * Beendet die laufende Runde und berechnet einen Rundenwechsel.
     *
     * @param calledByUser Wurde das Rundenende (in)direkt durch einen Spieler ausgeloesst? (<code>true</code>)
     * @return <code>true</code>, falls die Schlacht weiterhin existiert. <code>false</code>, falls sie beendet wurde.
     */
    public boolean endTurn(Battle battle, boolean calledByUser) {
        Context context = ContextMap.getContext();

        List<List<BattleShip>> sides = new ArrayList<>();
        if (battle.getOwnSide() == 0) {
            sides.add(battle.getOwnShips());
            sides.add(battle.getEnemyShips());
        } else {
            sides.add(battle.getEnemyShips());
            sides.add(battle.getOwnShips());
        }

        //
        // Zuerst die Schiffe berechnen
        //
        for (int i = 0; i < 2; i++) {
            List<BattleShip> shipsSecond = new ArrayList<>();

            // Liste kopieren um Probleme beim Entfernen von Schiffen aus der Ursprungsliste zu vermeiden
            List<BattleShip> shiplist = new ArrayList<>(sides.get(i));
            for (BattleShip ship : shiplist) {
                if (ship.hasFlag(BattleShipFlag.HIT)) {
                    ship.getShip().setAblativeArmor(ship.getAblativeArmor());
                    ship.getShip().setHull(ship.getHull());
                    ship.getShip().setShields(ship.getShields());
                    ship.getShip().setEngine(ship.getEngine());
                    ship.getShip().setWeapons(ship.getWeapons());
                    ship.getShip().setComm(ship.getComm());
                    ship.getShip().setSensors(ship.getSensors());
                    ship.removeFlag(BattleShipFlag.HIT);
                } else if (ship.hasFlag(BattleShipFlag.DESTROYED)) {
                    if (new ConfigService().getValue(WellKnownConfigValue.DESTROYABLE_SHIPS)) {
                        //
                        // Verluste verbuchen (zerstoerte/verlorene Schiffe)
                        //
                        User destroyer = em.find(User.class, ship.getDestroyer());
                        Ally destroyerAlly = destroyer.getAlly();
                        if (destroyerAlly != null) {
                            destroyerAlly.setDestroyedShips(destroyerAlly.getDestroyedShips() + 1);
                        }
                        destroyer.setDestroyedShips(destroyer.getDestroyedShips() + 1);

                        Ally looserAlly = ship.getOwner().getAlly();
                        if (looserAlly != null) {
                            looserAlly.setLostShips(looserAlly.getLostShips() + 1);
                        }
                        User looser = ship.getOwner();
                        looser.setLostShips(looser.getLostShips() + 1);

                        ShipLost lost = new ShipLost(ship.getShip());
                        lost.setDestAlly(destroyerAlly);
                        lost.setDestOwner(destroyer);
                        em.persist(lost);

                        destroyShip(battle, ship);
                        continue;
                    } else {
                        ship.removeFlag(BattleShipFlag.DESTROYED);
                        continue; //Das Schiff kann nicht zerstoert werden
                    }
                }

                if (ship.hasFlag(BattleShipFlag.FLUCHT)) {
                    ShipTypeData ashipType = ship.getTypeData();
                    removeShip(battle, ship, ashipType.getCost() > 0);
                }

                ship.removeFlag(BattleShipFlag.SHOT);
                ship.removeFlag(BattleShipFlag.SECONDROW_BLOCKED);

                if (ship.hasFlag(BattleShipFlag.BLOCK_WEAPONS)) {
                    if (!((ship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ) && ship.getShip().isDocked())) {
                        ship.removeFlag(BattleShipFlag.BLOCK_WEAPONS);
                    }
                }

                if ((i == 0) && battle.hasFlag(BattleFlag.DROP_SECONDROW_0)) {
                    ship.removeFlag(BattleShipFlag.SECONDROW);
                } else if ((i == 1) && battle.hasFlag(BattleFlag.DROP_SECONDROW_1)) {
                    ship.removeFlag(BattleShipFlag.SECONDROW);
                }

                if (ship.hasFlag(BattleShipFlag.JOIN)) {
                    ShipTypeData ashipType = ship.getTypeData();
                    if (ashipType.hasFlag(ShipTypeFlag.SECONDROW)) {
                        shipsSecond.add(ship);
                    }
                    ship.removeFlag(BattleShipFlag.JOIN);
                }

                Map<String, Integer> heat = ship.getWeaponHeat();

                heat.replaceAll((n, v) -> 0);

                if (ship.hasFlag(BattleShipFlag.FLUCHTNEXT)) {
                    ship.removeFlag(BattleShipFlag.FLUCHTNEXT);
                    ship.addFlag(BattleShipFlag.FLUCHT);
                }

                ship.getShip().setWeaponHeat(heat);
                ship.getShip().setBattleAction(false);
            }

            for (BattleShip second : shipsSecond) {
                if (battle.isSecondRowStable(i, second)) {
                    second.addFlag(BattleShipFlag.SECONDROW);
                }
            }
        }

        context.getDB().flush();

        // Ist die Schlacht zuende (weil keine Schiffe mehr vorhanden sind?)
        int owncount = battle.getOwnShips().size();
        int enemycount = battle.getEnemyShips().size();

        if ((owncount == 0) && (enemycount == 0)) {
            pmService.send(battle.getCommanders()[battle.getEnemySide()], battle.getCommanders()[battle.getOwnSide()].getId(), "Schlacht unentschieden", "Die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getEnemySide()].getName() + " wurde mit einem Unentschieden beendet!");
            pmService.send(battle.getCommanders()[battle.getOwnSide()], battle.getCommanders()[battle.getEnemySide()].getId(), "Schlacht unentschieden", "Die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getOwnSide()].getName() + " wurde mit einem Unentschieden beendet!");

            // Schlacht beenden - unendschieden
            endBattle(battle, 0, 0);

            battle.getOwnShips().clear();
            battle.getEnemyShips().clear();

            if (calledByUser) {
                try {
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
                        .append(locationService.urlFragment(battle.getLocation())).append("'>")
                        .append(locationService.displayCoordinates(battle.getLocation(), false))
                        .append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(bbCodeParser, battle.getCommanders()[battle.getEnemySide()].getName()));
                    context.getResponse().getWriter().append(" mit einem Unentschieden beendet!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        } else if (owncount == 0) {
            pmService.send(battle.getCommanders()[battle.getEnemySide()], battle.getCommanders()[battle.getOwnSide()].getId(), "Schlacht verloren", "Du hast die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getEnemySide()].getName() + " verloren!");
            pmService.send(battle.getCommanders()[battle.getOwnSide()], battle.getCommanders()[battle.getEnemySide()].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getOwnSide()].getName() + " gewonnen!");

            // Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
            if (battle.getOwnSide() == 0) {
                this.endBattle(battle, -1, 1);
            } else {
                this.endBattle(battle, 1, -1);
            }

            battle.getOwnShips().clear();
            battle.getEnemyShips().clear();


            if (calledByUser) {
                try {
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
                        .append(locationService.urlFragment(battle.getLocation())).append("'>")
                        .append(locationService.displayCoordinates(battle.getLocation(), false))
                        .append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(bbCodeParser, battle.getCommanders()[battle.getEnemySide()].getName()));
                    context.getResponse().getWriter().append(" verloren!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        } else if (enemycount == 0) {
            pmService.send(battle.getCommanders()[battle.getEnemySide()], battle.getCommanders()[battle.getOwnSide()].getId(), "Schlacht gewonnen", "Du hast die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getEnemySide()].getName() + " gewonnen!");
            pmService.send(battle.getCommanders()[battle.getOwnSide()], battle.getCommanders()[battle.getEnemySide()].getId(), "Schlacht verloren", "Du hast die Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " gegen " + battle.getCommanders()[battle.getOwnSide()].getName() + " verloren!");

            // Schlacht beenden - eine siegreiche Schlacht fuer den aktive Seite verbuchen sowie eine verlorene fuer den Gegner
            if (battle.getOwnSide() == 0) {
                this.endBattle(battle, 1, -1);
            } else {
                this.endBattle(battle, -1, 1);
            }

            battle.getOwnShips().clear();
            battle.getEnemyShips().clear();

            if (calledByUser) {
                try {
                    context.getResponse().getWriter().append("Du hast die Schlacht bei <a class='forschinfo' href='./client#/map/")
                        .append(locationService.urlFragment(battle.getLocation())).append("'>")
                        .append(locationService.displayCoordinates(battle.getLocation(), false))
                        .append("</a> gegen ");
                    context.getResponse().getWriter().append(Common._title(bbCodeParser, battle.getCommanders()[battle.getEnemySide()].getName()));
                    context.getResponse().getWriter().append(" gewonnen!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        }

        battle.setReady(battle.getOwnSide(), false);
        battle.setReady(battle.getEnemySide(), false);
        battle.setBlockcount(2);

        battle.setLastturn(Common.time());

        for (int i = 0; i < 2; i++) {
            if (!calledByUser && battle.getTakeCommands()[i] != 0) {
                User com = em.find(User.class, battle.getTakeCommands()[i]);

                pmService.send(com, battle.getCommanders()[i].getId(), "Schlacht &uuml;bernommen", "Ich habe die Leitung der Schlacht bei " + locationService.displayCoordinates(battle.getLocation(), false) + " &uuml;bernommen.");

                log(battle, new SchlachtLogAktion(i, "[Automatisch] " + Common._titleNoFormat(bbCodeParser, com.getName()) + " kommandiert nun die Truppen"));

                battle.setCommander(i, com);

                log(battle, new SchlachtLogKommandantWechselt(i, battle.getCommanders()[i]));

                battle.setTakeCommand(i, 0);
            }
        }

        if (battle.hasFlag(BattleFlag.FIRSTROUND)) {
            battle.setFlag(BattleFlag.FIRSTROUND, false);
        }

        battle.setFlag(BattleFlag.BLOCK_SECONDROW_0, false);
        battle.setFlag(BattleFlag.BLOCK_SECONDROW_1, false);

        if (battle.hasFlag(BattleFlag.DROP_SECONDROW_0)) {
            battle.setFlag(BattleFlag.DROP_SECONDROW_0, false);
            battle.setFlag(BattleFlag.BLOCK_SECONDROW_0, true);
        }
        if (battle.hasFlag(BattleFlag.DROP_SECONDROW_1)) {
            battle.setFlag(BattleFlag.DROP_SECONDROW_1, false);
            battle.setFlag(BattleFlag.BLOCK_SECONDROW_1, true);
        }

        return true;
    }

    /**
     * Beendet die Schlacht.
     *
     * @param side1points Die Punkte, die die erste Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
     * @param side2points Die Punkte, die die zweite Seite bekommen soll (Positiv meint Schlacht gewonnen; Negativ meint Schlacht verloren)
     */
    public void endBattle(Battle battle, int side1points, int side2points) {
        if (battle.getDeleted()) {
            return;
        }

        battle.setDeleted(true);

        em.createQuery("delete from BattleShip where battle=:battle", BattleShip.class)
            .setParameter("battle", this)
            .executeUpdate();
        em.createQuery("update Ship set battle=null,battleAction=0 where id>0 and battle=:battle", Ship.class)
            .setParameter("battle", this)
            .executeUpdate();

        int[] points = new int[]{side1points, side2points};

        for (int i = 0; i < points.length; i++) {
            if (battle.getAllys()[i] != 0) {
                Ally ally = em.find(Ally.class, battle.getAllys()[i]);
                if (points[i] > 0) {
                    ally.setWonBattles((short) (ally.getWonBattles() + points[i]));
                } else {
                    ally.setLostBattles((short) (ally.getLostBattles() - points[i]));
                }
            }
            if (points[i] > 0) {
                battle.getCommanders()[i].setWonBattles((short) (battle.getCommanders()[i].getWonBattles() + points[i]));
            } else {
                battle.getCommanders()[i].setLostBattles((short) (battle.getCommanders()[i].getLostBattles() - points[i]));
            }
        }

        battle.getEnemyShips().clear();
        battle.getOwnShips().clear();

        em.remove(this);
    }

    /**
     * Zerstoert ein Schiff und alle an ihm gedockten Schiff.
     *
     * @param ship Das zu zerstoerende Schiff
     */
    private void destroyShip(Battle battle, BattleShip ship) {
        Context context = ContextMap.getContext();
        org.hibernate.Session db = context.getDB();

        if (!ship.getShip().isDestroyed()) {
            db.delete(ship);
            dismantlingService.destroy(ship.getShip());
        }

        //
        // Feststellen in welcher der beiden Listen sich das Schiff befindet und dieses daraus entfernen
        //

        List<BattleShip> shiplist = battle.getOwnShips();

        if (ship.getSide() != battle.getOwnSide()) {
            shiplist = battle.getEnemyShips();
        }

        for (int i = 0; i < shiplist.size(); i++) {
            BattleShip aship = shiplist.get(i);

            if (aship == ship) {
                shiplist.remove(i);
                break;
            }
        }

        if ((ship.getSide() == battle.getEnemySide()) && (battle.getActiveSEnemy() >= shiplist.size())) {
            battle.setActiveSEnemy(0);
        } else if ((ship.getSide() == battle.getOwnSide()) && (battle.getActiveSOwn() >= shiplist.size())) {
            battle.setActiveSOwn(0);
        }
    }

    /**
     * Fuegt einen Eintrag zum Schlachtlog hinzu.
     *
     * @param eintrag Der Eintrag
     */
    public void log(Battle battle, SchlachtLogEintrag eintrag) {
        int tick = configService.getValue(WellKnownConfigValue.TICKS);
        eintrag.setTick(tick);

        if (battle.getSchlachtLog() == null) {
            var log = new SchlachtLog(battle, tick);
            em.persist(log);
            battle.setSchlachtLog(log);
        }
        em.persist(eintrag);
        battle.getSchlachtLog().add(eintrag);
    }

    /**
     * @param user Der Spieler von dem die Nahrungsbalance berechnet werden soll.
     * @return Gibt die Nahrungsbalance der Schlacht zurueck.
     */
    public int getNahrungsBalance(Battle battle, User user) {
        int balance = 0;

        var battleFoodCost = em.createQuery("from Ship where owner=:owner and id>0 and battle=:battle", Ship.class)
            .setParameter("owner", user)
            .setParameter("battle", battle)
            .getResultStream()
            .mapToInt(shipService::getFoodConsumption)
            .sum();

        balance -= battleFoodCost;

        return balance;
    }

    /**
     * Loggt eine Nachricht fuer aktuellen Spieler.
     *
     * @param text Die zu loggende Nachricht
     */
    public void logme(Battle battle, String text) {
        battle.getLogoutputbuffer().append(text);
    }

    /**
     * @return Das Traegerschiff
     */
    public BattleShip getBaseShip(BattleShip battleShip) {
        //gucken, ob das BattleShiff ueberhaupt einen Traeger hat
        //erst umwandeln vom BattleShip in ein Ship
        Ship ship = battleShip.getShip();
        if (ship.isLanded() || ship.isDocked()) {
            //OK, es sollte also einen Traeger haben
            Ship baseShip = shipService.getBaseShip(ship);
            //sicherheitshalber auch hier nochmal eine Null abfangen
            if (baseShip != null) {
                int shipid = baseShip.getId();
                //Schiffe zum durchsuchen laden
                //dazu erstmal die Seite bestimmen
                int side = battleShip.getSide();
                List<BattleShip> ships;
                Battle battle = battleShip.getBattle();

                if (side == battle.getOwnSide()) {
                    ships = battle.getOwnShips();
                } else {
                    ships = battle.getEnemyShips();
                }

                for (BattleShip aship : ships) {
                    if (aship.getId() == shipid) {
                        return aship;
                    }
                }
                //nicht in der Schlacht gefunden
            }
            //kein Traegerschiff, komisch
        }
        //nicht gelandet oder gedockt, also kein Traegerschiff
        return null;
    }

    /**
     * Checks, if the ship is in the second row.
     *
     * @return true, if the ship is in the second row, false otherwise.
     */
    public boolean isSecondRow(BattleShip battleShip) {
        Ship aship = battleShip.getShip();
        if (aship != null) {
            if (aship.isDocked() || aship.isLanded()) {
                BattleShip traeger = getBaseShip(battleShip);
                if (traeger != null) {
                    return traeger.hasFlag(BattleShipFlag.SECONDROW);
                }
            }
        }
        return battleShip.hasFlag(BattleShipFlag.SECONDROW);
    }
}
