package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.StarSystemData;
import net.driftingsouls.ds2.server.map.StationaryObjectData;
import org.jooq.Records;

import java.sql.SQLException;
import java.util.*;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.SYSTEMS;

public class StarsystemRepository {

    private final static StarsystemRepository instance = new StarsystemRepository();

    private final Map<Integer, Map<Location, Nebel.Typ>> nebulaData = new HashMap<>();

    private StarsystemRepository() {}

    public static StarsystemRepository getInstance() {
        return instance;
    }

    public Map<Integer, StarSystemData> starsystemData = new HashMap<>();

    public StarSystemData getStarsystemData(int system)
    {
        if(!starsystemData.containsKey(system)) {
            synchronized (starsystemData) {
                if(!starsystemData.containsKey(system)) {
                    getStarsystemsData();
                }
            }
        }

        if(!starsystemData.containsKey(system)) return null;
        return starsystemData.get(system);
    }

    public Collection<StarSystemData> getStarsystemsData()
    {
        if(starsystemData.size() == 0) {
            synchronized (starsystemData) {
                if(starsystemData.size() == 0) {
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        try(var select = db
                                .selectFrom(SYSTEMS)
                        ) {
                            var result = select.fetch(Records.mapping(StarSystemData::new));

                            for (var row : result) {
                                starsystemData.put(row.getId(), row);
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return starsystemData.values();
    }
}
