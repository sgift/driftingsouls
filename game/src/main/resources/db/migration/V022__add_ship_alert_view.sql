DELIMITER //
CREATE PROCEDURE get_sectors_with_attacking_ships(
    IN userid Int
)
BEGIN
    SELECT s.star_system,
           s.x,
           s.y
    FROM users u
             LEFT JOIN user_relations ur ON u.id = ur.user_id AND ur.target_id = userid
             LEFT JOIN ships s ON s.owner = u.id
    WHERE s.alarm <> 0
      AND ((s.alarm = 2 AND ur.status <> 2 OR ur.status is null) OR (s.alarm = 1 AND ur.status = 1));
END
//