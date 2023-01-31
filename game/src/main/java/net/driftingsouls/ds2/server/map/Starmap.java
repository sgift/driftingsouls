package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.repositories.*;
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
public class Starmap
{
	private final int system;

	private Set<JumpNode> nodes;
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
			rockPositions = ShipsRepository.getRockPositions(system);
		}
		return rockPositions;
	}

	/**
	 * @return Die Liste der Basen im System sortiert nach Sektoren.
	 */
	Map<Location, List<BaseData>> getBaseMap()
	{
		if( this.baseMap == null ) {
			var bases = BasesRepository.getInstance().getBaseDataBySystem(this.system);
			this.baseMap = this.buildBaseMap(bases);
		}

		return Collections.unmodifiableMap(this.baseMap);
	}

	/**
	 * @return Die Liste der Sektoren mit Schlachten.
	 */
	Set<Location> getBattlePositions()
	{
		if( this.battlePositions == null ) this.battlePositions = BattleRepository.getBattlePositionsInSystem(system);
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

		var newJumpNodes = new HashSet<JumpNode>();
		var jumpnodes = JumpNodeRepository.getInstance().getJumpNodesInSystem(system);

		for (var jumpnode : jumpnodes.values()) {
			newJumpNodes.add(jumpnode);
		}

		this.nodes = Collections.unmodifiableSet(newJumpNodes);
	}

	/**
	 * @return Die Liste der Schiffe im System sortiert nach Sektoren.
	 */
	Map<Location, Nebel.Typ> getNebulaMap()
	{
		return NebulaRepository.getInstance().getNebulaData(this.system);
	}

	protected Map<Location, List<BaseData>> buildBaseMap(List<BaseData> bases)
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
					if(!baseMap.containsKey(location.getKey()))
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

	public static class JumpNode {
		private final int x;
		private final int y;
		private final boolean hidden;

		public JumpNode(int x, int y, boolean hidden) {
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
