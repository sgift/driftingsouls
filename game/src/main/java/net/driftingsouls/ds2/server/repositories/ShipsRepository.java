package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetSectorsWithAttackingShips;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;

public class ShipsRepository {



    public static HashSet<Location> getAttackingSectors(int userid){
        var attackingSectors = new HashSet<Location>();
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            var sectorsWithAttackingShips = new GetSectorsWithAttackingShips();
            sectorsWithAttackingShips.setUserid(userid);
            sectorsWithAttackingShips.execute(db.configuration());

            for(var record: sectorsWithAttackingShips.getResults().get(0)) {
                attackingSectors.add(new Location(
                        record.get(SHIPS.STAR_SYSTEM),
                        record.get(SHIPS.X),
                        record.get(SHIPS.Y)
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return attackingSectors;
    }

}
