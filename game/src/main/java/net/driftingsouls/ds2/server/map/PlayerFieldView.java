package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.framework.BasicUser;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Eine Sicht auf ein bestimmtes Sternenkartenfeld.
 * Die Sicht geht davon aus, dass der Spieler das Feld sehen darf.
 * Es findet aus Performancegruenden keine(!) Abfrage ab, um das sicherzustellen.
 * 
 * @author Drifting-Souls Team
 */
public class PlayerFieldView implements FieldView
{
	private final Session db;
	private final Field field;
	private final User user;
	private final Ship scanShip;
	private final Location location;
	private final boolean inScanRange;

    /**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param position Der gesuchte Sektor.
     * @param scanShip Schiff mit dem der Spieler den Sektor scannt.
	 */
	public PlayerFieldView(Session db, User user, Location position, Ship scanShip)
	{
		this.field = new Field(db, position);
		this.user = user;
        this.scanShip = scanShip;
		this.db = db;
        this.location = position;
		this.inScanRange = this.isInScanRange();
	}
	
	/**
	 * Gibt die Liste aller Basen in dem Feld zurueck.
	 * @return Die Basenliste
	 */
	@Override
	public List<Base> getBases()
	{
        boolean shipInSector = isSameSector();

        List<Base> bases = new ArrayList<>();
		for( Base base : this.field.getBases() )
		{
			if( base.getOwner().getId() == this.user.getId() )
			{
				bases.add(base);
			}
			else if( (this.user.getAlly() != null && user.getAlly().equals(base.getOwner().getAlly())) ||
					 this.user.getRelations().isOnly(base.getOwner(), Relation.FRIEND) )
			{
				bases.add(base);
			}
		}
        
		boolean nebula = !shipInSector && this.field.isNebula() && !this.field.getNebula().allowsScan();
		if( nebula )
		{
			nebula = bases.isEmpty();
		}
		
		if( !nebula && this.inScanRange )
		{
			bases.addAll(this.field.getBases().stream().filter(base -> !bases.contains(base)).collect(Collectors.toList()));
		}
		
		return bases;
	}

	private boolean isSameSector()
	{
		return scanShip != null && scanShip.getLocation().sameSector(0, this.location, 0);
	}

	private boolean isInScanRange()
	{
		if(!canUse())
        {
            return false;
        }

        int scanRange = scanShip.getEffectiveScanRange();
        Nebel nebula = (Nebel)db.get(Nebel.class, new MutableLocation(scanShip.getLocation()));
        if(nebula != null)
        {
            scanRange /= 2;
            if(!nebula.allowsScan())
            {
                return false;
            }
        }

        if(!scanShip.getLocation().sameSector(scanRange, location, 0))
        {
            return false;
        }

		Nebel targetNebula = field.getNebula();
		if( targetNebula != null ) {
			if( !targetNebula.allowsScan() ) {
				return false;
			}
			return PlayerStarmap.ALLOW_NEBULA_SCANS || location.equals(scanShip.getLocation());
		}

        return true;
	}
	
	/**
	 * @return Die Schiffe, die der Spieler sehen kann.
	 */
	@Override
	public Map<User, Map<ShipType, List<Ship>>> getShips()
	{
		Map<User, Map<ShipType, List<Ship>>> ships = new TreeMap<>(BasicUser.PLAINNAME_ORDER);
        if(!this.inScanRange)
        {
            return ships;
        }

        boolean shipInSector = isSameSector();

		if(!shipInSector && this.field.isNebula() && !this.field.getNebula().allowsScan() )
		{
			return ships;
		}
		
		Ally ally = this.user.getAlly();
		Relations relations = this.user.getRelations();
		for (Ship viewableShip : field.getShips())
		{
			if (viewableShip.isLanded())
			{
				continue;
			}

			final ShipType type = (ShipType) db.get(ShipType.class, viewableShip.getType());
			final User owner = viewableShip.getOwner();

			boolean enemy = false;
			if (!viewableShip.getOwner().equals(this.user))
			{
				if (ally != null)
				{
					Ally ownerAlly = viewableShip.getOwner().getAlly();
					if (ownerAlly == null || !ownerAlly.equals(this.user.getAlly()))
					{
						enemy = true;
					}
				}
				else
				{
					if( !relations.isOnly(viewableShip.getOwner(), Relation.FRIEND) )
					{
						enemy = true;
					}
				}
			}

			if (enemy)
			{
				if (!shipInSector)
				{
					if (type.hasFlag(ShipTypeFlag.SEHR_KLEIN))
					{
						continue;
					}

					if (viewableShip.isDocked())
					{
						Ship mship = viewableShip.getBaseShip();
						if (mship.getTypeData().hasFlag(ShipTypeFlag.SEHR_KLEIN))
						{
							continue;
						}
					}

					if (this.field.isNebula() &&
							this.field.getNebula().getType().getMinScanbareSchiffsgroesse() > type.getSize())
					{
						continue;
					}
				}
			}

			if (!ships.containsKey(owner))
			{
				ships.put(owner, new HashMap<>());
			}

			if (!ships.get(owner).containsKey(type))
			{
				ships.get(owner).put(type, new ArrayList<>());
			}

			ships.get(owner).get(type).add(viewableShip);
		}
		
		return ships;
	}

	@Override
	public List<JumpNode> getJumpNodes()
	{
		return this.field.getNodes().stream().filter(jumpNode -> !jumpNode.isHidden() || this.inScanRange).collect(Collectors.toList());
	}

	@Override
	public List<Jump> getSubraumspalten()
	{
		if( !this.inScanRange )
		{
			return new ArrayList<>();
		}
		return field.getSubraumspalten();
	}

	@Override
	public List<Battle> getBattles()
	{
		if( !this.inScanRange )
		{
			return new ArrayList<>();
		}
		return this.field.getBattles();
	}

	@Override
	public boolean isRoterAlarm()
	{
		if( this.scanShip == null )
		{
			return false;
		}
		// Nur den Status in benachbarten Sektoren ermitteln
		if( Math.abs(this.scanShip.getLocation().getX()-this.location.getX()) > 1 )
		{
			return false;
		}
		if( Math.abs(this.scanShip.getLocation().getY()-this.location.getY()) > 1 )
		{
			return false;
		}
		return !Ship.getAlertStatus(this.user, this.location).isEmpty();
	}

	@Override
	public Nebel getNebel()
	{
		return field.getNebula();
	}

	/**
     * @return <code>true</code>, wenn der Spieler das Schiff zum Scannen nutzen darf, <code>false</code> ansonsten.
     */
    private boolean canUse()
    {
		if( scanShip == null )
		{
			return false;
		}
        User owner = scanShip.getOwner();
        if(owner.getId() == user.getId())
        {
            return true;
        }
        
        Ally userAlly = user.getAlly();
        Ally ownerAlly = owner.getAlly();
        if( userAlly != null && ownerAlly != null && userAlly.getId() == ownerAlly.getId())
        {
            return true;
        }
        
        Relations relations = user.getRelations();
		return relations.isOnly(owner, Relation.FRIEND);
	}
}
