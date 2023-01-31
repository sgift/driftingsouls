package net.driftingsouls.ds2.server.map;

/**
 * An area of the map.
 */
public class MapArea {
    private final int xStart;
    private final int xSize;
    private final int yStart;
    private final int ySize;


    public MapArea(int xStart, int xSize, int yStart, int ySize) {
        this.xStart = xStart;
        this.xSize = xSize;
        this.yStart = yStart;
        this.ySize = ySize;
    }

    public int getLowerBoundX() {
        return xStart;
    }

    public int getUpperBoundX() {
        return xStart + xSize;
    }

    public int getLowerBoundY() {
        return yStart;
    }

    public int getUpperBoundY() {
        return yStart + ySize;
    }
}
