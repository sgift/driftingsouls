package net.driftingsouls.ds2.server.map;

public class ShipData {
    public final int id;
    public final String name;
    public final int ownerId;
    public final int ownerRaceId;
    public final int landedShips;
    public final int dockedShips;
    public final int energy;
    public final int heat;
    public final boolean isDocked;
    public final boolean isLanded;
    public final int sensors;
    public final Integer fleetId;
    public final String fleetName;

    public ShipData(int id, String name, int ownerId, int ownerRaceId, int landedShips, int dockedShips, int energy, int heat, String dockInformation, int sensors, Integer fleetId, String fleetName) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerRaceId = ownerRaceId;
        this.landedShips = landedShips;
        this.dockedShips = dockedShips;
        this.energy = energy;
        this.heat = heat;
        this.sensors = sensors;
        this.fleetId = fleetId;
        this.fleetName = fleetName;

        if(!dockInformation.isBlank()) {
            isLanded = dockInformation.startsWith("l");
            isDocked = !isLanded;
        } else {
            isLanded = false;
            isDocked = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShipData shipData = (ShipData) o;

        return id == shipData.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
