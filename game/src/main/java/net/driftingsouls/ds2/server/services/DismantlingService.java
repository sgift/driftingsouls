package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DSObject;
import net.driftingsouls.ds2.server.ships.SchiffHinzufuegenService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.tasks.Task;
import net.driftingsouls.ds2.server.tasks.TaskManager;
import net.driftingsouls.ds2.server.werften.ShipWerft;
import net.driftingsouls.ds2.server.werften.WerftObject;
import net.driftingsouls.ds2.server.werften.WerftQueueEntry;
import net.driftingsouls.ds2.server.werften.WerftTyp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dismantling of ships. Extracted to work around circular dependencies.
 */
@Service
public class DismantlingService {
    private static final Log log = LogFactory.getLog(ShipService.class);

    @PersistenceContext
    private EntityManager em;

    private final ShipyardService shipyardService;
    private final ShipService shipService;
    private final ShipActionService shipActionService;
    private final TaskManager taskManager;
    private final CargoService cargoService;
    private final SchiffHinzufuegenService schiffHinzufuegenService;

    public DismantlingService(ShipyardService shipyardService, ShipService shipService, ShipActionService shipActionService, TaskManager taskManager, CargoService cargoService, SchiffHinzufuegenService schiffHinzufuegenService) {
        this.shipyardService = shipyardService;
        this.shipService = shipService;
        this.shipActionService = shipActionService;
        this.taskManager = taskManager;
        this.cargoService = cargoService;
        this.schiffHinzufuegenService = schiffHinzufuegenService;
    }

    /**
     * Entfernt das Schiff aus der Datenbank.
     */
    public void destroy(Ship ship) {
        // Checken wir mal ob die Flotte danach noch bestehen darf....
        if( ship.getFleet() != null ) {
            long fleetcount = em.createQuery("select count(*) from Ship where fleet=:fleet", Long.class)
                .setParameter("fleet", ship.getFleet().getId())
                .getSingleResult();
            if( fleetcount <= 2 ) {
                final ShipFleet fleet = ship.getFleet();

                List<Ship> ships = em.createQuery("from Ship where fleet=:fleet", Ship.class)
                    .setParameter("fleet", ship.getFleet())
                    .getResultList();
                for(Ship aship: ships) {
                    aship.setFleet(null);
                }

                em.remove(fleet);
            }
        }

        // Ist das Schiff selbst gedockt? -> Abdocken
        if(ship.getDocked() != null && !ship.getDocked().equals("") && (ship.getDocked().charAt(0) != 'l') ) {
            Ship docked = em.find(Ship.class, Integer.parseInt(ship.getDocked()));
            if(docked != null)
            {
                shipService.undock(docked, ship);
            }
            else
            {
                log.debug("Docked entry of ship was illegal: " + ship.getDocked());
            }
        }

        // Evt. gedockte Schiffe abdocken
        ShipTypeData type = ship.getTypeData();
        if( type.getADocks() != 0 ) {
            shipService.undock(ship);
        }
        if( type.getJDocks() != 0 ) {
            shipService.start(ship);
        }

        // Gibts bereits eine Loesch-Task? Wenn ja, dann diese entfernen
        Task[] tasks = taskManager.getTasksByData( TaskManager.Types.SHIP_DESTROY_COUNTDOWN, Integer.toString(ship.getId()), "*", "*");
        for (Task task : tasks)
        {
            taskManager.removeTask(task.getTaskID());
        }

        // Und nun loeschen wir es...
        ship.clearFlags();

        em.createQuery("delete from Offizier where stationiertAufSchiff=:dest")
            .setParameter("dest", ship)
            .executeUpdate();

        em.createQuery("delete from Jump where ship=:id")
            .setParameter("id", ship)
            .executeUpdate();

        Optional<ShipWerft> werft = em.createQuery("from ShipWerft where ship=:ship", ShipWerft.class)
            .setParameter("ship", ship)
            .getResultStream()
            .findAny();
        werft.ifPresent(shipyardService::destroyShipyard);

        // Delete Trade Limits if necessary
        if (ship.isTradepost())
        {
            em.createQuery("delete from ResourceLimit where ship=:ship").setParameter("ship", ship).executeUpdate();
            em.createQuery("delete from SellLimit where ship=:ship").setParameter("ship", ship).executeUpdate();
        }

        if(ship.getUnitCargo() != null)
        {
            ship.getUnitCargo().getUnitList().forEach(em::remove);
        }

        if( ship.getEinstellungen() != null )
        {
            em.remove(ship.getEinstellungen());
            ship.setEinstellungen(null);
        }

        em.flush(); //Damit auch wirklich alle Daten weg sind und Hibernate nicht auf dumme Gedanken kommt *sfz*
        em.remove(ship);

        ship.setDestroyed(true);
    }

