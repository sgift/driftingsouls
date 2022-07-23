package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
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
import static net.driftingsouls.ds2.server.entities.jooq.Tables.NEBEL;
import static net.driftingsouls.ds2.server.entities.jooq.Tables.USERS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;
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
	private Map<Location, List<BaseData>> baseMap;
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

			try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
				var db = DBUtil.getDSLContext(conn);
				try(var basesSelect = db
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
					.where(BASES.STAR_SYSTEM.eq(this.system)))
				{
					var result = basesSelect.fetch();
					this.baseMap = buildBaseMap(result);
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
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
		// Optimized for common case that a system has at least one nebula
		if(nebulaMap.isEmpty()) {
			synchronized (nebulaMap) {
				if(nebulaMap.isEmpty()) {
					try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
						var db = DBUtil.getDSLContext(conn);
						var select = db.select(NEBEL.X, NEBEL.Y, NEBEL.TYPE)
							.from(NEBEL)
							.where(NEBEL.STAR_SYSTEM.eq(system));
						try(select) {
							for(var record: select.fetch()) {
								var nebula = Nebel.Typ.getType(record.get(NEBEL.TYPE));
								var location = new Location(system, record.get(NEBEL.X), record.get(NEBEL.Y));
								nebulaMap.put(location, nebula);
							}
						}
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return Collections.unmodifiableMap(nebulaMap);
	}

	protected Map<Location, List<BaseData>> buildBaseMap(List<org.jooq.Record8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, String>> bases)
	{
		Map<Location, List<BaseData>> baseMap = new HashMap<>();

		for(var base: bases)
		{
			Location position = new Location(base.get(BASES.STAR_SYSTEM), base.get(BASES.X), base.get(BASES.Y));

			if(!baseMap.containsKey(position))
			{
				baseMap.put(position, new ArrayList<>());
			}

			int size = base.get(BASES.SIZE);
			if(size > 0)
			{
				for(int y = position.getY() - size; y <= position.getY() + size; y++)
				{
					for(int x = position.getX() - size; x <= position.getX() + size; x++)
					{
						Location loc = new Location(position.getSystem(), x, y);

						if( !position.sameSector( 0, loc, base.get(BASES.SIZE) ) ) {
							continue;
						}

						if(!baseMap.containsKey(loc))
						{
							baseMap.put(loc, new ArrayList<>());
						}


						baseMap.get(loc).add(0, base.into(BaseData.class)); //Big objects are always printed first
					}
				}
			}
			else
			{
				baseMap.get(position).add(base.into(BaseData.class));
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
