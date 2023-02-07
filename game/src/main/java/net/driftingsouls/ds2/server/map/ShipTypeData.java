package net.driftingsouls.ds2.server.map;

import org.jetbrains.annotations.NotNull;

public class ShipTypeData implements Comparable<ShipTypeData> {
    public final int id;
    public final String name;
    public final String picture;
    public final int size;
    public final int fighterDocks;
    public final int externalDocks;
    public final int maxEnergy;
    public final int movementCost;
    public final int scanRange;

    public ShipTypeData(int id, String name, String picture, int size, int fighterDocks, int externalDocks, int maxEnergy, int movementCost, int scanRange) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.size = size;
        this.fighterDocks = fighterDocks;
        this.externalDocks = externalDocks;
        this.maxEnergy = maxEnergy;
        this.movementCost = movementCost;
        this.scanRange = scanRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShipTypeData that = (ShipTypeData) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(@NotNull ShipTypeData o) {
        return Integer.compare(id, o.id);
    }
}
