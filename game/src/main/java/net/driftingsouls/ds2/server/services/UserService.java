package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.WellKnownConfigValue;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.comm.Ordner;
import net.driftingsouls.ds2.server.config.Medal;
import net.driftingsouls.ds2.server.config.Rang;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.entities.UserRelation;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ConfigService;
import net.driftingsouls.ds2.server.framework.bbcode.BBCodeParser;
import net.driftingsouls.ds2.server.namegenerator.PersonenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsKlassenNamenGenerator;
import net.driftingsouls.ds2.server.namegenerator.SchiffsNamenGenerator;
import net.driftingsouls.ds2.server.ships.Alarmstufe;
import net.driftingsouls.ds2.server.ships.SchiffsReKosten;
import net.driftingsouls.ds2.server.ships.Ship;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
public class UserService {
    private static final Log log = LogFactory.getLog(UserService.class);

    @PersistenceContext
    private EntityManager em;

    private final Rassen races;
    private final FolderService folderService;
    private final MedalService medalService;
    private final BBCodeParser bbCodeParser;
    private final AllianzService allyService;
    private final ConfigService configService;
    private final ShipService shipService;

    public UserService(Rassen races, FolderService folderService, MedalService medalService, BBCodeParser bbCodeParser, AllianzService allyService, ConfigService configService, ShipService shipService) {
        this.races = races;
        this.folderService = folderService;
        this.medalService = medalService;
        this.bbCodeParser = bbCodeParser;
        this.allyService = allyService;
        this.configService = configService;
        this.shipService = shipService;
    }

    /**
     * Liefert alle Beziehungen vom Spieler selbst zu anderen Spielern und umgekehrt.
     *
     * @return Ein Objekt mit Beziehung von diesem Spieler zu anderen und von anderen zu diesem Spieler.
     * Beziehungen zu Spieler 0 betreffen alle Spieler ohne eigene Regelung
     */
    public Relations getRelations(User user) {
        Relations relations = new Relations(user);
        Map<User, User.Relation> defaults = new HashMap<>();

        List<UserRelation> relationlist = em.createQuery("from UserRelation " +
            "where user= :user OR target= :user OR (user!= :user AND target.id=0) " +
            "order by abs(target.id) desc", UserRelation.class)
            .setParameter("user", user)
            .getResultList();

        for (UserRelation relation : relationlist) {
            if (relation.getUser().getId() == user.getId()) {
                relations.toOther.put(relation.getTarget().getId(), User.Relation.values()[relation.getStatus()]);
            } else if (relation.getTarget().getId() == 0) {
                defaults.put(relation.getUser(), User.Relation.values()[relation.getStatus()]);
            } else {
                relations.fromOther.put(relation.getUser().getId(), User.Relation.values()[relation.getStatus()]);
            }
        }

        for (Map.Entry<User, User.Relation> userRelationEntry : defaults.entrySet()) {
            if (relations.fromOther.containsKey(userRelationEntry.getKey().getId())) {
                continue;
            }
            relations.fromOther.put(userRelationEntry.getKey().getId(), userRelationEntry.getValue());
        }


        if (!relations.toOther.containsKey(0)) {
            relations.toOther.put(0, User.Relation.NEUTRAL);
        }

        relations.toOther.put(user.getId(), User.Relation.FRIEND);
        relations.fromOther.put(user.getId(), User.Relation.FRIEND);

        Relations rel = new Relations(user);
        rel.fromOther.putAll(relations.fromOther);
        rel.toOther.putAll(relations.toOther);
        return rel;
    }

    /**
     * Gibt den Status der Beziehung des Spielers zu einem anderen Spieler zurueck.
     *
     * @param user Der andere Spieler oder <code>null</code>, falls die Standardbeziehung abgefragt werden soll
     * @return Der Status der Beziehungen zu dem anderen Spieler
     */
    public User.Relation getRelation(User user, User otherUser) {
        if (user == otherUser) {
            return User.Relation.FRIEND;
        }

        if (otherUser == null) {
            otherUser = em.find(User.class, 0);
        }

        if (user.getAlly() != null && user.getAlly().getMembers().contains(otherUser)) {
            return User.Relation.FRIEND;
        }


        UserRelation currelation = em.createQuery("from UserRelation WHERE user=:user AND target=:userid", UserRelation.class)
            .setParameter("user", user.getId())
            .setParameter("userid", user.getId())
            .getSingleResult();

        if (currelation == null) {
            currelation = em.createQuery("from UserRelation WHERE user=:user AND target.id=0", UserRelation.class)
                .setParameter("user", user.getId())
                .getSingleResult();
        }

        User.Relation rel = User.Relation.NEUTRAL;
        if (currelation != null) {
            rel = User.Relation.values()[currelation.getStatus()];
        }
        return rel;
    }

