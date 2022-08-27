package net.driftingsouls.ds2.server.map;

public class StarSystemData {
    public final int id;
    public final Byte military;
    public final String description;

    public final String gtuDropZone;

    public final int height;
    public final Byte starmap;
    public final int mapX;
    public final int mapY;
    public final int maxColonies;
    public final String name;
    public final String orderLoc;
    public final String spawnableRessources;
    public final int access;
    public final int width;

    public StarSystemData(int id, Byte military, String description, String gtuDropZone, int height, Byte starmap, int mapX, int mapY, int maxColonies, String name, String orderLoc, String spawnableRessources, int access, int width) {
        this.id = id;
        this.military = military;
        this.description = description;
        this.gtuDropZone = gtuDropZone;
        this.height = height;
        this.starmap = starmap;
        this.mapX = mapX;
        this.mapY = mapY;
        this.maxColonies = maxColonies;
        this.name = name;
        this.orderLoc = orderLoc;
        this.spawnableRessources = spawnableRessources;
        this.access = access;
        this.width = width;

    }
}
