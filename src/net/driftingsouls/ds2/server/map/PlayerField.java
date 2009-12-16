package net.driftingsouls.ds2.server.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;

import org.hibernate.Session;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Eine Sicht auf ein bestimmtes Sternenkartenfeld.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Rechte, Schiffe, etc.
 * 
 * @author Sebastian Gift
 */
public class PlayerField 
{
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param position Der gesuchte Sektor.
	 * @param map Die Map des Sternensystems in dem dieses Feld ist.
	 */
	public PlayerField(Session db, User user, Location position, PlayerStarmap map)
	{
		this.field = new Field(db, position);
		this.user = user;
		this.db = db;
		this.map = map;
	}
	
	/**
	 * @return Die Schiffe, die der Spieler sehen kann.
	 */
	public Map<User, Map<ShipType, List<Ship>>> getShips()
	{		
		Iterator<Ship> viewableShips = Iterators.filter(field.getShips().iterator(), new Predicate<Ship>() 
		{
			@Override
			public boolean apply(Ship ship) 
			{
				if(hasShipInSector())
				{
					return true;
				}
				
				if(map.isScannable(field.getPosition()) && field.isScannableInLrs(ship))
				{
					return true;
				}
				
				return false;
			}
		});
		
		Map<User, Map<ShipType, List<Ship>>> ships = new HashMap<User, Map<ShipType,List<Ship>>>();
		while(viewableShips.hasNext())
		{
			Ship viewableShip = viewableShips.next();
			ShipType type = (ShipType)db.get(ShipType.class, viewableShip.getType());
			User owner = viewableShip.getOwner();
			
			if(!ships.containsKey(owner))
			{
				ships.put(owner, new HashMap<ShipType, List<Ship>>());
			}
			
			if(!ships.get(owner).containsKey(type))
			{
				ships.get(owner).put(type, new ArrayList<Ship>());
			}
			
			ships.get(owner).get(type).add(viewableShip);
		}
		
		return ships;
	}
	
	/**
	 * Prueft, ob der Spieler oder einer seiner Verbuendeten ein Schiff im Sektor hat.
	 * 
	 * @return <code>true</code>, wenn es so ein Schiff gibt, sonst <code>false</code>.
	 */
	private boolean hasShipInSector()
	{
		return Iterators.any(field.getShips().iterator(), new Predicate<Ship>()
		{
			@Override
			public boolean apply(Ship ship) 
			{
				if(ship.getOwner().equals(user))
				{
					return true;
				}
				
				if(ship.getOwner().getAlly().equals(user.getAlly()))
				{
					return true;
				}
				
				Relations relations = user.getRelations();
				if(relations.toOther.get(ship.getOwner()) == Relation.FRIEND && relations.fromOther.get(ship.getOwner()) == Relation.FRIEND)
				{
					return true;
				}
				
				return false;
			}
		});
	}

	private final Session db;
	private final Field field;
	private final PlayerStarmap map;
	private final User user;
}
