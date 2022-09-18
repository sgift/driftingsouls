package net.driftingsouls.ds2.server.map;

import java.util.Collection;
import java.util.List;

public class StarSystemMapData {
    public int id;
    public String name;
    public int mapX;
    public int mapY;
    public int radius;
    public List<Integer> jns;
    public boolean ships;
    public StarSystemMapAlliance alliance;


    public StarSystemMapData(int id, String name, int mapX, int mapY, int radius, List<Integer> jns, boolean ships, StarSystemMapAlliance alliance) {
        this.id = id;
        this.name = name;
        this.mapX = mapX;
        this.mapY = mapY;
        this.radius = radius;
        this.jns = jns;
        this.ships = ships;
        this.alliance = alliance;
    }

    public static class StarSystemMapAlliance
    {
        public int id;
        public String name;
        public String plainname;
        public String color;

        public StarSystemMapAlliance(int id, String name, String plainname, String color)
        {
            this.id = id;
            this.name = name;
            this.plainname = plainname;
        }
    }
}
