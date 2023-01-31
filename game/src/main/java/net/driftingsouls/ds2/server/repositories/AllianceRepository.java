package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.StarSystemData;
import net.driftingsouls.ds2.server.map.StarSystemMapData;
import org.jooq.Records;

import java.sql.SQLException;
import java.util.*;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.ALLY;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.SHIPS;

public class AllianceRepository {

    public static Map<Integer, StarSystemMapData.StarSystemMapAlliance> getStarSystemMapAlliances()
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var select = db
                    .select(ALLY.ID, ALLY.NAME, ALLY.PLAINNAME, ALLY.COLOR)
                    .from(ALLY)
            ) {
                var alliances = select.fetch(Records.mapping(StarSystemMapData.StarSystemMapAlliance::new));
                var result = new HashMap<Integer, StarSystemMapData.StarSystemMapAlliance>();

                for (var alliance : alliances) {
                    result.put(alliance.id, alliance);
                }

                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
