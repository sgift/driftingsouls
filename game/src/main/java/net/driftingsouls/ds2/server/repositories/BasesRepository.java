package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.cargo.ItemData;
import net.driftingsouls.ds2.server.entities.jooq.tables.Bases;
import net.driftingsouls.ds2.server.entities.jooq.tables.records.BasesRecord;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.BaseData;
import org.jooq.Condition;
import org.jooq.Records;
import org.jooq.SelectConditionStep;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.USERS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;

public class BasesRepository {
    public static ArrayList<BaseData> getBaseMap(int system, Condition condition)
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
                        .where(BASES.STAR_SYSTEM.eq(system)).and(condition))
                {
                    var result = new ArrayList<>(basesSelect.fetch(Records.mapping(BaseData::new)));
                    return result;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }
}
