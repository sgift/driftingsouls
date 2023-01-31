package net.driftingsouls.ds2.server.map;

import org.jetbrains.annotations.NotNull;

public class UserData implements Comparable<UserData> {
    public final int id;
    public final String name;
    public final int raceId;

    public UserData(int id, String name, int raceId) {
        this.id = id;
        this.name = name;
        this.raceId = raceId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserData userData = (UserData) o;

        return id == userData.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(@NotNull UserData o) {
        int diff = name.compareTo(o.name);
        if(diff == 0) {
            return Integer.compare(id, o.id);
        }

        return diff;
    }
}
