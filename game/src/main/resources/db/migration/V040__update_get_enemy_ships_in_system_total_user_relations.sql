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
        ur.relation_to,
        ur.relation_from,
        n.type nebeltype,
        MAX(COALESCE(sm.size, st.size)) max_size
    FROM ships s
        INNER JOIN v_total_user_relations ur
            ON (ur.id = userid AND s.owner = ur.target)
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
    GROUP BY s.star_system, s.x, s.y, relation_to, relation_from;
END;
//