    /**
     * Demontiert ein Schiff. Es wird dabei nicht ueberprueft, ob sich Schiff
     * und Werft im selben Sektor befinden, ob das Schiff in einem Kampf ist usw sondern
     * nur das demontieren selbst.{@link DSObject#MESSAGE} enthaelt die Hinweistexte
     *
     * @param ship Das zu demontierende Schiff
     * @param testonly Soll nur geprueft (true) oder wirklich demontiert werden (false)?
     * @return true, wenn kein Fehler aufgetreten ist
     */
    public boolean dismantleShip(@NonNull WerftObject werftObject, @NonNull Ship ship, boolean testonly) {
        if(werftObject.isEinwegWerft())
        {
            werftObject.MESSAGE.get().append("Diese Werft ist vollständig auf ihr einziges Bauprojekt konzentriert.");
            return false;
        }
        StringBuilder output = werftObject.MESSAGE.get();

        if( ship.getId() < 0 ) {
            ContextMap.getContext().addError("Das angegebene Schiff existiert nicht");
            return false;
        }

        if(werftObject instanceof ShipWerft)
        {
            Ship shipyard = ((ShipWerft)werftObject).getShip();
            if(ship.equals(shipyard)) {
                return false;
            }
        }

        Cargo scargo = ship.getCargo();

        Cargo cargo = werftObject.getCargo(false);

        long maxcargo = werftObject.getMaxCargo(false);

        Cargo cost = werftObject.getDismantleCargo( ship );

        Cargo newcargo = cargo.clone();
        long totalcargo = cargoService.getMass(cargo);

        boolean ok = true;

        cost.addCargo( scargo );
        newcargo.addCargo( cost );

        if( cargoService.getMass(cost) + totalcargo > maxcargo ) {
            output.append("Nicht gen&uuml;gend Platz f&uuml;r alle Waren\n");
            ok = false;
        }

        int maxoffis = werftObject.canTransferOffis();

        Set<Offizier> offiziere = ship.getOffiziere();

        if( offiziere.size() > maxoffis ) {
            output.append("Nicht genug Platz f&uuml;r alle Offiziere");
            ok = false;
        }
        if( !ok ) {
            return false;
        }

        if(!testonly) {
            shipyardService.setCargo(werftObject, newcargo, false);

            werftObject.setCrew(werftObject.getCrew()+ship.getCrew());
            if(werftObject.getCrew() > werftObject.getMaxCrew())
            {
                werftObject.setCrew(werftObject.getMaxCrew());
            }

            //Iterate over copy or we get ConcurrentModificationException when officer is moved out of original set
            Set<Offizier> dockyardOfficers = new HashSet<>(offiziere);
            for( Offizier offi : dockyardOfficers ) {
                werftObject.transferOffi(offi.getID());
            }

            destroy(ship);

            if(werftObject instanceof ShipWerft) {
                shipActionService.recalculateShipStatus(((ShipWerft) werftObject).getShip());
            }
        }

        return true;
    }

