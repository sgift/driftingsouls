package net.driftingsouls.ds2.server.services;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.UserRelationData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.V_TOTAL_USER_RELATIONS;
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

    protected Map<Integer, UserRelationData> userRelationData;

    public Map<Integer, UserRelationData> getUserRelationData() {
        if (this.userRelationData != null) {
            return this.userRelationData;
        }
        Map<Integer, UserRelationData> userRelationData = new HashMap<>();
        try (var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try
                (
                    var userRelations = db
                        .selectFrom(V_TOTAL_USER_RELATIONS)
                        .where(V_TOTAL_USER_RELATIONS.ID.eq(userId))
                ) {
                var result = userRelations.fetch();

                for (var row : result) {
                    var urd = new UserRelationData(row.getValue(V_TOTAL_USER_RELATIONS.ID), row.getValue(V_TOTAL_USER_RELATIONS.TARGET), row.getValue(V_TOTAL_USER_RELATIONS.RELATION_TO).intValue(), row.getValue(V_TOTAL_USER_RELATIONS.RELATION_FROM).intValue());

                    userRelationData.put(urd.getTargetUserId(), urd);

                }
                this.userRelationData = userRelationData;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return userRelationData;
    }

    public boolean isMutualFriendTo(int targetId) {

        if(relationFrom(targetId) == User.Relation.FRIEND && relationTo(targetId) == User.Relation.FRIEND )
        {
            return true;
        }
        else return false;
    }

    public User.Relation relationTo(User otherUser){return relationTo(otherUser.getId());}
    public User.Relation relationTo(int otherUserId)
    {
        var userRelations = getUserRelationData();
        var relation = userRelations.get(otherUserId);

        return User.getRelation(relation.getRelationTo());
    }

    public User.Relation relationFrom(User otherUser){return relationFrom(otherUser.getId());}
    /**
     * Gibt die Beziehung eines anderen Spielers zu diesem Spieler zurueck.
     * @param otherUserId Der andere Spieler
     * @return Der Beziehungstyp
     */
    public User.Relation relationFrom(int otherUserId)
    {
        var userRelations = getUserRelationData();

        var relation = userRelations.get(otherUserId);

        if(relation == null) {
            return User.Relation.ENEMY;
        }
        return User.getRelation(relation.getRelationFrom());
    }
}
