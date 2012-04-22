package net.driftingsouls.ds2.server.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.hibernate.Session;

/**
 * Eine Sicht auf ein bestimmtes Sternenkartenfeld.
 * Die Sicht geht davon aus, dass der Spieler das Feld sehen darf.
 * Es findet aus Performancegruenden keine(!) Abfrage ab, um das sicherzustellen.
 * 
 * @author Drifting-Souls Team
 */
public class PlayerField 
{
    /**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param position Der gesuchte Sektor.
     * @param scanShip Schiff mit dem der Spieler den Sektor scannt.
	 */
	public PlayerField(Session db, User user, Location position, Ship scanShip)
	{
		this.field = new Field(db, position);
		this.user = user;
        this.scanShip = scanShip;
		this.db = db;
        this.location = position;
	}
	
	/**
	 * @return Die Schiffe, die der Spieler sehen kann.
	 */
	public Map<User, Map<ShipType, List<Ship>>> getShips()
	{
		Map<User, Map<ShipType, List<Ship>>> ships = new HashMap<User, Map<ShipType,List<Ship>>>();
        if(!canUse())
        {
            return ships;
        }

        boolean shipInSector = scanShip.getLocation().sameSector(0, this.location, 0);

		if(!shipInSector && this.field.isNebula() && !this.field.getNebula().allowsScan() )
		{
			return ships;
		}
		
		Ally ally = this.user.getAlly();
		Relations relations = this.user.getRelations();
		Iterator<Ship> viewableShips = field.getShips().iterator();
		while(viewableShips.hasNext())
		{
			final Ship viewableShip = viewableShips.next();
			if( viewableShip.isLanded() )
			{
				continue;
			}
			
			final ShipType type = (ShipType)db.get(ShipType.class, viewableShip.getType());
			final User owner = viewableShip.getOwner();
			
			boolean enemy = false;
			if(!viewableShip.getOwner().equals(this.user))
			{
				if(ally != null)
				{
					Ally ownerAlly = viewableShip.getOwner().getAlly();
					if(ownerAlly == null || !ownerAlly.equals(this.user.getAlly()))
					{
						enemy = true;
					}
				}
				else
				{
					if(relations.toOther.get(viewableShip.getOwner()) != Relation.FRIEND || relations.fromOther.get(viewableShip.getOwner()) != Relation.FRIEND)
					{
						enemy = true;
					}
				}
			}
			
			if( enemy )
			{
                if(!shipInSector)
                {
                    if(type.hasFlag(ShipTypes.SF_SEHR_KLEIN) )
                    {
                        continue;
                    }

                    if( viewableShip.isDocked() )
                    {
                        Ship mship = viewableShip.getBaseShip();
                        if( mship.getTypeData().hasFlag(ShipTypes.SF_SEHR_KLEIN))
                        {
                            continue;
                        }
                    }

                    if( this.field.isNebula() && this.field.getNebula().getMinScanableShipSize() > type.getSize() )
                    {
                        continue;
                    }
                }
			}
					
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
     * @return <code>true</code>, wenn der Spieler das Schiff zum Scannen nutzen darf, <code>false</code> ansonsten.
     */
    private boolean canUse()
    {
        User owner = scanShip.getOwner();
        if(owner.getId() == user.getId())
        {
            return true;
        }
        
        Ally userAlly = user.getAlly();
        Ally ownerAlly = owner.getAlly();
        if(userAlly.getId() == ownerAlly.getId())
        {
            return true;
        }
        
        Relations relations = user.getRelations();
        if(relations.fromOther.get(owner) == Relation.FRIEND && relations.toOther.get(owner) == Relation.FRIEND)
        {
            return true;
        }

        return false;
    }

	private final Session db;
	private final Field field;
    private final User user;
    private final Ship scanShip;
    private final Location location;
}
