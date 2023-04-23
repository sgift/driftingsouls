CREATE OR REPLACE
VIEW friendly_scan_ranges AS
SELECT s.id,
       s.star_system,
       s.x,
       s.y,
       s.owner,
       ur.target_id,
       s.sensors as sensor_status,
       COALESCE(sm.sensorrange, st.sensorrange) as sensor_range
FROM user_relations ur
         INNER JOIN ships s ON s.owner = ur.user_id
         INNER JOIN users u on s.owner = u.id
         INNER JOIN ship_types st
                    ON st.id = s.type
         LEFT JOIN ships_modules sm
                   ON sm.id = s.modules
         LEFT JOIN nebel n
                   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
WHERE
	ur.status = 2 AND
	s.owner != -1 AND
	COALESCE(n.type, 1) NOT IN (3, 4, 5) AND
    (u.vaccount = 0 OR (u.vaccount > 0 AND u.wait4vac > 0));



CREATE OR REPLACE
VIEW friendly_nebel_scan_ranges AS
SELECT s.id,
       s.star_system,
       s.x,
       s.y,
       s.owner,
       ur.target_id,
       s.sensors as sensor_status,
       COALESCE(sm.sensorrange, st.sensorrange) as sensor_range
FROM user_relations ur
         INNER JOIN ships s ON s.owner = ur.user_id
         INNER JOIN users u on s.owner = u.id
         INNER JOIN ship_types st
                    ON st.id = s.type
         LEFT JOIN ships_modules sm
                   ON sm.id = s.modules
         LEFT JOIN nebel n
                   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
         INNER JOIN ship_flags sf ON sf.ship = s.id
WHERE
	ur.status = 2 AND
	s.owner != -1 AND
	COALESCE(n.type, 1) NOT IN (3, 4, 5) AND
    (u.vaccount = 0 OR (u.vaccount > 0 AND u.wait4vac > 0)) AND
    sf.flagType = 24;