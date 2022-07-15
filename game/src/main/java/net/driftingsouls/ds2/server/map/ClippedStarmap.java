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
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
	private static final Logger log = LoggerFactory.getLogger(ClippedStarmap.class);

	private final Starmap inner;
	private final int[] ausschnitt;
	private final Map<Location, List<Ship>> clippedShipMap;
	private final Map<Location, List<Ship>> clippedBrockenMap;
	private final Map<Location, Nebel> clippedNebulaMap;
	private final Map<Location, List<Base>> clippedBaseMap;
	private final Session db;

	/**
	 * Konstruktor.
	 * @param inner Das zu Grunde liegende eigentliche Sternensystem
	 * @param ausschnitt Der gewaehlte Ausschnitt <code>[x, y, w, h]</code>
	 */
	public ClippedStarmap(Starmap inner, int[] ausschnitt)
	{
		super(inner.getSystem());
		this.db = ContextMap.getContext().getDB();
		this.inner = inner;
		this.ausschnitt = ausschnitt.clone();
		this.clippedShipMap = this.buildClippedShipMap();
		this.clippedNebulaMap = this.buildClippedNebulaMap();
		this.clippedBaseMap = this.buildClippedBaseMap();
		this.clippedBrockenMap = this.buildClippedBrockenMap();
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
	Map<Location, List<Ship>> getShipMap()
	{
		return Collections.unmodifiableMap(this.clippedShipMap);
	}

	@Override
	Map<Location, List<Ship>> getBrockenMap()
	{
		return Collections.unmodifiableMap(this.clippedBrockenMap);
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

	private Map<Location, List<Ship>> buildClippedShipMap()
	{
		// Nur solche Schiffe laden, deren LRS potentiell in den Ausschnitt hinein ragen oder die
		// sich komplett im Ausschnitt befinden.
		// TODO: Die Menge der Schiffe laesst sich sicherlich noch weiter eingrenzen
		long start = System.nanoTime();
		List<Ship> shipList = Common.cast(db.createQuery("select s from Ship as s left join s.modules m left join s.shiptype st" +
				" where s.system=:sys and s.shiptype.shipClass!=:shipClass and s.docked not like 'l %' and " +
				"((s.x between :minx-st.sensorRange and :maxx+st.sensorRange) or" +
				"(s.x between :minx-m.sensorRange and :maxx+m.sensorRange)) and " +
				"((s.y between :miny-st.sensorRange and :maxy+st.sensorRange) or" +
				"(s.x between :miny-m.sensorRange and :maxy+m.sensorRange))")
			.setInteger("sys", this.inner.getSystem())
			.setInteger("minx", this.ausschnitt[0])
			.setInteger("maxx", this.ausschnitt[0]+this.ausschnitt[2])
			.setInteger("miny", this.ausschnitt[1])
			.setInteger("maxy", this.ausschnitt[1]+this.ausschnitt[3])
			.setParameter("shipClass", ShipClasses.FELSBROCKEN)
			.list());
		long duration = System.nanoTime() - start;
		log.info("Time to load ships in range: {} ms", TimeUnit.NANOSECONDS.toMillis(duration));

		return this.buildLocatableMap(shipList);
	}

	private Map<Location, List<Ship>> buildClippedBrockenMap()
	{
		// Nur solche Brocken laden, deren LRS potentiell in den Ausschnitt hinein ragen oder die
		// sich komplett im Ausschnitt befinden.
		List<Ship> brockenList = Common.cast(db.createQuery("select s from Ship as s left join s.modules m" +
				" where s.system=:sys and s.shiptype.shipClass=:shipClass and " +
				"((s.x between :minx-s.shiptype.sensorRange and :maxx+s.shiptype.sensorRange) or" +
				"(s.x between :minx-m.sensorRange and :maxx+m.sensorRange)) and " +
				"((s.y between :miny-s.shiptype.sensorRange and :maxy+s.shiptype.sensorRange) or" +
				"(s.x between :miny-m.sensorRange and :maxy+m.sensorRange))")
				.setInteger("sys", this.inner.getSystem())
				.setInteger("minx", this.ausschnitt[0])
				.setInteger("maxx", this.ausschnitt[0]+this.ausschnitt[2])
				.setInteger("miny", this.ausschnitt[1])
				.setInteger("maxy", this.ausschnitt[1]+this.ausschnitt[3])
				.setParameter("shipClass", ShipClasses.FELSBROCKEN)
				.list());

		return this.buildLocatableMap(brockenList);
	}

	private Map<Location, Nebel> buildClippedNebulaMap()
	{
		int[] load = new int[] {
				this.ausschnitt[0],
				this.ausschnitt[1],
				this.ausschnitt[0]+this.ausschnitt[2],
				this.ausschnitt[1]+this.ausschnitt[3]
		};

		for( Location loc : this.clippedShipMap.keySet() )
		{
			if( loc.getX() < load[0] )
			{
				load[0] = loc.getX();
			}
			else if( loc.getX() > load[2] )
			{
				load[2] = loc.getX();
			}

			if( loc.getY() < load[1] )
			{
				load[1] = loc.getY();
			}
			else if( loc.getY() > load[3] )
			{
				load[3] = loc.getY();
			}
		}

		List<Nebel> nebelList = Common.cast(db.createQuery("from Nebel " +
				"where loc.system=:sys and " +
				"loc.x between :minx and :maxx and " +
				"loc.y between :miny and :maxy")
			.setInteger("sys", this.inner.getSystem())
			.setInteger("minx", load[0])
			.setInteger("miny", load[1])
			.setInteger("maxx", load[2])
			.setInteger("maxy", load[3])
			.list());

		return this.buildNebulaMap(nebelList);
	}

	private Map<Location, List<Base>> buildClippedBaseMap()
	{
		List<Base> baseList = Common.cast(db.createQuery("from Base "+
				"where system=:sys and " +
				"x between :minx and :maxx and " +
				"y between :miny and :maxy")
			.setInteger("sys", this.inner.getSystem())
			.setInteger("minx", this.ausschnitt[0])
			.setInteger("miny", this.ausschnitt[1])
			.setInteger("maxx", this.ausschnitt[0]+this.ausschnitt[2])
			.setInteger("maxy", this.ausschnitt[1]+this.ausschnitt[3])
			.list());

		return this.buildBaseMap(baseList);
	}
}