    /**
     * Setzt die Beziehungen des Spielers zu einem anderen Spieler auf den angegebenen
     * Wert.
     *
     * @param user       Ein erster Spieler
     * @param targetuser Ein zweiter Spieler
     * @param relation   Der neue Status der Beziehungen
     */
    public void setRelation(User user, User targetuser, User.Relation relation) {
        if (user.getId() == targetuser.getId()) {
            return;
        }

        Optional<UserRelation> currelation = em.createQuery("from UserRelation WHERE user=:user AND target=:target", UserRelation.class)
            .setParameter("user", user)
            .setParameter("target", targetuser)
            .getResultStream().findAny();
        if (targetuser.getId() != 0) {
            if ((relation != User.Relation.FRIEND) && (user.getAlly() != null)) {
                if (targetuser.getAlly() == user.getAlly()) {
                    log.warn("Versuch die allyinterne Beziehung von User " + user.getId() + " zu " + targetuser.getId() + " auf " + relation + " zu aendern", new Throwable());
                    return;
                }
            }
            Optional<UserRelation> possibleDefRelation = em.createQuery("from UserRelation WHERE user=:user AND target.id=0", UserRelation.class)
                .setParameter("user", user)
                .getResultStream().findAny();

            UserRelation defrelation = possibleDefRelation.orElseGet(() -> {
                User nullUser = em.find(User.class, 0);
                return new UserRelation(user, nullUser, User.Relation.NEUTRAL.ordinal());
            });

            if (relation.ordinal() == defrelation.getStatus()) {
                currelation.filter(rel -> rel.getTarget().getId() != 0).ifPresent(rel -> em.remove(rel));
            } else {
                currelation.ifPresentOrElse(rel -> rel.setStatus(relation.ordinal()),
                    () -> {
                        var newRelation = new UserRelation(user, targetuser, relation.ordinal());
                        em.persist(newRelation);
                    });
            }
        } else {
            if (relation == User.Relation.NEUTRAL) {
                em.createQuery("delete from UserRelation where user=:user and target.id=0")
                    .setParameter("user", user)
                    .executeUpdate();
            } else {
                currelation.ifPresentOrElse(rel -> rel.setStatus(relation.ordinal()),
                    () -> {
                        User nullUser = em.find(User.class, 0);
                        var newRelation = new UserRelation(user, nullUser, relation.ordinal());
                        em.persist(newRelation);
                    });
            }
            em.createQuery("delete from UserRelation where user=:user and status=:status AND target.id!=0")
                .setParameter("user", user)
                .setParameter("status", relation.ordinal())
                .executeUpdate();
        }
    }

    public Map<Location, List<Ship>> alertCheck(User user, Location ... locs )
    {
        Set<Integer> xSektoren = new HashSet<>();
        Set<Integer> ySektoren = new HashSet<>();

        Map<Location,List<Ship>> results = new HashMap<>();
        Set<Location> locations = new HashSet<>();
        for(Location location: locs)
        {
            results.put(location, new ArrayList<>());
            locations.add(location);
            xSektoren.add(location.getX());
            ySektoren.add(location.getY());
        }

        if(locations.isEmpty())
        {
            return results;
        }

        List<Ship> ships = em.createQuery("from Ship s inner join fetch s.owner " +
            "where s.e > 0 and s.alarm!=:green and s.docked='' and " +
            "	s.crew!=0 and s.system=:system and s.owner!=:owner and " +
            "   (s.owner.vaccount=0 or s.owner.wait4vac>0) and " +
            "	s.x in :xSektoren and s.y in :ySektoren", Ship.class)
            .setParameter("green", Alarmstufe.GREEN)
            .setParameter("system", locs[0].getSystem())
            .setParameter("owner", user)
            .setParameter("xSektoren", xSektoren)
            .setParameter("ySektoren", ySektoren)
            .getResultList();

        Relations relationlist = getRelations(user);
        for(Ship ship: ships)
        {
            Location location = ship.getLocation();
            if(!locations.contains(location))
            {
                continue;
            }

            User owner = ship.getOwner();
            Alarmstufe alert = ship.getAlarm();
            boolean attack = false;
            if(alert == Alarmstufe.RED)
            {
                if(relationlist.beziehungVon(owner) != User.Relation.FRIEND)
                {
                    attack = true;
                }
                else if(relationlist.beziehungZu(owner) != User.Relation.FRIEND)
                {
                    attack = true;
                }
            }
            else if(alert == Alarmstufe.YELLOW)
            {
                if(relationlist.beziehungVon(owner) == User.Relation.ENEMY)
                {
                    attack = true;
                }
            }

            if(attack)
            {
                results.get(location).add(ship);
            }
        }

        return results;
    }

