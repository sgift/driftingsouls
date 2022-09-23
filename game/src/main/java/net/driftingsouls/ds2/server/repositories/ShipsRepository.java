package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetSectorsWithAttackingShips;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.ScanData;
import net.driftingsouls.ds2.server.map.ShipData;
import net.driftingsouls.ds2.server.map.ShipTypeData;
import net.driftingsouls.ds2.server.map.UserData;
import net.driftingsouls.ds2.server.ships.FleetsOverviewView;
import net.driftingsouls.ds2.server.ships.ShipBookmarkView;
import net.driftingsouls.ds2.server.ships.MoveableShip;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.jooq.Name;
import org.jooq.Query;
import org.jooq.Records;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.*;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.*;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyNebelScanRanges.FRIENDLY_NEBEL_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipFleets.SHIP_FLEETS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Users.USERS;
import static org.jooq.impl.DSL.update;

public class ShipsRepository {



    public static HashSet<Location> getAttackingSectors(int userid){
        var attackingSectors = new HashSet<Location>();
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            var sectorsWithAttackingShips = new GetSectorsWithAttackingShips();
            sectorsWithAttackingShips.setUserid(userid);
            sectorsWithAttackingShips.execute(db.configuration());

            for(var record: sectorsWithAttackingShips.getResults().get(0)) {
                attackingSectors.add(new Location(
                        record.get(SHIPS.STAR_SYSTEM),
                        record.get(SHIPS.X),
                        record.get(SHIPS.Y)
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return attackingSectors;
    }


    public static ArrayList<ScanData> getScanships(int userid, int system)
    {
        var resultlist = new ArrayList<ScanData>();
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var scanDataSelect = db
                    .selectFrom(FRIENDLY_SCAN_RANGES)
                    .where(FRIENDLY_SCAN_RANGES.STAR_SYSTEM.eq(system)
                            .and((FRIENDLY_SCAN_RANGES.TARGET_ID.eq(userid)
                                    .and(FRIENDLY_SCAN_RANGES.STATUS1.eq(2L)
                                    .and(FRIENDLY_SCAN_RANGES.STATUS2.eq(2L))))
                                    .or(FRIENDLY_SCAN_RANGES.OWNER.eq(userid))))
                        ) {
                var result = scanDataSelect.fetch();
                for (var record : result) {
                    var scanData = new ScanData(system, record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorStatus().intValue(), record.getSensorRange().intValue());
                    resultlist.add(scanData);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return resultlist;
    }

    public static ArrayList<ScanData> getNebulaScanships(int userid, int system)
    {
        var nebulaScanships = new ArrayList<ScanData>();
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var scanDataSelect = db
                    .selectFrom(FRIENDLY_NEBEL_SCAN_RANGES)
                    .where(FRIENDLY_NEBEL_SCAN_RANGES.STAR_SYSTEM.eq(system)
                            .and((FRIENDLY_NEBEL_SCAN_RANGES.TARGET_ID.eq(userid)
                                    .and(FRIENDLY_NEBEL_SCAN_RANGES.STATUS1.eq(2L)
                                    .and(FRIENDLY_NEBEL_SCAN_RANGES.STATUS2.eq(2L))))
                                .or(FRIENDLY_NEBEL_SCAN_RANGES.OWNER.eq(userid))))) {
                var result = scanDataSelect.fetch();
                for (var record : result) {
                    var scanData = new ScanData(system, record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorStatus().intValue(), record.getSensorRange().intValue());
                    nebulaScanships.add(scanData);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return nebulaScanships;
    }


    public static List<ShipBookmarkView> getShipBookmarkViewData(int userid)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var bookmarkDataSelect = db
                    .select(
                            SHIPS.ID,
                            SHIPS.STAR_SYSTEM,
                            SHIPS.X,
                            SHIPS.Y,
                            SHIPS.NAME,
                            SHIP_TYPES.NICKNAME,
                            SCHIFF_EINSTELLUNGEN.DESTSYSTEM,
                            SCHIFF_EINSTELLUNGEN.DESTX,
                            SCHIFF_EINSTELLUNGEN.DESTY,
                            SCHIFF_EINSTELLUNGEN.DESTCOM
                            )
                    .from(SHIPS)
                    .innerJoin(SHIP_TYPES).on(SHIP_TYPES.ID.eq(SHIPS.TYPE))
                    .innerJoin(SCHIFF_EINSTELLUNGEN).on(SHIPS.EINSTELLUNGEN_ID.eq(SCHIFF_EINSTELLUNGEN.ID))
                    .where(SHIPS.OWNER.eq(userid))
                    .and(SCHIFF_EINSTELLUNGEN.BOOKMARK.isTrue())) {
                return bookmarkDataSelect.fetch(Records.mapping(ShipBookmarkView::new));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<FleetsOverviewView> getFleetsOverviewViewData(int userid)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var bookmarkDataSelect = db
                    .select(
                            SHIP_FLEETS.ID,
                            SHIP_FLEETS.NAME,
                            DSL.max(SHIPS.ID),
                            SHIPS.STAR_SYSTEM,
                            SHIPS.X,
                            SHIPS.Y,
                            DSL.count(SHIPS),
                            SHIPS.DOCKED
                    )
                    .from(SHIP_FLEETS)
                    .innerJoin(SHIPS).on(SHIP_FLEETS.ID.eq(SHIPS.FLEET))
                    .where(SHIPS.OWNER.eq(userid))
                    .groupBy(SHIP_FLEETS.ID, SHIP_FLEETS.NAME, SHIPS.STAR_SYSTEM, SHIPS.X, SHIPS.Y, SHIPS.DOCKED)
                    .orderBy(SHIPS.DOCKED, SHIP_FLEETS.ID, SHIPS.STAR_SYSTEM, SHIPS.X, SHIPS.Y)
            ) {
                return bookmarkDataSelect.fetch(Records.mapping(FleetsOverviewView::new));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static MoveableShip getMoveableShip(int shipid, int userid)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var moveableShipSelect = db
                    .selectFrom(SHIP_MOVEMENT)
                    .where(SHIP_MOVEMENT.SHIP_ID.eq(shipid))
                    .and(SHIP_MOVEMENT.OWNER.eq(userid))
            ) {
                //var result = bookmarkDataSelect.fetch();
                var moveableShips = moveableShipSelect.fetch(Records.mapping(MoveableShip::new));
                return moveableShips.get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<MoveableShip> getMoveableShips(int shipid, int userid)
    {
        var ship = getMoveableShip(shipid, userid);
        var fleet = ship.getFleet();

        if(fleet == 0)
        {
            var result = new ArrayList<MoveableShip>(1);
            result.add(ship);
            return result;
        }

        if(ship == null) return null;

        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var moveableShipSelect = db
                    .selectFrom(SHIP_MOVEMENT)
                    .where(SHIP_MOVEMENT.FLEET.eq((long)fleet))
                    .and(SHIP_MOVEMENT.STAR_SYSTEM.eq(ship.getLocation().getSystem()))
                    .and(SHIP_MOVEMENT.X.eq(ship.getLocation().getX()))
                    .and(SHIP_MOVEMENT.Y.eq(ship.getLocation().getY()))

            ) {
                //var result = bookmarkDataSelect.fetch();
                var moveableShips = moveableShipSelect.fetch(Records.mapping(MoveableShip::new));
                return moveableShips;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateMovedShips(List<MoveableShip> ships)
    {
        long start = System.nanoTime();

        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            var location = ships.get(0).getLocation();


            var query = db.update(SHIPS)
                    .set(SHIPS.X, location.getX())
                    .set(SHIPS.Y, location.getY());
                    //.set(SHIPS.E, DSL.when(SHIPS.ID.eq(ship.getId()), ship.getEnergy()))
                    //.set(SHIPS.S, location.getX());

            var energy = DSL.when(SHIPS.ID.eq(ships.get(0).getId()), ships.get(0).getEnergy());
            var heat = DSL.when(SHIPS.ID.eq(ships.get(0).getId()), ships.get(0).getHeat());

            var shipIds = new ArrayList<Integer>(ships.size());
            var carrierIds = new ArrayList<String>(ships.size());
            var dockableIds = new ArrayList<String>(ships.size());

            for (MoveableShip ship : ships) {

                location = ship.getLocation();
                energy.when(SHIPS.ID.eq(ship.getId()), ship.getEnergy());
                heat.when(SHIPS.ID.eq(ship.getId()), ship.getHeat());

                shipIds.add(ship.getId());
                carrierIds.add("l " + ship.getId());
                dockableIds.add(String.valueOf(ship.getId()));
            }

            db.update(SHIPS)
                .set(SHIPS.X, location.getX())
                .set(SHIPS.Y, location.getY())
                .where(SHIPS.DOCKED.in(carrierIds))
                .or(SHIPS.DOCKED.in(dockableIds))
                .execute();

            query.set(SHIPS.E, energy)
                .set(SHIPS.S, heat)
                .where(SHIPS.ID.in(shipIds))
                .execute();

            int sum = 0;

            var officerQuery = db.update(OFFIZIERE);
            var officer = ships.get(0).getOfficer();

            var nav = DSL.when(OFFIZIERE.ID.eq(officer.getId()), officer.getNav());
            var navu = DSL.when(OFFIZIERE.ID.eq(officer.getId()), officer.getNavU());
            var ing = DSL.when(OFFIZIERE.ID.eq(officer.getId()), officer.getIng());
            var ingu = DSL.when(OFFIZIERE.ID.eq(officer.getId()), officer.getIngU());

            var officerIds = new ArrayList<Integer>();

            for (MoveableShip ship : ships) {
                officer = ship.getOfficer();
                if(officer.getId() != 0 && officer.getHasChanges())
                {
                    officerIds.add(officer.getId());
                    nav.when(OFFIZIERE.ID.eq(officer.getId()), officer.getNav());
                    navu.when(OFFIZIERE.ID.eq(officer.getId()), officer.getNavU());
                    ing.when(OFFIZIERE.ID.eq(officer.getId()), officer.getIng());
                    ingu.when(OFFIZIERE.ID.eq(officer.getId()), officer.getIngU());
                }
            }
            officerQuery.set(OFFIZIERE.NAV, nav)
                .set(OFFIZIERE.NAVU, navu)
                .set(OFFIZIERE.ING, ing)
                .set(OFFIZIERE.INGU, ingu)
                .where(OFFIZIERE.ID.in(officerIds))
                .execute();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

    }

    public static HashMap<Integer, ShipData.DockedShipCount> getDockedShipsCount
            (Location location)
    {
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try(var scanDataSelect = db
                    .select(SHIPS.DOCKED)
                    .from(SHIPS)
                    .where(SHIPS.STAR_SYSTEM.eq(location.getSystem())
                            .and(SHIPS.X.eq(location.getX()))
                            .and(SHIPS.Y.eq(location.getY()))
                            .and(SHIPS.DOCKED.isNotNull())
                            .and(SHIPS.DOCKED.notEqual(""))
                    )) {
                var result = scanDataSelect.fetch();
                var map = new HashMap<Integer, ShipData.DockedShipCount>();

                for (var record : result) {
                    int carrierId = 0;
                    boolean isLanded = false;

                    String dockedId = record.get(0, String.class);
                    if(dockedId.startsWith("l"))
                    {
                        carrierId = Integer.parseInt(dockedId.substring(2));
                        isLanded = true;
                    }
                    else{
                        carrierId = Integer.parseInt(dockedId);
                    }

                    if(!map.containsKey(carrierId))
                    {
                        map.put(carrierId, new ShipData.DockedShipCount(carrierId, isLanded ? 0 : 1, isLanded ? 1 : 0));
                    }
                    else
                    {
                        var dockedShipCount = map.get(carrierId);
                        if(isLanded) dockedShipCount.landedCount += 1;
                        else dockedShipCount.externalCount +=1;
                    }
                }

                return map;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    public static Map<UserData, Map<ShipTypeData, List<ShipData>>> getShipsInMapSector(Location location, int userid, int minSize)
    {
        Map<UserData, Map<ShipTypeData, List<ShipData>>> ships = new TreeMap<>();
        var docked = getDockedShipsCount(location);
        UserData ownerData = new UserData(0, "", 0);

        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            var select =
                    db.select(SHIPS.ID,
                                    SHIPS.NAME,
                                    SHIPS.OWNER,
                                    USERS.RACE,
                                    SHIPS.E,
                                    SHIPS.S,
                                    SHIPS.DOCKED,
                                    SHIPS.SENSORS,
                                    SHIPS.FLEET,
                                    SHIPS.CARGO,
                                    SHIP_FLEETS.NAME,
                                    SHIP_TYPES.ID,
                                    SHIP_TYPES.NICKNAME,
                                    SHIP_TYPES.PICTURE,
                                    SHIP_TYPES.SIZE,
                                    SHIP_TYPES.JDOCKS,
                                    SHIP_TYPES.ADOCKS,
                                    SHIP_TYPES.EPS,
                                    SHIP_TYPES.COST,
                                    SHIP_TYPES.SENSORRANGE,
                                    USERS.NAME)
                            .from(SHIPS)
                            .join(SHIP_TYPES)
                                .on(SHIPS.TYPE.eq(SHIP_TYPES.ID))
                            .join(USERS)
                                .on(SHIPS.OWNER.eq(USERS.ID))
                            .leftJoin(SHIP_FLEETS)
                                .on(SHIPS.FLEET.eq(SHIP_FLEETS.ID))
                            .leftJoin(SHIPS_MODULES)
                                .on(SHIPS_MODULES.ID.eq(SHIPS.MODULES))
                            .where(SHIPS.STAR_SYSTEM.eq(location.getSystem())
                                    .and(SHIPS.X.eq(location.getX()))
                                    .and(SHIPS.Y.eq(location.getY())))
                            .and((SHIPS_MODULES.SIZE.isNotNull().and(SHIPS_MODULES.SIZE.greaterOrEqual(minSize)).or(SHIPS_MODULES.SIZE.isNull().and(SHIP_TYPES.SIZE.greaterOrEqual(minSize)))))
                    ;

            var landedShips = new HashMap<Integer, List<ShipData>>();
            for(var row: select.fetch()) {
                ShipData.DockedShipCount dockedCount;
                if(!docked.containsKey(row.get(SHIPS.ID)))
                {
                    dockedCount = new ShipData.DockedShipCount(row.get(SHIPS.ID), 0, 0);
                }
                else
                {
                    dockedCount = docked.get(row.get(SHIPS.ID));
                }

                var ship = new ShipData(row.get(SHIPS.ID), row.get(SHIPS.NAME), row.get(SHIPS.OWNER), row.get(USERS.RACE), row.get(SHIPS.OWNER) == userid ? dockedCount.landedCount : 0, dockedCount.externalCount, row.get(SHIPS.E), row.get(SHIPS.S), row.get(SHIPS.DOCKED), row.get(SHIPS.SENSORS), row.get(SHIPS.FLEET), row.get(SHIP_FLEETS.NAME), row.get(SHIP_TYPES.ID), row.get(SHIPS.CARGO));
                if(ship.isLanded)
                {
                    if(!landedShips.containsKey(ship.carrierId)) landedShips.put(ship.carrierId, new ArrayList<>());
                    landedShips.get(ship.carrierId).add(ship);
                }


                var typeData = new ShipTypeData(row.get(SHIP_TYPES.ID), row.get(SHIP_TYPES.NICKNAME), row.get(SHIP_TYPES.PICTURE), row.get(SHIP_TYPES.SIZE), row.get(SHIP_TYPES.JDOCKS), row.get(SHIP_TYPES.ADOCKS), row.get(SHIP_TYPES.EPS), row.get(SHIP_TYPES.COST), row.get(SHIP_TYPES.SENSORRANGE));
                var userData = new UserData(row.get(SHIPS.OWNER), Common._text(row.get(USERS.NAME)) , row.get(USERS.RACE));

                if(userData.id == userid) ownerData = userData;

                if(ship.isLanded) continue;

                ships.computeIfAbsent(userData, data -> new TreeMap<>())
                        .computeIfAbsent(typeData, data -> new ArrayList<>())
                        .add(ship);

            }

            if(ships.containsKey(ownerData))
            {
                for (var shiptypes: ships.get(ownerData).values()) {
                    for (var ship:shiptypes
                         ) {
                        if(landedShips.containsKey(ship.id))
                        {
                            ship.landedShips.addAll(landedShips.get(ship.id));
                        }
                    }
                }
            }



        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ships;
    }

    public static Map<Integer, ShipTypeData> getShipTypesData()
    {
        var map = new HashMap<Integer, ShipTypeData>();
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            var select =
                    db.select(SHIP_TYPES.ID, SHIP_TYPES.NICKNAME, SHIP_TYPES.PICTURE, SHIP_TYPES.SIZE, SHIP_TYPES.JDOCKS, SHIP_TYPES.ADOCKS, SHIP_TYPES.EPS, SHIP_TYPES.COST, SHIP_TYPES.SENSORRANGE)
                            .from(SHIP_TYPES);

            for(var row: select.fetch()) {
                var typeData = new ShipTypeData(row.get(SHIP_TYPES.ID), row.get(SHIP_TYPES.NICKNAME), row.get(SHIP_TYPES.PICTURE), row.get(SHIP_TYPES.SIZE), row.get(SHIP_TYPES.JDOCKS), row.get(SHIP_TYPES.ADOCKS), row.get(SHIP_TYPES.EPS), row.get(SHIP_TYPES.COST), row.get(SHIP_TYPES.SENSORRANGE));
                map.put(typeData.id, typeData);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static Set<Location> getRockPositions(int system)
    {

        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            var rockSelect = db.select(SHIPS.X, SHIPS.Y)
                    .from(SHIPS)
                    .join(SHIP_TYPES)
                    .on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
                    .where(SHIPS.STAR_SYSTEM.eq(system));

            try(rockSelect; var rocks = rockSelect.stream()) {
                var rockPositions = rocks
                        .map(rock -> new Location(system, rock.value1(), rock.value2()))
                        .collect(toUnmodifiableSet());
                return rockPositions;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }
}
