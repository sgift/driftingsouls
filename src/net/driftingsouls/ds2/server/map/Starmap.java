package net.driftingsouls.ds2.server.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Eine Systemkarte.
 * Die Karte wird intern von den diversen Sichten verwendet, um
 * auf die im System enthaltenen Objekte zuzugreifen.
 * 
 * @author Sebastian Gift
 */
@Table(name="starmap")
@Entity
class Starmap
{
	Starmap()
	{}
	
	/**
	 * Initialisiert die Karte.
	 * Diese Methode muss vor allen anderen aufgerufen werden.
	 * Der Code kann nicht in den Konstruktor verlegt werden,
	 * weil Hibernate die Variablen erst nach Ablauf des Konstruktors setzt.
	 */
	void init()
	{
		shipMap = buildShipMap();
		nebulaMap = buildNebulaMap();
		nodeMap = buildNodeMap();
		baseMap = buildBaseMap();
	}
	
	boolean isNebula(Location location)
	{
		return nebulaMap.containsKey(location);
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
		if(nodes != null)
		{
			return Collections.unmodifiableCollection(this.nodes);
		}
		
		return null;
	}
	
	/**
	 * @return Die Liste der Schiffe im System, sortiert nach Sektoren.
	 */
	Map<Location, List<Ship>> getShipMap()
	{
		return Collections.unmodifiableMap(this.shipMap);
	}
	
	/**
	 * @return Die Liste der Basen im System, sortiert nach Sektoren.
	 */
	Map<Location, List<Base>> getBaseMap()
	{
		return Collections.unmodifiableMap(this.baseMap);
	}
	
	/**
	 * @return Die Liste der Jumpnodes im System, sortiert nach Sektoren.
	 */
	Map<Location, List<JumpNode>> getNodeMap()
	{
		return Collections.unmodifiableMap(this.nodeMap);
	}
	
	/**
	 * @return Die Liste der Schiffe im System, sortiert nach Sektoren.
	 */
	Map<Location, Nebel> getNebulaMap()
	{
		return Collections.unmodifiableMap(this.nebulaMap);
	}
	
	private Map<Location, List<Ship>> buildShipMap()
	{
		Map<Location, List<Ship>> shipMap = new HashMap<Location, List<Ship>>();

		for(Ship ship: ships)
		{
			Location position = ship.getLocation();
			if(!shipMap.containsKey(position))
			{
				shipMap.put(position, new ArrayList<Ship>());
			}

			shipMap.get(position).add(ship);
		}

		return shipMap;
	}
	
	private Map<Location, Nebel> buildNebulaMap()
	{
		Map<Location, Nebel> nebulaMap = new HashMap<Location, Nebel>();

		for(Nebel nebula: nebulas)
		{
			nebulaMap.put(nebula.getLocation(), nebula);
		}

		return nebulaMap;
	}
	
	private Map<Location, List<JumpNode>> buildNodeMap()
	{
		Map<Location, List<JumpNode>> nodeMap = new HashMap<Location, List<JumpNode>>();

		for(JumpNode node: nodes)
		{
			Location position;
			if(node.getSystem() == system)
			{
				position = new Location(node.getSystem(), node.getX(), node.getY());
			}
			else
			{
				position = new Location(node.getSystemOut(), node.getXOut(), node.getYOut());
			}

			if(!nodeMap.containsKey(position))
			{
				nodeMap.put(position, new ArrayList<JumpNode>());
			}

			nodeMap.get(position).add(node);
		}

		return nodeMap;		
	}
	
	private Map<Location, List<Base>> buildBaseMap()
	{
		Map<Location, List<Base>> baseMap = new HashMap<Location, List<Base>>();

		for(Base base: bases)
		{
			Location position = base.getLocation();
			if(!baseMap.containsKey(position))
			{
				baseMap.put(position, new ArrayList<Base>());
			}

			int size = base.getSize();
			if(size > 0)
			{
				for(int y = base.getY() - size; y <= base.getY() + size; y++)
				{
					for(int x = base.getX() - size; x <= base.getX() + size; x++)
					{
						Location loc = new Location(system, x, y);

						if( !position.sameSector( 0, loc, base.getSize() ) ) {
							continue;	
						}

						if(!baseMap.containsKey(loc))
						{
							baseMap.put(loc, new ArrayList<Base>());
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
	
	@Id
	private int system;
	@OneToMany
	@JoinColumn(name="system", nullable=true)
	private List<Ship> ships;
	@OneToMany
	@JoinColumn(name="system", nullable=true)
	private List<Nebel> nebulas;
	@OneToMany
	@JoinColumn(name="system", nullable=true)
	private List<JumpNode> nodes;
	@OneToMany
	@JoinColumn(name="system", nullable=true)
	private List<Base> bases = new ArrayList<Base>();
	
	@Transient
	private Map<Location, List<Ship>> shipMap;
	@Transient
	private Map<Location, Nebel> nebulaMap;
	@Transient
	private Map<Location, List<JumpNode>> nodeMap;
	@Transient
	private Map<Location, List<Base>> baseMap;
}
