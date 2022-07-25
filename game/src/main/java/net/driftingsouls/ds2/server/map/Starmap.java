package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.repositories.BasesRepository;
import net.driftingsouls.ds2.server.repositories.NebulaRepository;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Battles.BATTLES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumpnodes.JUMPNODES;
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

	private Set<JumpNode> nodes;
	private final Map<Location, Nebel.Typ> nebulaMap = new HashMap<>();
	private Map<Location, List<JumpNode>> nodeMap;
	protected Map<Location, List<BaseData>> baseMap;
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
	Map<Location, List<BaseData>> getBaseMap()
	{
		if( this.baseMap == null ) {
			var bases = BasesRepository.getBaseMapBySystem(this.system);
			this.baseMap = this.buildBaseMap(bases);
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

	Set<JumpNode> getNodes() {
		if(nodes == null) {
			loadNodes();
		}
		return nodes;
	}

	/**
	 * @return Die Liste der Jumpnodes im System, sortiert nach Sektoren.
	 */
	Map<Location, List<JumpNode>> getNodeMap()
	{
		if( this.nodeMap == null ) {
			loadNodes();

			var nodeMap = new HashMap<Location, List<JumpNode>>();
			for(var node: nodes) {
				var location = new Location(system, node.getX(), node.getY());
				nodeMap.computeIfAbsent(location, k -> new ArrayList<>()).add(node);
			}
			this.nodeMap = Collections.unmodifiableMap(nodeMap);
		}
		return this.nodeMap;
	}

	private void loadNodes()
	{
		if( this.nodes != null )
		{
			return;
		}

		var nodes = new HashSet<JumpNode>();
		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			var result = db.select(JUMPNODES.X, JUMPNODES.Y, JUMPNODES.HIDDEN)
				.from(JUMPNODES)
				.where(JUMPNODES.STAR_SYSTEM.eq(system))
				.fetch();
			for(var record: result) {
				nodes.add(new JumpNode(record.value1(), record.value2(), record.value3() != 0));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		this.nodes = Collections.unmodifiableSet(nodes);
	}

	/**
	 * @return Die Liste der Schiffe im System sortiert nach Sektoren.
	 */
	Map<Location, Nebel.Typ> getNebulaMap()
	{
		return NebulaRepository.getInstance().getNebulaData(this.system);
	}

	protected Map<Location, List<BaseData>> buildBaseMap(ArrayList<BaseData> bases)
	{
		Map<Location, List<BaseData>> baseMap = new HashMap<>();

		for(var base: bases)
		{
			Location baseCenterLocation = base.getLocation();

			int size = base.getSize();
			if(size > 0)
			{
				var locations = base.getLocationsMap();

				for (var location : locations.entrySet())
				{
					if(!baseMap.containsKey(location))
					{
						baseMap.put(location.getKey(), new ArrayList<>());
					}

					baseMap.get(location.getKey()).add(0, location.getValue()); //Big objects are always printed first
				}
			}
			else
			{
				if(!baseMap.containsKey(baseCenterLocation))
				{
					baseMap.put(baseCenterLocation, new ArrayList<>());
				}
				baseMap.get(baseCenterLocation).add(base);
			}
		}

		return baseMap;
	}

	protected static class JumpNode {
		private final int x;
		private final int y;
		private final boolean hidden;

		protected JumpNode(int x, int y, boolean hidden) {
			this.x = x;
			this.y = y;
			this.hidden = hidden;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public boolean isHidden() {
			return hidden;
		}
	}
}
