package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.StarSystemData;
import net.driftingsouls.ds2.server.map.StationaryObjectData;
import org.jooq.Records;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.SYSTEMS;

public class StarsystemRepository {

    private final static StarsystemRepository instance = new StarsystemRepository();

    private final Map<Integer, Map<Location, Nebel.Typ>> nebulaData = new HashMap<>();

    private StarsystemRepository() {}

    public static StarsystemRepository getInstance() {
        return instance;
    }

    public Map<Integer, StarSystemData> staraystemData = new HashMap<>();

    public Map<Location, Nebel.Typ> getNebulaData(int system)
    {
        if(!staraystemData.containsKey(system)) {
            synchronized (staraystemData) {
                if(!staraystemData.containsKey(system)) {
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        try(var select = db
                                .selectFrom(SYSTEMS)
                                .where(SYSTEMS.ID.eq(system))
                        ) {
                            var result = select.fetch(Records.mapping(StarSystemData::new));

                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(nebulaData.get(system));
    }

}
