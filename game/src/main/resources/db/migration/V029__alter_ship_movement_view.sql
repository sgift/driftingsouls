
CREATE OR REPLACE VIEW ship_movement AS
SELECT
s.id as ship_id,
s.engine,
s.e,
s.s,
COALESCE(sm.cost, st.cost) as energy_cost,
COALESCE(sm.heat, st.heat) as heat_increment,

COALESCE(o.id, 0) as officer_id,
COALESCE(o.nav, 0) as nav,
COALESCE(o.navu, 0) as navu,
COALESCE(o.ing, 0) as ing,
COALESCE(o.ingu, 0) as ingu,
COALESCE(o.spec, 0) as spec,

s.star_system,
s.x,
s.y,

COALESCE(s.battle, 0) as battle,
s.owner,
COALESCE(s.fleet, 0) as fleet

FROM ships s
left join offiziere o on o.stationiertAufSchiff_id = s.id
INNER JOIN ship_types st ON s.type = st.id
LEFT JOIN ships_modules sm ON sm.id = s.modules
WHERE s.battle is null AND s.docked = '';