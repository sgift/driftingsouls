package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;

public class FleetsOverviewView {
    private final int id;
    private final String name;
    private final Location location;
    private final int shipCount;
    private final boolean isLandedFleet;
    private final int fleetShipId;

    public FleetsOverviewView(int id, String name, int fleetShipId, int system, int x, int y, int shipCount, String docked) {
        this.id = id;
        this.name = name;
        this.fleetShipId = fleetShipId;
        this.shipCount = shipCount;
        this.isLandedFleet = docked != null && docked.startsWith("l");
        this.location = new Location(system, x, y);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public int getShipCount() {
        return shipCount;
    }

    public boolean isLandedFleet() {
        return isLandedFleet;
    }

    public int getFleetShipId() {
        return fleetShipId;
    }
}
