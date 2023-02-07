DROP procedure get_enemy_ships_in_system;
DELIMITER //
CREATE PROCEDURE get_enemy_ships_in_system(
    IN userid Int,
    IN in_star_system Int
)
BEGIN
    SELECT
        s.star_system,
        s.x,
        s.y,
        COALESCE(ur.status, ur_default.status, 0) relation_to_user,
        COALESCE(ur2.status, ur_default2.status, 0) relation_from_user,
        n.type nebeltype,
        MAX(COALESCE(sm.size, st.size)) max_size
    FROM ships s
        LEFT JOIN user_relations ur
            ON (ur.user_id = userid AND s.owner = ur.target_id)
        LEFT JOIN user_relations ur_default
            ON (ur_default.user_id = userid AND ur_default.target_id = 0)
        LEFT JOIN user_relations ur2
            ON (ur2.user_id = s.owner AND ur2.target_id = userid)
        LEFT JOIN user_relations ur_default2
            ON (ur_default2.user_id = s.owner AND ur_default2.target_id = 0)
        INNER JOIN ship_types st
            ON st.id = s.type
        LEFT JOIN nebel n
            ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
        LEFT JOIN ships_modules sm
            ON sm.id = s.modules
        LEFT JOIN ship_flags sf ON sf.ship = s.id AND sf.flagType = 7
    WHERE
		sf.ship is null AND
        s.owner<>-1 AND
        COALESCE(n.type, 1) NOT IN (3, 4, 5) AND
        s.star_system = in_star_system AND
        s.owner != userid AND
        !Locate("l", s.docked)
    GROUP BY s.star_system, s.x, s.y, relation_to_user, relation_from_user;
END;
//