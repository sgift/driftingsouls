package net.driftingsouls.ds2.server.ships;

// Moves one or many ships (fleets)

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.repositories.ShipsRepository;

import java.util.List;

public class ShipsMovement {
    private List<MoveableShip> ships;
    private final int userid;
    public ShipsMovement(int shipid, int userid)
    {
        ships = ShipsRepository.getMoveableShips(shipid, userid);
        this.userid = userid;
    }

    SchiffFlugService.FlugStatus flugStatus = SchiffFlugService.FlugStatus.SUCCESS;
    public SchiffFlugService.FlugErgebnis MoveShips(Location destination)
    {
        // Don't need to move if we are already at the destination
        var start = ships.get(0).getLocation();
        if(start.getXYDistance(destination) == 0) return new SchiffFlugService.FlugErgebnis(flugStatus, "Schiffe bereits am Zielort!");

        // maxDistance is the smallest distance one of the ships can fly.
        int maxDistance = Integer.MAX_VALUE;
        for (var ship:ships) {
            maxDistance = Math.min(ship.computeFlight(destination), maxDistance);
        }

        // reduce maxDistance if we face an emp nebula, damage nebula or an alarm red
        maxDistance = Math.min(maxDistance, checkPath(destination));

        if(maxDistance == 0) return new SchiffFlugService.FlugErgebnis(flugStatus, "whatever.");

        // set the new values for the ships that have flown
        for (var ship:ships) {
            ship.Fly(maxDistance);
        }

        // update values in the database
        ShipsRepository.updateMovedShips(ships);

        var newLocation = ships.get(0).getLocation();
        return new SchiffFlugService.FlugErgebnis(flugStatus, "Siehe Flugstatus! ;)");
    }

    private int checkPath(Location destination)
    {
        var attackingSectors = ShipsRepository.getAttackingSectors(userid);
        var start = ships.get(0).getLocation();
        var lastLocation = start;
        var locations = start.getPathInSystem(destination);

        for (int i=0;i<locations.size();i++) {
            var location = locations.get(i);
            if(attackingSectors.contains(location)) { flugStatus = SchiffFlugService.FlugStatus.BLOCKED_BY_ALERT; break; }

            if(Nebel.getNebula(location) != null) {
                if (Nebel.getNebula(location).isEmp()) {
                    flugStatus = SchiffFlugService.FlugStatus.BLOCKED_BY_EMP;
                    break;
                }
                if (Nebel.getNebula(location).isDamage()) {
                    flugStatus = SchiffFlugService.FlugStatus.BLOCKED_BY_DAMAGE_NEBULA;
                    break;
                }
            }

            lastLocation = location;
        }

        return lastLocation.getXYDistance(start);
    }
}
