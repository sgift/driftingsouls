package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BaseData implements Locatable {
    private final int system;
    private final int x;
    private final int y;
    private final int size;
    private final int ownerId;
    private final int ownerAllyId;
    private final String starmapImage;
    public BaseData(int system, int x, int y, int ownerId, Integer ownerAllyId, int size, String starmapImage)
    {
        this.system = system;
        this.x = x;
        this.y = y;
        this.size = size;
        this.ownerId = ownerId;
        this.ownerAllyId = ownerAllyId != null ? ownerAllyId : 0;
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

    public String getStarmapImage(){ return starmapImage; }


    public Map<Location, BaseData> getLocationsMap()
    {
        var baseMap = new HashMap<Location, BaseData>();
        Location position = getLocation();

        if(size > 0)
        {
            for(int y = position.getY() - size; y <= position.getY() + size; y++)
            {
                for(int x = position.getX() - size; x <= position.getX() + size; x++)
                {
                    Location loc = new Location(position.getSystem(), x, y);

                    if( !position.sameSector( 0, loc, getSize() ) ) {
                        continue;
                    }

                    baseMap.put(loc, this); //Big objects are always printed first
                }
            }
        }
        else
        {
            baseMap.put(position, new BaseData(system, x, y, getOwnerId(), getOwnerAllyId(), size, getStarmapImage()));
        }

        return baseMap;
    }
}
