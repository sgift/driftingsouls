package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.BaseData;
import org.jooq.Condition;
import org.jooq.Records;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.USERS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;

public class BasesRepository {
    private static final BasesRepository instance = new BasesRepository();

    private final ConcurrentMap<Integer, List<BaseData>> systemBases = new ConcurrentHashMap<>();

    public static BasesRepository getInstance() {
        return instance;
    }

    public List<BaseData> getBaseDataBySystem(int system)
    {
        return systemBases.computeIfAbsent(system, sys -> {
            Condition condition = DSL.trueCondition();
            condition.and(BASES.STAR_SYSTEM.eq(sys));
            return getBaseMap(condition);
        });
    }

    public void clearSystem(int system) {
        systemBases.remove(system);
    }

    private List<BaseData> getBaseMap(Condition condition)
    {
        List<BaseData> bases = new ArrayList<>();
            try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                var db = DBUtil.getDSLContext(conn);
                try(var basesSelect = db
                        .select(
                                BASES.STAR_SYSTEM,
                                BASES.X,
                                BASES.Y,
                                BASES.OWNER,
                                USERS.ALLY,
                                BASE_TYPES.SIZE,
                                BASE_TYPES.STARMAPIMAGE
                        )
                        .from(
                                BASES.innerJoin(BASE_TYPES)
                                        .on(BASES.KLASSE.eq(BASE_TYPES.ID))
                                        .innerJoin(USERS)
                                        .on(USERS.ID.eq(BASES.OWNER))
                        )
                        .where(condition))
                {
                    return new ArrayList<>(basesSelect.fetch(Records.mapping(BaseData::new)));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }
}
