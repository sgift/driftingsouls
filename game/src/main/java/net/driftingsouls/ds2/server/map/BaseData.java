package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaseData implements Locatable {
    private final int system;
    private final int x;
    private final int y;
    private final int size;
    private final int ownerId;
    private final int ownerAllyId;
    private final String starmapImage;
    public BaseData(int system, int x, int y, int ownerId, int ownerAllyId, int size, String starmapImage)
    {
        this.system = system;
        this.x = x;
        this.y = y;
        this.size = size;
        this.ownerId = ownerId;
        this.ownerAllyId = ownerAllyId;
        this.starmapImage = starmapImage;
    }

    @Override
    public Location getLocation() {
        return new Location(system, x, y);
    }

    public int getSize()
    {
        return size;
    }
    public int getOwnerId()
    {
        return ownerId;
    }
    public int getOwnerAllyId()
    {
        return ownerAllyId;
    }

    public int[] getSectorImageOffset(Location location, Location baseLocation)
    {
        if( size == 0 || !location.sameSector(0, baseLocation, size))
        {
            return new int[] {0,0};
        }

        for(int by = baseLocation.getY() - size; by <= baseLocation.getY() + size; by++)
        {
            for(int bx = baseLocation.getX() - size; bx <= baseLocation.getX() + size; bx++)
            {
                Location loc = new Location(baseLocation.getSystem(), bx, by);

                if( !baseLocation.sameSector(0, loc, getSize()))
                {
                    continue;
                }

                if(location.equals(loc))
                {
                    return new int[] {-bx+baseLocation.getX()-size, -by+baseLocation.getY()-size};
                }
            }
        }
        return new int[] {0,0};
    }

    public String getSectorImage(Location location)
    {
        if(!location.sameSector(0, this.getLocation(), size))
        {
            return "";
        }

        return starmapImage;
    }

    public String getOverlayImage(Location location, User user, boolean scanned, boolean areMutualFriends)
    {
        if(!location.sameSector(0, getLocation(), size))
        {
            return null;
        }

        org.hibernate.Session db = ContextMap.getContext().getDB();
        User nobody = (User)db.get(User.class, -1);
        User zero = (User)db.get(User.class, 0);

        if(size > 0)
        {
            return null;
        }
        else if(ownerId == user.getId())
        {
            return "data/starmap/asti_own/asti_own.png";
        }
        else if(((ownerId != 0) && (user.getAlly() != null) && (ownerAllyId == user.getAlly().getId()) && user.getAlly().getShowAstis()) ||
                areMutualFriends)
        {
            return "data/starmap/asti_ally/asti_ally.png";
        }
        else if(scanned && ownerId != -1 && ownerId != 0)
        {
            return "data/starmap/asti_enemy/asti_enemy.png";
        }
        else if(scanned && ownerId != -1 || ownerId == 0)
        {
            return starmapImage;
        }
        else
        {
            return null;
        }
    }
}
