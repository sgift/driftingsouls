CREATE OR REPLACE VIEW non_friendly_ship_locations AS
SELECT s.star_system, s.x, s.y, ur.target_id, ur.status, n.type nebeltype, MAX(st.size) max_size
FROM user_relations ur
         INNER JOIN ships s ON s.owner = ur.user_id
         INNER JOIN ship_types st
                    ON st.class != 20 AND st.id = s.type
         LEFT JOIN nebel n
                   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
WHERE ur.status
    <> 2
  AND COALESCE(n.type, 1) NOT IN (4, 5, 6)
GROUP BY s.star_system, s.x, s.y, ur.target_id, ur.status;