package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Alarmstufe;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.werften.WerftObject;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class FleetMgmtService {
    @PersistenceContext
    private EntityManager em;

    private final ShipService shipService;
    private final DismantlingService dismantlingService;

    public FleetMgmtService(ShipService shipService, DismantlingService dismantlingService) {
        this.shipService = shipService;
        this.dismantlingService = dismantlingService;
    }

    /**
     * Schickt die Flotte in die Werft zur Demontage.
     *
     * @param shipyard Werft, in der die Schiffe demontiert werden sollen.
     * @return <code>true</code>, wenn alle Schiffe demontiert wurden.
     */
    public boolean dismantleFleet(WerftObject shipyard, ShipFleet fleet) {
        Location shipyardLocation = shipyard.getLocation();
        List<Ship> ships = em.createQuery("from Ship where fleet=:fleet and system=:system and x=:x and y=:y", Ship.class)
            .setParameter("fleet", fleet)
            .setParameter("system", shipyardLocation.getSystem())
            .setParameter("x", shipyardLocation.getX())
            .setParameter("y", shipyardLocation.getY())
            .getResultList();
        int dismantled = dismantlingService.dismantleShips(shipyard, ships);
        return dismantled == ships.size();
    }

    /**
     * Gibt alle Schiffe der Flotte unabhaengig von Position/Besitzer zurueck.
     *
     * @return Die Schiffe
     */
    public List<Ship> getShips(ShipFleet shipFleet) {
        return em.createQuery("from Ship where fleet=:fleet", Ship.class)
            .setParameter("fleet", shipFleet)
            .getResultList();
    }

    /**
     * Entfernt ein Schiff aus der Flotte. Sollte die Flotte anschliessend zu wenige Schiffe haben
     * wird sie aufgeloesst.
     *
     * @param ship Das Schiff
     */
    public void removeShip(ShipFleet fleet, Ship ship) {
        if (fleet == null) {
            return;
        }

        if (!fleet.equals(ship.getFleet())) {
            throw new IllegalArgumentException("Das Schiff gehört nicht zu dieser Flotte");
        }

        long shipsInFleetCount = em.createQuery("select count(*) from Ship where fleet=:fleet and id>0", Long.class)
            .setParameter("fleet", fleet)
            .getSingleResult();

        if (shipsInFleetCount > 2 || fleet.isConsignMode()) {
            ship.setFleet(null);
            ShipFleet.MESSAGE.get().append("Das Schiff hat die Flotte verlassen");
        } else {
            em.createQuery("from Ship where fleet=:fleet", Ship.class)
                .setParameter("fleet", fleet)
                .getResultStream()
                .forEach(aship -> aship.setFleet(null));

            em.remove(fleet);
            ShipFleet.MESSAGE.get().append("Flotte aufgelöst");
        }
    }

    /**
     * Gibt den Besitzer der Flotte zurueck.
     *
     * @return Der Besitzer
     */
    public User getOwner(ShipFleet fleet) {
        return em.createQuery("select s.owner from Ship as s where s.id>0 and s.fleet=:fleet", User.class)
            .setParameter("fleet", fleet)
            .setMaxResults(1)
            .getSingleResult();
    }

    /**
     * Setzt die Alarmstufe der Schiffe in der Flotte.
     *
     * @param alarm Die Alarmstufe
     */
    public void setAlarm(ShipFleet fleet, Alarmstufe alarm) {
        List<Ship> ships = em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
            .setParameter("fleet", fleet)
            .getResultList();

        for (Ship ship : ships) {

            if ((ship.getTypeData().getShipClass() == ShipClasses.GESCHUETZ) || !ship.getTypeData().isMilitary()) {
                continue;
            }

            ship.setAlarm(alarm);
        }
    }

    /**
     * Fuegt alle Schiffe der Zielflotte dieser Flotte hinzu.
     *
     * @param fleetToAdd Die Zielflotte
     */
    public void joinFleet(ShipFleet currentFleet, ShipFleet fleetToAdd) {
        em.createQuery("update Ship set fleet=:fleet where id>0 and fleet=:fleetToAdd")
            .setParameter("fleet", currentFleet)
            .setParameter("fleetToAdd", fleetToAdd)
            .executeUpdate();

        // Problem i<0 beruecksichtigen - daher nur loeschen, wenn die Flotte auch wirklich leer ist
        long count = em.createQuery("select count(*) from Ship where fleet=:fleet", Long.class)
            .setParameter("fleet", fleetToAdd)
            .getSingleResult();
        if (count == 0) {
            em.remove(fleetToAdd);
        }
    }

    /**
     * Startet alle Jaeger der Flotte.
     */
    public void startFighters(ShipFleet fleet) {
        em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
            .setParameter("fleet", fleet)
            .getResultStream()
            .forEach(shipService::start);
    }

    /**
     * Dockt alle Container auf Schiffen der Flotte ab.
     */
    public void undockContainers(ShipFleet fleet) {
        em.createQuery("from Ship where id>0 and fleet=:fleet and battle is null", Ship.class)
            .setParameter("fleet", fleet)
            .getResultStream()
            .forEach(shipService::undock);
    }
}
