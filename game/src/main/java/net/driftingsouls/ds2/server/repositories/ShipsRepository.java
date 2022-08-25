package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.Offiziere;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetSectorsWithAttackingShips;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.ScanData;
import net.driftingsouls.ds2.server.ships.FleetsOverviewView;
import net.driftingsouls.ds2.server.ships.ShipBookmarkView;
import net.driftingsouls.ds2.server.ships.MoveableShip;
import org.jooq.Name;
import org.jooq.Query;
import org.jooq.Records;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.*;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyNebelScanRanges.FRIENDLY_NEBEL_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
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
                    .where(FRIENDLY_SCAN_RANGES.TARGET_ID.eq(userid)
                            .and(FRIENDLY_SCAN_RANGES.STAR_SYSTEM.eq(system)))) {
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
                    .where(FRIENDLY_NEBEL_SCAN_RANGES.TARGET_ID.eq(userid)
                            .and(FRIENDLY_NEBEL_SCAN_RANGES.STAR_SYSTEM.eq(system)))) {
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
                    .where(SHIPS.OWNER.eq(userid))) {
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
            System.out.println("After ships update: " + String.valueOf(System.nanoTime() - start));

            var officerQuery = db.update(OFFIZIERE);
            //.set(SHIPS.E, DSL.when(SHIPS.ID.eq(ship.getId()), ship.getEnergy()))
            //.set(SHIPS.S, location.getX());

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


            System.out.println("After Officers update: " + String.valueOf(System.nanoTime() - start));
            System.out.println("Number of rows updated:" + String.valueOf(sum));

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        long time = System.nanoTime() - start;
        System.out.println(time);
    }
}
