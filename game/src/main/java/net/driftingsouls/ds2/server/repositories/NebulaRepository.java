package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;

public class NebulaRepository {
    private final static NebulaRepository instance = new NebulaRepository();

    private final Map<Integer, Map<Location, Nebel.Typ>> nebulaData = new HashMap<>();

    private NebulaRepository() {}

    public static NebulaRepository getInstance() {
        return instance;
    }

    public Map<Location, Nebel.Typ> getNebulaData(int system)
    {
        if(!nebulaData.containsKey(system)) {
            synchronized (nebulaData) {
                if(!nebulaData.containsKey(system)) {
                    nebulaData.put(system, new HashMap<Location, Nebel.Typ>());
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        try(var select = db
                                .selectFrom(NEBEL)
                                .where(NEBEL.STAR_SYSTEM.eq(system))
                        )
                        {
                            for(var record : select.fetch())
                            {
                                nebulaData.get(system).put(new Location(system, record.getX(), record.getY()), Nebel.Typ.getType(record.getType()));
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(nebulaData.get(system));
    }

    public Nebel.Typ getNebula(Location location)
    {
        if(!nebulaData.containsKey(location.getSystem()))
        {
            getNebulaData(location.getSystem());
        }
        return nebulaData.get(location.getSystem()).get(location);
    }

    public void clearItemCache() {
        nebulaData.clear();
    }
}
