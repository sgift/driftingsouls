package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;

public class ScanData implements Locatable {
    private final int system;
    private final int x;
    private final int y;
    private final int scanRange;

    public ScanData(int system, int x, int y, int scanRange) {
        this.system = system;
        this.x = x;
        this.y = y;
        this.scanRange = scanRange;
    }

    @Override
    public Location getLocation() {
        return new Location(system, x, y);
    }


    public int getScanRange() {
        return scanRange;
    }
}
