/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.USERS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;

/**
 * Eine auf einen Teilausschnitt reduzierte Version eines Sternensystems.
 * Implementiert alle Standardfunktionen. Innerhalb des gewaehlten Ausschnitts entspricht
 * die Funktionalitaet der normalen Sternensystem-Klasse. Ausserhalb ist die Funktionsweise nicht
 * definiert.
 *
 * @author Christopher Jung
 */
public class ClippedStarmap extends Starmap {
    private final Starmap inner;
    private final MapArea mapArea;
    private final Set<Location> clippedRockMap;
    private final Map<Location, Nebel.Typ> clippedNebulaMap;
    private final Map<Location, List<BaseData>> clippedBaseMap;
    private final EntityManager em;

    /**
     * Konstruktor.
     *
     * @param inner   Das zugrunde liegende eigentliche Sternensystem
     * @param mapArea The area of the star map this clipped map represents.
     */
    public ClippedStarmap(Starmap inner, MapArea mapArea) {
        super(inner.getSystem());
        this.em = ContextMap.getContext().getEM();
        this.inner = inner;
        this.mapArea = mapArea;
        this.clippedNebulaMap = this.buildClippedNebulaMap();
        this.clippedBaseMap = this.buildClippedBaseMap();
        this.clippedRockMap = this.buildClippedRockLocations();
    }

    @Override
    boolean isNebula(Location location) {
        return inner.isNebula(location);
    }

    @Override
    int getSystem() {
        return inner.getSystem();
    }

    @Override
    Set<Location> getRockPositions() {
        return this.clippedRockMap;
    }

    @Override
    Map<Location, List<BaseData>> getBaseMap() {
        return Collections.unmodifiableMap(this.clippedBaseMap);
    }

    @Override
    Map<Location, List<JumpNode>> getNodeMap() {
        return inner.getNodeMap();
    }

    @Override
    Map<Location, Nebel.Typ> getNebulaMap() {
        return Collections.unmodifiableMap(this.clippedNebulaMap);
    }

    private Set<Location> buildClippedRockLocations() {
        try (var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);

            var rockSelect = db.select(SHIPS.X, SHIPS.Y)
                .from(SHIPS)
                .join(SHIP_TYPES)
                .on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
                .where(SHIPS.STAR_SYSTEM.eq(getSystem())
                    .and(SHIPS.X.ge(mapArea.getLowerBoundX()))
                    .and(SHIPS.X.le(mapArea.getUpperBoundX()))
                    .and(SHIPS.Y.ge(mapArea.getLowerBoundY()))
                    .and(SHIPS.Y.le(mapArea.getUpperBoundY())));

            try (rockSelect; var rocks = rockSelect.stream()) {
                return rocks
                    .map(rock -> new Location(getSystem(), rock.value1(), rock.value2()))
                    .collect(toUnmodifiableSet());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Location, Nebel.Typ> buildClippedNebulaMap() {
        var clippedNebulaMap = new HashMap<Location, Nebel.Typ>();
        try (var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            var select = db.select(NEBEL.X, NEBEL.Y, NEBEL.TYPE)
                .from(NEBEL)
                .where(NEBEL.STAR_SYSTEM.eq(getSystem())
                    .and(NEBEL.X.between(mapArea.getLowerBoundX(), mapArea.getUpperBoundX()))
                    .and(NEBEL.Y.between(mapArea.getLowerBoundY(), mapArea.getUpperBoundY())));
            try (select) {
                for (var record : select.fetch()) {
                    var nebula = Nebel.Typ.getType(record.get(NEBEL.TYPE));
                    var location = new Location(getSystem(), record.get(NEBEL.X), record.get(NEBEL.Y));
                    clippedNebulaMap.put(location, nebula);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return clippedNebulaMap;
    }

    private Map<Location, List<BaseData>> buildClippedBaseMap() {
        try (var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
            var db = DBUtil.getDSLContext(conn);
            try (var basesSelect = db
                .select(
                    BASES.ID,
                    BASES.OWNER,
                    USERS.ALLY,
                    BASES.STAR_SYSTEM,
                    BASES.X,
                    BASES.Y,
                    BASE_TYPES.SIZE,
                    BASE_TYPES.STARMAPIMAGE
                )
                .from(
                    BASES.innerJoin(BASE_TYPES)
                        .on(BASES.KLASSE.eq(BASE_TYPES.ID))
                        .innerJoin(USERS)
                        .on(USERS.ID.eq(BASES.OWNER))
                )
                .where(BASES.OWNER.notEqual(0)
                    .and(BASES.STAR_SYSTEM.eq(this.getSystem())))
                .and(BASES.X.between(mapArea.getLowerBoundX(), mapArea.getUpperBoundX()))
                .and(BASES.Y.between(mapArea.getLowerBoundY(), mapArea.getUpperBoundY()))

            ) {
                var result = basesSelect.fetch();
                return buildBaseMap(result);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