    /**
     * Gibt eine Liste von booleans zurueck, welche angeben ob ein angegebener Sektor fuer den angegebenen Spieler
     * unter Alarm steht, d.h. bei einem Einflug eine Schlacht gestartet wird.
     * Die Reihenfolge der Liste entspricht der der uebergebenen Koordinaten. <code>true</code> kennzeichnet,
     * dass der Sektor unter Alarm steht.
     * @param user Der Spieler
     * @param locs Die Positionen, die ueberprueft werden sollen
     * @return Liste von Sektoren mit rotem Alarm
     */
    public Set<Location> getAlertStatus( User user, Location ... locs ) {
        return alertCheck(user, locs).entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(toSet());
    }

    /**
     * Klasse, welche die Beziehungen eines Spielers zu anderen
     * Spielern enthaelt.
     *
     * @author Christopher Jung
     */
    public static class Relations {
        /**
         * Die Beziehungen des Spielers zu anderen Spielern.
         * Schluessel ist die Spieler-ID
         */
        protected final Map<Integer, User.Relation> toOther = new HashMap<>();
        /**
         * Die Beziehungen von anderen Spielern zum Spieler selbst.
         * Schluessel ist die Spieler-ID.
         */
        protected final Map<Integer, User.Relation> fromOther = new HashMap<>();

        private final User user;

        public Relations(User user) {
            this.user = user;
        }

        /**
         * Gibt zurueck, ob die Beziehung zu einem gegebenen anderen Spieler
         * in beide Richtungen den angegebenen Beziehungtyp hat.
         *
         * @param otherUser Der andere Spieler
         * @param relation  Der Beziehungstyp
         * @return <code>true</code>, falls in beide Richtungen der Beziehungstyp gilt
         */
        public boolean isOnly(User otherUser, User.Relation relation) {
            return beziehungZu(otherUser) == relation && beziehungVon(otherUser) == relation;
        }

        /**
         * Gibt die Beziehung des Spielers zu einem anderen Spieler zurueck.
         *
         * @param otherUser Der andere Spieler
         * @return Der Beziehungstyp
         */
        public User.Relation beziehungZu(User otherUser) {
            if (user.getAlly() != null && user.getAlly().getMembers().contains(otherUser)) {
                // Allianzen sind immer befreundet
                return User.Relation.FRIEND;
            }

            User.Relation relation = this.toOther.get(otherUser.getId());
            if (relation != null) {
                return relation;
            }
            relation = this.toOther.get(0);
            return Objects.requireNonNullElse(relation, User.Relation.NEUTRAL);
        }

        /**
         * Gibt die Beziehung eines anderen Spielers zu diesem Spieler zurueck.
         *
         * @param otherUser Der andere Spieler
         * @return Der Beziehungstyp
         */
        public User.Relation beziehungVon(User otherUser) {
            if (user.getAlly() != null && user.getAlly().getMembers().contains(otherUser)) {
                // Allianzen sind immer befreundet
                return User.Relation.FRIEND;
            }

            User.Relation relation = this.fromOther.get(otherUser.getId());
            return Objects.requireNonNullElse(relation, User.Relation.NEUTRAL);
        }
    }

    /**
     * Gibt den vom Spieler verwendeten Generator fuer Personenanmen zurueck.
     * @return Der Generator
     */
    public PersonenNamenGenerator getPersonenNamenGenerator(User user)
    {
        return user.getPersonenNamenGenerator() == null ? races.rasse(user.getRace()).getPersonenNamenGenerator() : user.getPersonenNamenGenerator();
    }

