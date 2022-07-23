package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.UserRelationData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static net.driftingsouls.ds2.server.entities.jooq.tables.UserRelations.USER_RELATIONS;


/**
 * Diese Klasse ist dazu gedacht, die Beziehung eines einzelnen Users zu allen anderen, sowie die Beziehung aller anderen zu ihm zwischenzuspeichern.
 * Aktuell dient diese Klasse nur dazu festzustellen ob Spieler befreundet sind.
 * Ziel ist es, mehrere Abfragen zum Server zu vermeiden indem einmal alle relevant eingeschätzten Daten geladen werden.
 */
public class SingleUserRelationsService {
    private final int userId;

    public SingleUserRelationsService(int userId) {
        this.userId = userId;
        getUserRelationData();
    }

    protected Map<Integer, ArrayList<UserRelationData>> userRelationData;

    public Map<Integer, ArrayList<UserRelationData>> getUserRelationData() {
        if (this.userRelationData != null) {
            return this.userRelationData;
        }
        Map<Integer, ArrayList<UserRelationData>> userRelationData = new HashMap<>();
        try (var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try
                (
                    var userRelations = db
                        .selectFrom(USER_RELATIONS)
                        .where
                            (
                                USER_RELATIONS.USER_ID.eq(userId)
                                    .or(USER_RELATIONS.TARGET_ID.eq(userId))
                            )
                ) {
                var result = userRelations.fetch();

                for (var row : result) {
                    var urd = new UserRelationData(row.getValue(USER_RELATIONS.USER_ID), row.getValue(USER_RELATIONS.TARGET_ID), row.getValue(USER_RELATIONS.STATUS));
                    if (urd.getUserId() == userId) // Meine Beziehung zu anderen Spielern
                    {
                        if (!userRelationData.containsKey(urd.getTargetUserId())) {
                            userRelationData.put(urd.getTargetUserId(), new ArrayList<>());
                        }
                        userRelationData.get(urd.getTargetUserId()).add(urd);
                    } else // Die Beziehung anderer Spieler zu mir
                    {
                        if (!userRelationData.containsKey(urd.getUserId())) {
                            userRelationData.put(urd.getUserId(), new ArrayList<>());
                        }
                        userRelationData.get(urd.getUserId()).add(urd);
                    }
                }
                this.userRelationData = userRelationData;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return userRelationData;
    }

    public boolean isMutualFriendTo(int targetId) {
        var userRelations = getUserRelationData();

        boolean areMutalFriends = false;
        if (userRelations.containsKey(targetId))
            areMutalFriends = userRelations.get(targetId).stream().filter(x -> x.getStatus() == 2).count() == 2;

        return areMutalFriends;
    }
}
