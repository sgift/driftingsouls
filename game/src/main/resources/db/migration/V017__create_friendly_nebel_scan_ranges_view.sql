CREATE OR REPLACE VIEW friendly_nebel_scan_ranges AS SELECT
                                                       s.id AS id,
                                                       s.star_system AS star_system,
                                                       s.x AS x,
                                                       s.y AS y,
                                                       s.owner AS owner,
                                                       ur.target_id AS target_id,
                                                       CAST(MAX(s.sensors * COALESCE(sm.sensorrange, st.sensorrange) * 0.01 * (1.0 * (n.type IS NULL) + 0.5 * (n.type IS NOT NULL)))
                                                           AS UNSIGNED) AS sensor_range
                                                   FROM
                                                       ((((user_relations ur
                                                       JOIN ships s ON (s.owner <> - 1
                                                           AND s.owner = ur.user_id))
                                                       JOIN ship_types st ON (st.id = s.type))
                                                       LEFT JOIN ships_modules sm ON (sm.id = s.modules))
                                                       LEFT JOIN nebel n ON (n.star_system = s.star_system
                                                           AND n.x = s.x
                                                           AND n.y = s.y))
                                               		LEFT JOIN ship_flags sf ON sf.ship=s.id
                                                   WHERE
                                                       ur.status = 2
                                                           AND COALESCE(n.type, 1) NOT IN (4 , 5, 6)
                                                           AND sf.flagType=24
                                                   GROUP BY s.star_system , s.x , s.y , ur.target_id , s.owner , s.id