    /**
     * Setzt den vom Spieler verwendeten Generator fuer Personenanmen.
     * @param personenNamenGenerator Der Generator
     */
    public void setPersonenNamenGenerator(User user, PersonenNamenGenerator personenNamenGenerator)
    {
        var generator = races.rasse(user.getRace()).getPersonenNamenGenerator() == personenNamenGenerator ? null : personenNamenGenerator;
        user.setPersonenNamenGenerator(generator);
    }

    /**
     * Gibt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe zurueck.
     * @return Der Generator
     */
    public SchiffsKlassenNamenGenerator getSchiffsKlassenNamenGenerator(User user)
    {
        return user.getSchiffsKlassenNamenGenerator() == null ? races.rasse(user.getRace()).getSchiffsKlassenNamenGenerator() : user.getSchiffsKlassenNamenGenerator();
    }

    /**
     * Setzt den vom Spieler verwendeten Generator fuer Schiffsklassen-Prefixe.
     * @param schiffsKlassenNamenGenerator Der Generator
     */
    public void setSchiffsKlassenNamenGenerator(User user, SchiffsKlassenNamenGenerator schiffsKlassenNamenGenerator)
    {
        user.setSchiffsKlassenNamenGenerator(races.rasse(user.getRace()).getSchiffsKlassenNamenGenerator() == schiffsKlassenNamenGenerator ? null : schiffsKlassenNamenGenerator);
    }

    /**
     * Gibt den vom Spieler verwendeten Generator fuer Schiffsnamen zurueck.
     * @return Der Generator
     */
    public SchiffsNamenGenerator getSchiffsNamenGenerator(User user)
    {
        return user.getSchiffsNamenGenerator() == null ? races.rasse(user.getRace()).getSchiffsNamenGenerator() : user.getSchiffsNamenGenerator();
    }

    /**
     * Setzt den vom Spieler verwendeten Generator fuer Schiffsnamen.
     * @param schiffsNamenGenerator Der Generator
     */
    public void setSchiffsNamenGenerator(User user, SchiffsNamenGenerator schiffsNamenGenerator)
    {
        user.setSchiffsNamenGenerator(races.rasse(user.getRace()).getSchiffsNamenGenerator() == schiffsNamenGenerator ? null : schiffsNamenGenerator);
    }

    /**
     * <p>Ermittelt zu einem gegebenen Identifier den Benutzer. Ein Identifier
     * kann die ID des Benutzers oder sein (unformatierter) Name sein.
     * Beim Namen werden auch teilweise Matches beruecksichtigt.</p>
     * <p>Es wird nur dann ein User-Objekt zurueckgegeben, wenn
     * zu dem gegebenen Identifier genau ein Benutzer ermittelt
     * werden kann (Eindeutigkeit).</p>
     * @param identifier Der Identifier
     * @return Der passende Benutzer oder <code>null</code>
     */
    public User lookupByIdentifier(String identifier)
    {
        if( identifier.isEmpty() )
        {
            return null;
        }
        if( NumberUtils.isCreatable(identifier) )
        {
            try
            {
                User user = em.find(User.class, Integer.parseInt(identifier));
                if( user != null && user.getId() != 0 )
                {
                    return user;
                }
            }
            catch( NumberFormatException e )
            {
                // Keine gueltige ID - anders weiter versuchen
            }
        }

        List<User> users = em.createQuery("select u from User u where plainname like :name and id<>0", User.class)
                .setParameter("name", "%"+identifier+"%")
                .setMaxResults(2)
                .getResultList();

        if( users.size() == 1 )
        {
            // Nur bei Eindeutigkeit den User zurueckgeben
            // um "Unfaelle" zu vermeiden
            return users.get(0);
        }

        return null;
    }

    public void setupTrash(User user) {
        Ordner trash = folderService.createNewOrdner("Papierkorb", folderService.getOrdnerByID(0, user), user);
        trash.setFlags(Ordner.FLAG_TRASH);
    }

