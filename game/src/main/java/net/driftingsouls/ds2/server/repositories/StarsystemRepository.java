package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.StarSystemData;
import net.driftingsouls.ds2.server.map.StarSystemMapData;
import net.driftingsouls.ds2.server.map.StationaryObjectData;
import org.jooq.Records;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.*;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.*;

public class StarsystemRepository {

    private final static StarsystemRepository instance = new StarsystemRepository();

    private final Map<Integer, Map<Location, Nebel.Typ>> nebulaData = new HashMap<>();

    private StarsystemRepository() {}

    public static StarsystemRepository getInstance() {
        return instance;
    }

    public Map<Integer, StarSystemData> starsystemData = new HashMap<>();

    public StarSystemData getStarsystemData(int system)
    {
        if(!starsystemData.containsKey(system)) {
            synchronized (starsystemData) {
                if(!starsystemData.containsKey(system)) {
                    getStarsystemsData();
                }
            }
        }

        if(!starsystemData.containsKey(system)) return null;
        return starsystemData.get(system);
    }

    public Collection<StarSystemData> getStarsystemsData()
    {
        if(starsystemData.size() == 0) {
            synchronized (starsystemData) {
                if(starsystemData.size() == 0) {
                    try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
                        var db = DBUtil.getDSLContext(conn);
                        try(var select = db
                                .selectFrom(SYSTEMS)
                        ) {
                            var result = select.fetch(Records.mapping(StarSystemData::new));

                            for (var row : result) {
                                starsystemData.put(row.getId(), row);
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return starsystemData.values();
    }

    public static Collection<StarSystemMapData> getStarSystemMapData(int userid)
    {
        var systemsWithShips = getSystemsWithShips(userid);
        var alliances = AllianceRepository.getStarSystemMapAlliances();
        var systemOwners = getSystemOwners();
        var jns = getSystemsJns();

        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            try(var select = db
                    .selectFrom(SYSTEMS)
            ) {
                var fetch = select.fetch();
                var result = new ArrayList<StarSystemMapData>();
                for (var system : fetch) {
                    //StarSystemMapData(int id, String name, int mapX, int mapY, int radius, List<Integer> jns, boolean ships, StarSystemMapAlliance alliance)
                    var systemId = system.get(SYSTEMS.ID);
                    var systemJns = jns.get(systemId);
                    var alliance = alliances.get(systemId);

                    var systemMapData = new StarSystemMapData(systemId, system.get(SYSTEMS.NAME), system.get(SYSTEMS.MAPX), system.get(SYSTEMS.MAPY),
                            (int)Math.round(Math.sqrt(system.get(SYSTEMS.WIDTH)*system.get(SYSTEMS.WIDTH) + system.get(SYSTEMS.HEIGHT) * system.get(SYSTEMS.HEIGHT))), systemJns, systemsWithShips.contains(systemId), alliance);
                    result.add(systemMapData);
                }

                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, List<Integer>> getSystemsJns()
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            try(var select = db
                    .selectFrom(JUMPNODES)
            ) {
                var jumpnodes = select.fetch();
                var result = new HashMap<Integer, List<Integer>>();

                for (var jumpnode : jumpnodes) {
                    var system = jumpnode.get(JUMPNODES.STAR_SYSTEM);
                    if(!result.containsKey(system)) result.put(system, new ArrayList<>());

                    result.get(system).add(jumpnode.get(JUMPNODES.SYSTEMOUT));
                }

                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, Integer> getSystemOwners()
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            try(var select = db
                    .select(SHIPS.STAR_SYSTEM, USERS.ALLY, DSL.sum(SHIP_TYPES.SIZE))
                    .from(SHIPS)
                    .innerJoin(SHIP_TYPES)
                        .on(SHIPS.TYPE.eq(SHIP_TYPES.ID))
                    .innerJoin(USERS)
                        .on(USERS.ID.eq(SHIPS.OWNER))
                    .leftJoin(SHIPS_MODULES)
                        .on(SHIPS_MODULES.ID.eq(SHIPS.MODULES))
                    .where("Locate('nebelscan', ships_modules.flags) OR Locate('nebelscan', ship_types.flags)")
                    .groupBy(SHIPS.STAR_SYSTEM, USERS.ALLY)
                    .orderBy(SHIPS.STAR_SYSTEM, DSL.sum(SHIP_TYPES.SIZE).desc())
            ) {
                System.out.println(select.getSQL());
                var fetch = select.fetch();
                var result = new HashMap<Integer, Integer>();

                Integer lastSystem = null;
                for (var row : fetch) {
                    if(lastSystem == null || row.get(SHIPS.STAR_SYSTEM) != lastSystem)
                    {
                        lastSystem = row.get(SHIPS.STAR_SYSTEM);
                        result.put(lastSystem, row.get(USERS.ALLY));
                    }
                }

                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashSet<Integer> getSystemsWithShips(int userid)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var select = db
                    .select(SHIPS.STAR_SYSTEM)
                    .from(SHIPS)
                    .where(SHIPS.OWNER.eq(userid))
                    .groupBy(SHIPS.STAR_SYSTEM)
            ) {
                var fetch = select.fetch();
                var result = new HashSet<Integer>();

                for (var row : fetch) {
                    result.add(row.get(SHIPS.STAR_SYSTEM));
                }

                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
