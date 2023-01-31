package net.driftingsouls.ds2.server.map;

/**
 * Things which cannot move on the map, e.g. bases, rocks ..
 */
public class StationaryObjectData {
    public final int id;
    public final String name;
    public final int ownerId;
    public final String ownerName;
    public final String image;
    public final int typeId;
    public final String typeName;

    public StationaryObjectData(int id, String name, int ownerId, String ownerName, String image, int typeId, String typeName) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.image = image;
        this.typeId = typeId;
        this.typeName = typeName;
    }
}
