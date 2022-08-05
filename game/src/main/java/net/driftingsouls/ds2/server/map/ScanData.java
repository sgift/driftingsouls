package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;

public class ScanData implements Locatable {
    private final Location location;
    private final int shipId;
    private final int ownerId;
    private final int scanRange;

    public ScanData(int system, int x, int y, int shipId, int ownerId, int scanRange) {
        this.location = new Location(system, x, y);
        this.shipId = shipId;
        this.ownerId = ownerId;
        this.scanRange = scanRange;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }


    public int getScanRange() {
        return scanRange;
    }

    public int getShipId() {
        return shipId;
    }

    public int getOwnerId() {
        return ownerId;
    }
}
