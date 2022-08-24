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
import org.jooq.Records;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.driftingsouls.ds2.server.entities.jooq.Tables.*;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyNebelScanRanges.FRIENDLY_NEBEL_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;

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
        try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            db.batched(c -> {
                for (MoveableShip ship : ships) {

                    var location = ship.getLocation();
                    c.dsl().update(SHIPS)
                            .set(SHIPS.X, location.getX())
                            .set(SHIPS.Y, location.getY())
                            .set(SHIPS.E, ship.getEnergy())
                            .set(SHIPS.S, ship.getHeat())
                            .where(SHIPS.ID.eq(ship.getId()))
                            .or(SHIPS.DOCKED.eq(String.valueOf(ship.getId())))
                            .execute(); // This doesn't execute the query yet

                    c.dsl().update(SHIPS)
                            .set(SHIPS.X, location.getX())
                            .set(SHIPS.Y, location.getY())
                            .where(SHIPS.DOCKED.eq(String.valueOf(ship.getId())))
                            .or(SHIPS.DOCKED.eq("l " + String.valueOf(ship.getId())))
                            .execute(); // This doesn't execute the query yet

                    var officer = ship.getOfficer();
                    if(officer.getId() != 0 && officer.getHasChanges())
                    {
                        /*

                        double rangf = (this.ing+this.waf+this.nav+this.sec+this.com)/5.0;
                        int rang = (int)(rangf/125);
                        if( rang > Offiziere.MAX_RANG ) {
                        rang = Offiziere.MAX_RANG;

                         */

                        c.dsl().update(OFFIZIERE)
                                .set(OFFIZIERE.NAV, officer.getNav())
                                .set(OFFIZIERE.NAVU, officer.getNavU())
                                .set(OFFIZIERE.ING, officer.getIng())
                                .set(OFFIZIERE.INGU, officer.getIngU())
                                //.set(OFFIZIERE.RANG, DSL.least(Offiziere.MAX_RANG, DSL.sum<int>(OFFIZIERE.COM, OFFIZIERE.ING, OFFIZIERE.NAV, OFFIZIERE.SEC, OFFIZIERE.WAF) / 5))
                                .where(OFFIZIERE.ID.eq(officer.getId()))
                                .execute();
                    }
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
