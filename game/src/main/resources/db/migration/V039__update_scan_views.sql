CREATE OR REPLACE
VIEW friendly_scan_ranges AS
SELECT s.id,
    s.star_system,
    s.x,
    s.y,
    s.owner,
	ur.target,
	ur.relation_to,
	ur.relation_from,
    s.sensors as sensor_status,
    COALESCE(sm.sensorrange, st.sensorrange) as sensor_range
FROM ships s
    INNER JOIN v_total_user_relations ur
        ON s.owner = ur.id
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
    AND NOT Locate("l", s.docked);


CREATE OR REPLACE
VIEW friendly_nebel_scan_ranges AS
SELECT s.id,
        s.star_system,
        s.x,
        s.y,
        s.owner,
        s.sensors as sensor_status,
        COALESCE(sm.sensorrange, st.sensorrange) as sensor_range,
       	ur.target,
       	ur.relation_to,
       	ur.relation_from
FROM ships s
    INNER JOIN v_total_user_relations ur
        ON s.owner = ur.id
    INNER JOIN users u
        ON s.owner = u.id
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
    (Locate("nebelscan", st.flags) OR
    Locate("nebelscan", sm.flags)) AND
    NOT Locate("l", s.docked);