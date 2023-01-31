package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;

public class StarSystemData {
    public final int id;
    public final Byte military;
    public final String description;

    public final String gtuDropZone;

    public final int height;
    public final Byte isSystemVisible;
    public final int mapX;
    public final int mapY;
    public final int maxColonies;
    public final String name;
    public final String orderLoc;
    public final String spawnableRessources;
    public final StarSystem.Access access;
    public final int width;

    public StarSystemData(int id, Byte military, String description, String gtuDropZone, int height, Byte starmap, int mapX, int mapY, int maxColonies, String name, String orderLoc, String spawnableRessources, int access, int width) {
        this.id = id;
        this.military = military;
        this.description = description;
        this.gtuDropZone = gtuDropZone;
        this.height = height;
        this.isSystemVisible = starmap;
        this.mapX = mapX;
        this.mapY = mapY;
        this.maxColonies = maxColonies;
        this.name = name;
        this.orderLoc = orderLoc;
        this.spawnableRessources = spawnableRessources;
        this.access = StarSystem.getAccess(access);
        this.width = width;

    }

    public int getId(){return id;}

    public boolean isVisibleFor(User user)
    {
        if( this.access == StarSystem.Access.NICHT_SICHTBAR )
        {
            return false;
        }
        if( user.hasFlag(UserFlag.VIEW_ALL_SYSTEMS) )
        {
            return true;
        }
        if( this.access == StarSystem.Access.ADMIN )
        {
            return false;
        }
        if( this.access == StarSystem.Access.NPC )
        {
            return user.hasFlag(UserFlag.VIEW_SYSTEMS);
        }
        return isSystemVisible == 1;
    }
}
