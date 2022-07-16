package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Locatable;
import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Battles.BATTLES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;

/**
 * Eine Systemkarte.
 * Die Karte wird intern von den diversen Sichten verwendet, um
 * auf die im System enthaltenen Objekte zuzugreifen.
 *
 * @author Sebastian Gift
 */
class Starmap
{
	private final int system;

	private List<JumpNode> nodes;
	private Map<Location, Nebel> nebulaMap;
	private Map<Location, List<JumpNode>> nodeMap;
	private Map<Location, List<Base>> baseMap;
	private Set<Location> battlePositions;
	private Set<Location> rockPositions;

	Starmap(int system)
	{
		this.system = system;
	}

	boolean isNebula(Location location)
	{
		return getNebulaMap().containsKey(location);
	}

	/**
	 * @return Die Nummer des Sternensystems
	 */
	int getSystem()
	{
		return this.system;
	}

	/**
	 * @return Die JumpNodes im System.
	 */
	Collection<JumpNode> getJumpNodes()
	{
		loadNodes();
		return Collections.unmodifiableCollection(this.nodes);
	}

	/**
	 * @return Die Liste der Brocken im System sortiert nach Sektoren.
	 */
	Set<Location> getRockPositions()
	{
		if( this.rockPositions == null ) {
			try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
				var db = DBUtil.getDSLContext(conn);

				var rockSelect = db.select(SHIPS.X, SHIPS.Y)
					.from(SHIPS)
					.join(SHIP_TYPES)
					.on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
					.where(SHIPS.STAR_SYSTEM.eq(system));

				try(rockSelect; var rocks = rockSelect.stream()) {
					rockPositions = rocks
						.map(rock -> new Location(system, rock.value1(), rock.value2()))
						.collect(toUnmodifiableSet());
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		return rockPositions;
	}

	/**
	 * @return Die Liste der Basen im System sortiert nach Sektoren.
	 */
	Map<Location, List<Base>> getBaseMap()
	{
		if( this.baseMap == null ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			List<Base> bases = Common.cast(db
					.createQuery("from Base where system=:system")
					.setInteger("system", this.system)
					.list());

			this.baseMap = buildBaseMap(bases);
		}
		return Collections.unmodifiableMap(this.baseMap);
	}

	/**
	 * @return Die Liste der Sektoren mit Schlachten.
	 */
	Set<Location> getBattlePositions()
	{
		if( this.battlePositions == null ) {
			try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
				var db = DBUtil.getDSLContext(conn);
				var battleSelect = db
					.select(BATTLES.X, BATTLES.Y)
					.from(BATTLES)
					.where(BATTLES.STAR_SYSTEM.eq(system));

				try(battleSelect; var battles = battleSelect.stream()) {
					this.battlePositions = battles
						.map(battle -> new Location(system, battle.value1(), battle.value2()))
						.collect(toUnmodifiableSet());
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		return battlePositions;
	}

	/**
	 * @return Die Liste der Jumpnodes im System, sortiert nach Sektoren.
	 */
	Map<Location, List<JumpNode>> getNodeMap()
	{
		if( this.nodeMap == null ) {
			loadNodes();
			this.nodeMap = buildLocatableMap(nodes);
		}
		return Collections.unmodifiableMap(this.nodeMap);
	}

	private void loadNodes()
	{
		if( this.nodes != null )
		{
			return;
		}
		org.hibernate.Session db = ContextMap.getContext().getDB();
		this.nodes = Common.cast(db
				.createQuery("from JumpNode where system=:system")
				.setInteger("system", this.system)
				.list());
	}

	/**
	 * @return Die Liste der Schiffe im System, sortiert nach Sektoren.
	 */
	Map<Location, Nebel> getNebulaMap()
	{
		if( this.nebulaMap == null ) {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			List<Nebel> nebulas = Common.cast(db
					.createQuery("from Nebel where loc.system=:system")
					.setInteger("system", this.system)
					.list());
			this.nebulaMap = buildNebulaMap(nebulas);
		}
		return Collections.unmodifiableMap(this.nebulaMap);
	}

	protected Map<Location, Nebel> buildNebulaMap(List<Nebel> nebulas)
	{
		Map<Location, Nebel> nebulaMap = new HashMap<>();

		for(Nebel nebula: nebulas)
		{
			nebulaMap.put(nebula.getLocation(), nebula);
		}

		return nebulaMap;
	}

	protected <T extends Locatable> Map<Location, List<T>> buildLocatableMap(List<T> nodes)
	{
		Map<Location, List<T>> nodeMap = new HashMap<>();

		for(T node: nodes)
		{
			Location position = node.getLocation();

			if(!nodeMap.containsKey(position))
			{
				nodeMap.put(position, new ArrayList<>());
			}

			nodeMap.get(position).add(node);
		}

		return nodeMap;
	}

	protected Map<Location, List<Base>> buildBaseMap(List<Base> bases)
	{
		Map<Location, List<Base>> baseMap = new HashMap<>();

		for(Base base: bases)
		{
			Location position = base.getLocation();
			if(!baseMap.containsKey(position))
			{
				baseMap.put(position, new ArrayList<>());
			}

			int size = base.getSize();
			if(size > 0)
			{
				for(int y = base.getY() - size; y <= base.getY() + size; y++)
				{
					for(int x = base.getX() - size; x <= base.getX() + size; x++)
					{
						Location loc = new Location(position.getSystem(), x, y);

						if( !position.sameSector( 0, loc, base.getSize() ) ) {
							continue;
						}

						if(!baseMap.containsKey(loc))
						{
							baseMap.put(loc, new ArrayList<>());
						}

						baseMap.get(loc).add(0, base); //Big objects are always printed first
					}
				}
			}
			else
			{
				baseMap.get(position).add(base);
			}
		}

		return baseMap;
	}
}
