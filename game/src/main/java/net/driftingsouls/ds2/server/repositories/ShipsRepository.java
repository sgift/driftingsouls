package net.driftingsouls.ds2.server.repositories;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetSectorsWithAttackingShips;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.map.ScanData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
                    var scanData = new ScanData(system, record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorRange().intValue());
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
                    var scanData = new ScanData(system, record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorRange().intValue());
                    nebulaScanships.add(scanData);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return nebulaScanships;
    }

}
