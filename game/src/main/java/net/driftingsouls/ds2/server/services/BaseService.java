package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class BaseService {
    @PersistenceContext
    private EntityManager em;

    private final UserService userService;
    private final ShipActionService shipActionService;

    public BaseService(UserService userService, ShipActionService shipActionService) {
        this.userService = userService;
        this.shipActionService = shipActionService;
    }

    /**
     * Gibt das userspezifische Bild der Basis zurueck. Falls es kein spezielles Bild
     * fuer den angegebenen Benutzer gibt wird <code>null</code> zurueckgegeben.
     *
     * @param base Die Basis für die ein Bild erstellt werden soll.
     * @param location Koordinate fuer die das Bild der Basis ermittelt werden soll.
     * @param user Aktueller Spieler.
     * @param scanned <code>true</code>, wenn die Basis derzeit von einem Schiff des Spielers gescannt werden kann.
     * @return Der Bildstring der Basis oder <code>null</code>
     */
    public String getOverlayImage(Base base, Location location, User user, boolean scanned)
    {
        if(!location.sameSector(0, base.getLocation(), base.getSize()))
        {
            return null;
        }

        User nobody = em.find(User.class, -1);
        User zero = em.find(User.class, 0);

        if(base.getSize() > 0)
        {
            return null;
        }
        else if(base.getOwner().getId() == user.getId())
        {
            return "data/starmap/asti_own/asti_own.png";
        }
        else if(((base.getOwner().getId() != 0) && (user.getAlly() != null) && (base.getOwner().getAlly() == user.getAlly()) && user.getAlly().getShowAstis()) ||
            userService.getRelations(user).isOnly(base.getOwner(), User.Relation.FRIEND))
        {
            return "data/starmap/asti_ally/asti_ally.png";
        }
        else if(scanned && !base.getOwner().equals(nobody) && !base.getOwner().equals(zero))
        {
            return "data/starmap/asti_enemy/asti_enemy.png";
        }
        else
        {
            return null;
        }
    }

    /**
     * Transfers crew from the asteroid to a ship.
     *
     * @param ship Ship that gets the crew.
     * @param amount People that should be transfered.
     * @return People that where transfered.
     */
    public int transferCrew(Base base, Ship ship, int amount)
    {
        //Check ship position
        if( !base.getLocation().sameSector(base.getSize(), ship, 0))
        {
            return 0;
        }

        //Only unemployed people can be transferred, when there is enough space on the ship
        int maxAmount = ship.getTypeData().getCrew() - ship.getCrew();
        int unemployed = Math.max(base.getBewohner() - base.getArbeiter(), 0);
        amount = Math.min(amount, maxAmount);
        amount = Math.min(amount, unemployed);

        base.setBewohner(base.getBewohner() - amount);
        ship.setCrew(ship.getCrew() + amount);
        shipActionService.recalculateShipStatus(ship);

        return amount;
    }
}