    /**
     * Gibt die Liste aller Orden und Auszeichnungen des Spielers zurueck.
     * Die einzelnen Orden-IDs sind mittels ; verbunden
     * @return Die Liste aller Orden
     */
    public Set<Medal> getMedals(User user) {
        int[] medals = Common.explodeToInt(";", user.getMedals());

        return Arrays.stream(medals).boxed().map(medalService::medal).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Gibt alle durch den NPC vergebbaren Raenge zurueck.
     * @return Die Raenge
     */
    public SortedSet<Rang> getOwnGrantableRanks(User user)
    {
        if( user.getAlly() != null )
        {
            return allyService.getFullRangNameList(user.getAlly());
        }
        return new TreeSet<>(medalService.raenge().values());
    }

    /**
     * Gibt einen durch den NPC vergebbaren Rang zurueck. Falls der Rang unbekannt ist
     * wird <code>null</code> zurueckgegeben.
     * @param rank Die Nummer des Rangs
     * @return Der Rang oder <code>null</code>
     */
    public Rang getOwnGrantableRank(User user, int rank)
    {
        for( Rang r : getOwnGrantableRanks(user) )
        {
            if( r.getId() == rank )
            {
                return r;
            }
        }
        return null;
    }

    /**
     * Liefert einen Profile-Link zu den Benutzer zurueck (als HTML).
     * Als CSS-Klasse fuer den Link wird die angegebene Klasse verwendet.
     * @param username Der anzuzeigende Spielername
     * @return Der Profile-Link
     */
    public String getProfileLink(User user, String username) {
        if( username == null || username.equals("") ) {
            username = Common._title(bbCodeParser, user.getName());
        }

        return "<a class=\"profile\" href=\""+Common.buildUrl("default", "module", "userprofile", "user", user.getId())+"\">"+username+"</a>";
    }

    /**
     * Liefert einen vollstaendigen Profile-Link zu den Benutzer zurueck (als HTML).
     * Der Linkt enthaelt einen &lt;a&gt;-Tag sowie den Benutzernamen als HTML.
     * @return Der Profile-Link
     */
    public String getProfileLink(User user) {
        return getProfileLink(user, "");
    }

    /**
     * Prueft, ob der Spieler noch unter Noob-Schutz steht.
     * @return <code>true</code>, falls der Spieler noch ein Noob ist
     */
    public boolean isNoob(User user) {
        return configService.getValue(WellKnownConfigValue.NOOB_PROTECTION) && user.hasFlag(UserFlag.NOOB);
    }

    /**
     * Gibt die Nahrungs- und RE-Bilanz zurueck.
     * @return die Bilanzen
     */
    public long[] getFullBalance(User user)
    {
        return new long[] {
            !user.hasFlag(UserFlag.NO_FOOD_CONSUMPTION) ? this.getNahrungBalance(user) : 0,
            !user.hasFlag(UserFlag.NO_DESERTEUR) ? getReBalance(user) : 0,
        };
    }

    /**
     * Gibt die RE-Bilanz zurueck. Ein negativer Wert bedeutet,
     * dass der Benutzer jeden Tick RE Zahlen muss. Ein positiver,
     * dass er jeden Tick RE erwirtschaftet.
     * @return die Bilanzen in RE
     */
    public long getReBalance(User user)
    {
        int baseRe = 0;
        for(Base base: user.getBases())
        {
            baseRe += base.getBalance();
        }

        // Kosten der Schiffe ermitteln
        long schiffsKosten = em.createQuery("select sum(coalesce(sm.reCost,st.reCost)) " +
                "from Ship s join s.shiptype st left join s.modules sm " +
                "where s.owner=:user and s.docked not like 'l %'", Long.class)
            .setParameter("user", user)
            .getSingleResult();

        // Kosten der auf den Schiffen stationierten Einheiten ermitteln
        long einheitenKosten = em.createQuery("select sum(ceil(u.amount*u.unittype.recost)) " +
                "from Ship s join s.units u "+
                "where s.owner=:user and s.docked not like 'l %'", Long.class)
            .setParameter("user", this)
            .getSingleResult();

        return baseRe - SchiffsReKosten.berecheKosten(schiffsKosten, einheitenKosten).longValue();
    }

    public long getNahrungBalance(User user)
    {
        long balance = 0;
        for(Base base: user.getBases())
        {
            balance += base.getNahrungsBalance();
        }

        for( Ship ship : user.getShips() )
        {
            if( ship.getId() <= 0 )
            {
                continue;
            }
            if( ship.getBattle() != null )
            {
                continue;
            }
            balance -= shipService.getFoodConsumption(ship);
        }

        return balance;
    }
}


