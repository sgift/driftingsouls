package net.driftingsouls.ds2.server.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.entities.ally.Ally;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.hibernate.Session;

/**
 * Eine Sicht auf eine bestimmte Sternenkarte.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Schiffe, Rechte, etc.
 * 
 * @author Sebastian Gift
 */
public class PlayerStarmap extends PublicStarmap
{
	static final boolean ALLOW_NEBULA_SCANS = true;

	private final Map<Location, Ship> scannableLocations;
	private final Set<Location> bekannteOrte;
	private final User user;
	private final Relations relations;
	
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 */
	public PlayerStarmap(User user, StarSystem system) {
		this(user, system, null);
	}
	
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 * @param ausschnitt Der Ausschnitt (x, y, w, h) auf den die Sicht beschraenkt werden soll.
	 */
	public PlayerStarmap(User user, StarSystem system, int[] ausschnitt)
	{
		super(system);
		
		if( ausschnitt != null ) {
			this.map = new ClippedStarmap(user, this.map, ausschnitt);
		}
		this.user = user;
		if(this.user == null)
		{
			throw new IllegalArgumentException("User may not be null.");
		}
		this.relations = user.getRelations();

		this.scannableLocations = buildScannableLocations(user);
		this.bekannteOrte = findeBekannteOrte(user);
	}

	private Set<Location> findeBekannteOrte(User user)
	{
		Set<Location> result = new HashSet<Location>();
		for (JumpNode jumpNode : this.map.getJumpNodes())
		{
			if( jumpNode.isHidden() )
			{
				continue;
			}
			result.add(jumpNode.getLocation());
		}

		for (Map.Entry<Location, List<Base>> loc : this.map.getBaseMap().entrySet())
		{
			for (Base base : loc.getValue())
			{
				User owner = base.getOwner();
				if( owner.getId() == user.getId() )
				{
					result.add(loc.getKey());
				}
				else if( user.getAlly() != null && user.getAlly().getShowAstis() && owner.getAlly() != null &&
						user.getAlly().getId() == owner.getAlly().getId() )
				{
					result.add(loc.getKey());
				}
				else
				{
					if( relations.isOnly(owner, Relation.FRIEND) )
					{
						result.add(loc.getKey());
					}
				}
			}
		}

		return result;
	}

	@Override
	public boolean isScannable(Location location)
	{
		return this.scannableLocations.containsKey(location) || this.bekannteOrte.contains(location);
	}

    @Override
    public Ship getSectorScanner(Location location)
    {
        return this.scannableLocations.get(location);
    }

	@Override
	public String getUserSectorBaseImage(Location location)
	{
		boolean scannable = scannableLocations.containsKey(location);
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			for (Base base : positionBases)
			{
				if( scannable || base.getOwner().getId() == this.user.getId() ||
						(user.getAlly() != null && user.getAlly().getShowAstis() && user.getAlly().equals(base.getOwner().getAlly())) ||
						relations.isOnly(base.getOwner(), Relation.FRIEND) )
				{
					String img = base.getOverlayImage(location, user, isScannable(location));
					if( img != null ) {
						return img+".png";
					}
				}
			}
		}
		
