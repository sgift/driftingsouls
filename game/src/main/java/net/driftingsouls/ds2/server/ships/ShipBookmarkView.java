package net.driftingsouls.ds2.server.ships;

import net.driftingsouls.ds2.server.Location;

public class ShipBookmarkView {
    private final int id;
    private final Location location;
    private final Location destLocation;
    private final String name;
    private final String typeName;
    private final String destCom;

    public ShipBookmarkView(int id, int system, int x, int y, String name, String typeName, int destSystem, int destX, int destY, String destCom) {
        this.id = id;
        this.location = new Location(system, x, y);
        this.name = name;
        this.typeName = typeName;
        this.destLocation = new Location(destX, destY, destSystem);
        this.destCom = destCom;
    }

    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public Location getDestLocation() {
        return destLocation;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getDestCom() {
        return destCom;
    }
}
