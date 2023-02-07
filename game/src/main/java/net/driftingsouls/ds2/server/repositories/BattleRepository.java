package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;

import java.sql.SQLException;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Battles.BATTLES;

public class BattleRepository {
    public static Set<Location> getBattlePositionsInSystem(int system)
    {
            try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                var db = DBUtil.getDSLContext(conn);
                var battleSelect = db
                        .select(BATTLES.X, BATTLES.Y)
                        .from(BATTLES)
                        .where(BATTLES.STAR_SYSTEM.eq(system));

                try(battleSelect; var battles = battleSelect.stream()) {
                    var battlePositions = battles
                            .map(battle -> new Location(system, battle.value1(), battle.value2()))
                            .collect(toUnmodifiableSet());
                    return battlePositions;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }
}
