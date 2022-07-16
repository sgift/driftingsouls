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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;

/**
 * Eine auf einen Teilausschnitt reduzierte Version eines Sternensystems.
 * Implementiert alle Standardfunktionen. Innerhalb des gewaehlten Ausschnitts entspricht
 * die Funktionalitaet der normalen Sternensystem-Klasse. Ausserhalb ist die Funktionsweise nicht
 * definiert.
 * @author Christopher Jung
 *
 */
public class ClippedStarmap extends Starmap
{
	private final Starmap inner;
	private final int[] ausschnitt;
	private final Set<Location> clippedRockMap;
	private final Map<Location, Nebel> clippedNebulaMap;
	private final Map<Location, List<Base>> clippedBaseMap;
	private final EntityManager em;

	/**
	 * Konstruktor.
	 * @param inner Das zugrunde liegende eigentliche Sternensystem
	 * @param ausschnitt Der gewaehlte Ausschnitt <code>[x, y, w, h]</code>
	 */
	public ClippedStarmap(Starmap inner, int[] ausschnitt)
	{
		super(inner.getSystem());
		this.em = ContextMap.getContext().getEM();
		this.inner = inner;
		this.ausschnitt = ausschnitt.clone();
		this.clippedNebulaMap = this.buildClippedNebulaMap();
		this.clippedBaseMap = this.buildClippedBaseMap();
		this.clippedRockMap = this.buildClippedRockMap();
	}

	@Override
	boolean isNebula(Location location)
	{
		return inner.isNebula(location);
	}

	@Override
	int getSystem()
	{
		return inner.getSystem();
	}

	@Override
	Collection<JumpNode> getJumpNodes()
	{
		return inner.getJumpNodes();
	}

	@Override
	Set<Location> getRockPositions()
	{
		return this.clippedRockMap;
	}

	@Override
	Map<Location, List<Base>> getBaseMap()
	{
		return Collections.unmodifiableMap(this.clippedBaseMap);
	}

	@Override
	Map<Location, List<JumpNode>> getNodeMap()
	{
		return inner.getNodeMap();
	}

	@Override
	Map<Location, Nebel> getNebulaMap()
	{
		return Collections.unmodifiableMap(this.clippedNebulaMap);
	}

	private Set<Location> buildClippedRockMap()
	{
		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);

			var rockSelect = db.select(SHIPS.X, SHIPS.Y)
				.from(SHIPS)
				.join(SHIP_TYPES)
				.on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
				.where(SHIPS.STAR_SYSTEM.eq(getSystem())
					.and(SHIPS.X.ge(this.ausschnitt[0]))
					.and(SHIPS.X.le(this.ausschnitt[1]))
					.and(SHIPS.Y.ge(this.ausschnitt[2]))
					.and(SHIPS.Y.le(this.ausschnitt[3])));

			try(rockSelect; var rocks = rockSelect.stream()) {
				return rocks
					.map(rock -> new Location(getSystem(), rock.value1(), rock.value2()))
					.collect(toUnmodifiableSet());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<Location, Nebel> buildClippedNebulaMap()
	{
		List<Nebel> nebelList = em.createQuery("from Nebel " +
				"where loc.system=:sys and " +
				"loc.x between :minx and :maxx and " +
				"loc.y between :miny and :maxy", Nebel.class)
			.setParameter("sys", this.inner.getSystem())
			.setParameter("minx", this.ausschnitt[0])
			.setParameter("miny", this.ausschnitt[1])
			.setParameter("maxx", this.ausschnitt[0]+this.ausschnitt[2])
			.setParameter("maxy", this.ausschnitt[1]+this.ausschnitt[3])
			.getResultList();

		return this.buildNebulaMap(nebelList);
	}

	private Map<Location, List<Base>> buildClippedBaseMap()
	{
		List<Base> baseList = em.createQuery("from Base "+
				"where system=:sys and " +
				"x between :minx and :maxx and " +
				"y between :miny and :maxy", Base.class)
			.setParameter("sys", this.inner.getSystem())
			.setParameter("minx", this.ausschnitt[0])
			.setParameter("miny", this.ausschnitt[1])
			.setParameter("maxx", this.ausschnitt[0]+this.ausschnitt[2])
			.setParameter("maxy", this.ausschnitt[1]+this.ausschnitt[3])
			.getResultList();

		return this.buildBaseMap(baseList);
	}
}