    /**
     * Demontiert mehrere Schiffe auf einmal.
     *
     * @param ships Schiffe, die demontiert werden sollten.
     * @return Anzahl der Schiffe, die wirklich demontiert wurde.
     */
    public int dismantleShips(@NonNull WerftObject werftObject, @NonNull Collection<Ship> ships)
    {
        int dismantledShips = 0;
        for(Ship ship: ships)
        {
            if(dismantleShip(werftObject, ship, false))
            {
                dismantledShips++;
            }
        }

        return dismantledShips;
    }

    /**
     * Beendet den Bauprozess dieses Bauschlangeneintrags erfolgreich.
     * Sollte dies nicht moeglich sein, wird 0 zurueckgegeben.
     *
     * @return die ID des gebauten Schiffes oder 0
     */
    public int finishBuildProcess(WerftQueueEntry entry) {
        entry.MESSAGE.get().setLength(0);

        if( !entry.isScheduled() ) {
            return 0;
        }

        ShipType shipd = entry.getBuildShipType();

        User auser = entry.getWerft().getOwner();

        Ship ship = schiffHinzufuegenService.erstelle(auser, shipd, entry.getWerft().getLocation());

        // Item benutzen
        if( entry.getRequiredItem() > -1 ) {
            Cargo cargo = entry.getWerft().getCargo(true);

            ItemCargoEntry<Item> item = null;
            boolean hasUses = true;
            // Check if the shipyard has the required item
            List<ItemCargoEntry<Item>> itemlist = cargo.getItem(entry.getRequiredItem());
            for (ItemCargoEntry<Item> anItemlist : itemlist) {
                item = anItemlist;
                if (anItemlist.getMaxUses() == 0) {
                    hasUses = false;
                    break;
                }
            }

            // Required item not found on shipyard, maybe it exists in alliance cargo?
            if(item == null) {
                User user = entry.getWerft().getOwner();
                if(user.getAlly() != null) {
                    Cargo allyitems = new Cargo( Cargo.Type.ITEMSTRING, user.getAlly().getItems() );
                    itemlist = allyitems.getItem(entry.getRequiredItem());
                    for (ItemCargoEntry<Item> anItemlist : itemlist) {
                        item = anItemlist;
                        if (anItemlist.getMaxUses() == 0)
                        {
                            hasUses = false;
                            break;
                        }
                    }
                }
            }

            //Required item not found
            if(item == null) {
                return 0;
            }

            if(hasUses) {
                item.useItem();
            }
        }

        if( entry.isBuildFlagschiff() ) {
            entry.getWerft().setBuildFlagschiff(false);
        }

        em.remove(entry);
        entry.getWerft().removeQueueEntry(entry);

        var successors = em.createQuery("from WerftQueueEntry where werft=:werft and position>:pos order by position", WerftQueueEntry.class)
            .setParameter("werft", entry.getWerft())
            .setParameter("pos", entry.getPosition())
            .getResultList();

        for(WerftQueueEntry successor: successors) {
            successor.setPosition(successor.getPosition() - 1);
        }

        onFinishedBuildProcess(entry.getWerft());

        return ship.getId();
    }

    /**
     * Wird von Bauschlangeneintraegen aufgerufen, nachdem sie erfolgreich abgeschlossen wurden.
     * Der Bauschlangeneintrag ist zu diesem Zeitpunkt bereits entfernt.
     */
    //TODO: This really should not be here, but is the only way for the moment to make sure we don't have
    //circular dependencies between DismantlingService and ShipyardService
    private void onFinishedBuildProcess(WerftObject werft) {
        werft.rescheduleQueue();

        if(werft instanceof ShipWerft) {
            // Falls es sich um eine Einwegwerft handelt, dann diese zerstoeren
            if( werft.getType() == WerftTyp.EINWEG ) {
                destroy(((ShipWerft) werft).getShip());
            }
        }
    }
}
