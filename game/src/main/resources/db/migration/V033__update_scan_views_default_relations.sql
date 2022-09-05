CREATE OR REPLACE
VIEW friendly_scan_ranges AS
SELECT s.id,
    s.star_system,
    s.x,
    s.y,
    s.owner,
    ur.target_id,
    s.sensors as sensor_status,
    COALESCE(sm.sensorrange, st.sensorrange) as sensor_range,
    COALESCE(ur.status, ur_default.status) as status1,
    COALESCE(ur2.status, ur_default2.status) as status2
FROM user_relations ur
    LEFT JOIN user_relations ur_default
        ON (ur_default.user_id = ur.user_id AND ur_default.target_id = 0)
    LEFT JOIN user_relations ur2
        ON (ur.user_id = ur2.target_id AND ur.target_id = ur2.user_id)
    LEFT JOIN user_relations ur_default2
            ON (ur_default2.user_id = ur2.user_id AND ur_default2.target_id = 0)
    INNER JOIN ships s
        ON s.owner = ur.user_id
    INNER JOIN users u
        ON s.owner = u.id
    INNER JOIN ship_types st
        ON st.id = s.type
    LEFT JOIN ships_modules sm
        ON sm.id = s.modules
    LEFT JOIN nebel n
        ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
WHERE
	s.owner != -1 AND
	COALESCE(n.type, 1) NOT IN (3, 4, 5) AND
    (u.vaccount = 0 OR (u.vaccount > 0 AND u.wait4vac > 0))
    AND !Locate("l", s.docked);


CREATE OR REPLACE
VIEW friendly_nebel_scan_ranges AS
SELECT s.id,
       s.star_system,
       s.x,
       s.y,
       s.owner,
       ur.target_id,
       s.sensors as sensor_status,
       COALESCE(sm.sensorrange, st.sensorrange) as sensor_range,
       COALESCE(ur.status, ur_default.status) as status1,
       COALESCE(ur2.status, ur_default2.status) as status2
FROM user_relations ur
    LEFT JOIN user_relations ur_default
        ON (ur_default.user_id = ur.user_id AND ur_default.target_id = 0)
    LEFT JOIN user_relations ur2
        ON (ur.user_id = ur2.target_id AND ur.target_id = ur2.user_id)
    LEFT JOIN user_relations ur_default2
        ON (ur_default2.user_id = ur2.user_id AND ur_default2.target_id = 0)
    INNER JOIN ships s
        ON s.owner = ur.user_id
    INNER JOIN users u
        on s.owner = u.id
    INNER JOIN ship_types st
        ON st.id = s.type
    LEFT JOIN ships_modules sm
        ON sm.id = s.modules
    LEFT JOIN nebel n
                   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
         INNER JOIN ship_flags sf ON sf.ship = s.id
WHERE
	s.owner != -1 AND
	COALESCE(n.type, 1) NOT IN (3, 4, 5) AND
    (u.vaccount = 0 OR (u.vaccount > 0 AND u.wait4vac > 0)) AND
    sf.flagType = 24
    AND !Locate("l", s.docked);