		List<JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			for(JumpNode node: positionNodes)
			{
				if(!node.isHidden() || scannableLocations.containsKey(location))
				{
					return "jumpnode/jumpnode.png";
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String getSectorOverlayImage(Location location)
	{
		if( !this.scannableLocations.containsKey(location) )
		{
			return null;
		}
		final String shipImage = getShipImage(location);
		if( shipImage == null )
		{
			return null;
		}
		return "fleet/fleet"+shipImage+".png";
	}
	
	private String getShipImage(Location location)
	{
		String imageName = "";
		//Fleet attachment
		List<Ship> sectorShips = this.map.getShipMap().get(location);
		int ownShips = 0;
		int alliedShips = 0;
		int enemyShips = 0;
		int unscannableEnemyShips = 0;
		Nebel nebula = this.map.getNebulaMap().get(location);

		if(sectorShips != null && !sectorShips.isEmpty())
		{
			for(Ship ship: sectorShips)
			{
				User shipOwner = ship.getOwner();
				if(shipOwner.equals(user))
				{
					ownShips++;
				}
				else 
				{
					Ally shipAlly = shipOwner.getAlly();
					Ally userAlly = user.getAlly();
					if((shipAlly != null && shipAlly.equals(userAlly)) || relations.isOnly(ship.getOwner(), Relation.FRIEND))
					{
						alliedShips++;
					}
					else
					{
						boolean scannable = true;
						
						if(ship.getTypeData().hasFlag(ShipTypes.SF_SEHR_KLEIN) )
						{
							scannable = false;
						}
						
						if( scannable && nebula != null &&
								nebula.getType().getMinScanbareSchiffsgroesse() > ship.getTypeData().getSize() )
						{
							scannable = false;
						}
						
						if( scannable && ship.isDocked() )
						{
							Ship mship = ship.getBaseShip();
							if( mship.getTypeData().hasFlag(ShipTypes.SF_SEHR_KLEIN)) 
							{
								scannable = false;
							}
						}
						
						if( !scannable )
						{
							unscannableEnemyShips++;
						}
						else
						{
							enemyShips++;
						}
					}
				}
			}
			
			if(isScannable(location))
			{
				//Ships which are small etc. are shown in fields with own or allied ships
				if(ownShips > 0 || alliedShips > 0)
				{
					enemyShips += unscannableEnemyShips;
				}
				
				if(ownShips > 0)
				{
					imageName += "_fo";
				}
	
				if(alliedShips > 0)
				{
					imageName += "_fa";
				}
	
				if(enemyShips > 0)
				{
					imageName += "_fe";
				}
			}
		}
		
		if( imageName.isEmpty() )
		{
			return null;
		}
		
		return imageName;
	}
	
	private Map<Location, Ship> buildScannableLocations(User user)
	{
		Map<Location, Nebel> nebulas = this.map.getNebulaMap();
		Ally ally = user.getAlly();
		Map<Location, Ship> scannableLocations = new HashMap<Location, Ship>();

		for(Map.Entry<Location, List<Ship>> sectorShips: this.map.getShipMap().entrySet())
		{
			Location position = sectorShips.getKey();
			List<Ship> ships = sectorShips.getValue();

			Ship scanShip = findBestScanShip(user, ally, ships);
			
			//No ship found
			if(scanShip == null)
			{
				continue;
			}

			int scanRange = scanShip.getEffectiveScanRange();

			//Adjust for nebula position
			//TODO: Check, if there's an performant way to bring this part into the Ship and/or the Location class
			if(nebulas.containsKey(position))
			{
				scanRange /= 2;
				
				Nebel nebula = nebulas.get(position);
				if(!nebula.allowsScan())
				{
					continue;
				}
			}

			//Find sectors scanned from ship
			for(int y = position.getY() - scanRange; y <= position.getY() + scanRange; y++)
			{
				for(int x = position.getX() - scanRange; x <= position.getX() + scanRange; x++)
				{
					Location loc = new Location(map.getSystem(), x, y);

					if(!position.sameSector(scanRange, loc, 0)) 
					{
						continue;	
					}

					//No nebula scan
					if(!nebulas.containsKey(loc))
					{
						scannableLocations.put(loc, scanShip);
					}
					else
					{
						if(ALLOW_NEBULA_SCANS || loc.equals(position))
						{
							Nebel nebula = nebulas.get(loc);
							if(nebula.allowsScan())
							{
								scannableLocations.put(loc, scanShip);
							}
						}
					}
				}
			}

            //Ships in sector always get priority for this sector to ensure best scan result for sector scans
            scannableLocations.put(scanShip.getLocation(), scanShip);
		}
		return scannableLocations;
	}

	private Ship findBestScanShip(User user, Ally ally,  List<Ship> ships)
	{
		int curScanRange = -1;
		Ship scanShip = null;
		//Find ship with best scanrange
		for(Ship ship: ships)
		{
			//Own ship?
			if(!ship.getOwner().equals(user))
			{
				//See allied scans?
				if(ally != null && ally.getShowLrs())
				{
					//Allied ship?
					Ally ownerAlly = ship.getOwner().getAlly();
					if(ownerAlly == null || !ownerAlly.equals(ally))
					{
						continue;
					}
				}
				else
				{
					if( !relations.isOnly(ship.getOwner(), Relation.FRIEND) )
					{
						continue;
					}
				}
			}

			if(ship.getOwner().isInVacation())
			{
				continue;
			}

			int shipScanRange = ship.getEffectiveScanRange();
			if(shipScanRange > curScanRange)
			{
				curScanRange = shipScanRange;
				scanShip = ship;
			}
		}
		return scanShip;
	}

	/**
	 * Gibt zurueck, ob der Sektor einen fuer den Spieler theoretisch sichtbaren Inhalt besitzt.
	 * Es spielt dabei einzig der Inhalt des Sektors eine Rolle. Nicht gerpueft wird,
	 * ob sich ein entsprechendes Schiff in scanreichweite befindet.
	 * @param position Die Position
	 * @return <code>true</code>, falls der Sektor sichtbaren Inhalt aufweist.
	 */
	@Override
	public boolean isHasSectorContent(Location position)
	{
		List<Base> bases = map.getBaseMap().get(position);
		if( bases != null && !bases.isEmpty()  )
		{
			return true;
		}

		List<JumpNode> nodes = map.getNodeMap().get(position);
		return nodes != null && !nodes.isEmpty() || this.getShipImage(position) != null;
	}

	@Override
	public boolean isBattleAtLocation(Location loc)
	{
		if( !this.scannableLocations.containsKey(loc) )
		{
			return false;
		}

		List<Battle> battles = this.map.getBattleMap().get(loc);
		return battles != null && !battles.isEmpty();
	}
}
