package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.BaseData;
import org.jooq.Records;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.TRANSMISSIONEN;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.USERS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;

public class MessageRepository {
    public static int getNewMessageCount(int userid)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var messageCountSelect = db.selectCount().from(TRANSMISSIONEN).where(TRANSMISSIONEN.GELESEN.eq(0).and(TRANSMISSIONEN.EMPFAENGER.eq(userid))))
            {
                return messageCountSelect.fetchOne(0, int.class);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
