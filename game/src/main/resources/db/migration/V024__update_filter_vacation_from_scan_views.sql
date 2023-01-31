CREATE OR REPLACE VIEW friendly_scan_ranges AS
SELECT s.id,
       s.star_system,
       s.x,
       s.y,
       s.owner,
       ur.target_id,
       CONVERT(MAX(s.sensors * COALESCE(sm.sensorrange, st.sensorrange) * 0.01), unsigned) sensor_range
FROM user_relations ur
         INNER JOIN ships s ON s.owner != -1 AND s.owner = ur.user_id
         INNER JOIN users u on s.owner = u.id AND u.vaccount = 0 OR (u.vaccount > 0 AND u.wait4vac = 0)
         INNER JOIN ship_types st
                    ON st.id = s.type
         LEFT JOIN ships_modules sm
                   ON sm.id = s.modules
         LEFT JOIN nebel n
                   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
WHERE ur.status = 2
  AND COALESCE(n.type, 1) NOT IN (4, 5, 6)
GROUP BY s.star_system, s.x, s.y, ur.target_id, s.owner, s.id;

CREATE OR REPLACE VIEW friendly_nebel_scan_ranges AS
SELECT s.id             AS id,
       s.star_system    AS star_system,
       s.x              AS x,
       s.y              AS y,
       s.owner          AS owner,
       ur.target_id     AS target_id,
       CAST(MAX(s.sensors * COALESCE(sm.sensorrange, st.sensorrange) * 0.01)
           AS UNSIGNED) AS sensor_range
FROM ((((user_relations ur
    JOIN ships s ON (s.owner <> - 1 AND s.owner = ur.user_id))
    JOIN users u on s.owner = u.id AND u.vaccount=0 OR (u.vaccount>0 AND u.wait4vac=0)
    JOIN ship_types st ON (st.id = s.type))
    LEFT JOIN ships_modules sm ON (sm.id = s.modules))
    INNER JOIN nebel n ON (n.star_system = s.star_system
    AND n.x = s.x
    AND n.y = s.y))
         LEFT JOIN ship_flags sf ON sf.ship = s.id
WHERE ur.status = 2
  AND n.type NOT IN (4, 5, 6)
  AND sf.flagType = 24
GROUP BY s.star_system, s.x, s.y, ur.target_id, s.owner, s.id