package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.config.items.Munition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipData {
    public final int id;
    public final String name;
    public final int ownerId;
    public final int ownerRaceId;
    public final int landedShipCount;
    public final int dockedShips;
    public final int energy;
    public final int heat;
    public final boolean isDocked;
    public final boolean isLanded;
    public final int sensors;
    public final Integer fleetId;
    public final String fleetName;
    public final List<ShipData> landedShips;
    public final int type;
    public final Integer carrierId;
    public final Cargo cargo;

    public ShipData(int id, String name, int ownerId, int ownerRaceId, int landedShips, int dockedShips, int energy, int heat, String dockInformation, int sensors, Integer fleetId, String fleetName, int type, String cargo) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerRaceId = ownerRaceId;
        this.landedShipCount = landedShips;
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

        if(isLanded)
        {
            this.cargo = new Cargo(Cargo.Type.ITEMSTRING, cargo);
            this.carrierId = Integer.parseInt(dockInformation.substring(2));
        }
        else
        {
            this.cargo = null;
            this.carrierId = null;
        }

        this.landedShips = new ArrayList<ShipData>();
        this.type = type;
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

    public static class DockedShipCount
    {
        public final int id;
        public int externalCount;
        public int landedCount;
        public DockedShipCount(int id, int externalCount, int landedCount)
        {
            this.id = id;
            this.externalCount = externalCount;
            this.landedCount = landedCount;
        }
    }

    public void addLandedShip(ShipData landedShip)
    {
        landedShips.add(landedShip);
    }

    public Map<Integer, ShipDataAmmo> getAmmo()
    {
        var map = new HashMap<Integer, ShipDataAmmo>();
        var ammos = cargo.getItems();

        for (var ammo: ammos) {
            if(ammo.isAmmo())
            {
                var shipAmmo = new ShipDataAmmo(ammo.getItemID(), ammo.getCount(), ammo.getItem().getPicture());
                map.put(ammo.getItemID(), shipAmmo);
            }
        }

        return map;
    }


    public static class ShipDataAmmo{
        public int id;
        public long amount;
        public String picture;

        public ShipDataAmmo(int id, long amount, String picture)
        {
            this.id=id;
            this.amount = amount;
            this.picture = picture;
        }
    }
}
