package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.Starmap;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumpnodes.JUMPNODES;

public class JumpNodeRepository {
    private final static JumpNodeRepository instance = new JumpNodeRepository();
    private final Map<Integer, Map<Location, Starmap.JumpNode>> jumpNodeMap = new HashMap<>();

    private JumpNodeRepository() {}

    public static JumpNodeRepository getInstance() {
        return instance;
    }

    public Map<Location, Starmap.JumpNode> getJumpNodesInSystem(int system)
    {
        if(!jumpNodeMap.containsKey(system)) {
            synchronized (jumpNodeMap) {
                if(!jumpNodeMap.containsKey(system)) {
                    jumpNodeMap.put(system, new HashMap<Location, Starmap.JumpNode>());
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        var result = db.select(JUMPNODES.X, JUMPNODES.Y, JUMPNODES.HIDDEN)
                                .from(JUMPNODES)
                                .where(JUMPNODES.STAR_SYSTEM.eq(system))
                                .fetch();
                        var nodes = jumpNodeMap.get(system);
                        for(var record: result) {
                            nodes.put(new Location(system, record.value1(), record.value2()), new Starmap.JumpNode(record.value1(), record.value2(), record.value3() != 0));
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return jumpNodeMap.get(system);
    }
}
