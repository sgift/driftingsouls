package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;

public class NonFriendScanData implements Locatable {
    private final int system;
    private final int x;
    private final int y;
    private final int size;
    private final User.Relation relation;


    public NonFriendScanData(int system, int x, int y, int size, int relationTo, int relationFrom) {
        this.system = system;
        this.x = x;
        this.y = y;
        this.relation = getRelation(relationTo, relationFrom);

        this.size = size;
    }

    @Override
    public Location getLocation() {
        return new Location(system, x, y);
    }


    public User.Relation getRelation()
    {
        return relation;
    }

    public int getSize()
    {
        return size;
    }

    private User.Relation getRelation(int relationTo, int relationFrom)
    {
        User.Relation relation;

        // This should never be the case, but if it is, better safe then sorry -> players are enemies
        if(relationTo < 0 || relationTo > 2 || relationFrom <0 || relationFrom > 2) return User.Relation.ENEMY;

        // Mutual friends
        if(relationTo == 2 && relationFrom == 2) relation = User.Relation.FRIEND;

        // Not mutual friends, so is nobody is enemy, the best relation they can have is neutral
        else if(relationTo != 1 && relationFrom != 1) relation = User.Relation.NEUTRAL;

        // Not mutual friends, not neutral, so at least one is enemy to the other
        else relation = User.Relation.ENEMY;

        return relation;
    }

}
