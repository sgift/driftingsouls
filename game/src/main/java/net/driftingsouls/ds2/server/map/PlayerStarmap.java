package net.driftingsouls.ds2.server.map;

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
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final Set<Location> sektorenMitBefreundetenSchiffen;
	private final Set<Location> sektorenMitRotemAlarm;
	private final Set<Location> bekannteOrte;
	private final User user;
	private final Relations relations;

	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 * @param ausschnitt Der gewaehlte Ausschnitt <code>[x, y, w, h]</code> oder <code>null</code>, falls kein Ausschnitt verwendet werden soll
	 */
	public PlayerStarmap(User user, StarSystem system, int[] ausschnitt)
	{
		super(system, ausschnitt);

		this.user = user;
		if(this.user == null)
		{
			throw new IllegalArgumentException("User may not be null.");
		}
		this.relations = user.getRelations();

		this.scannableLocations = new HashMap<>();
		this.sektorenMitBefreundetenSchiffen = new HashSet<>();
		buildScannableLocations(user);

		this.bekannteOrte = findeBekannteOrte(user);
		this.sektorenMitRotemAlarm = findeSektorenMitRotemAlarm(user);
	}

	private Set<Location> findeSektorenMitRotemAlarm(User user)
	{
		Set<Location> zuPruefendeSektoren = new HashSet<>();
		for (Location sektor : this.sektorenMitBefreundetenSchiffen)
		{
			for( int x=-1; x <= 1; x++ )
			{
				for( int y=-1; y <= 1; y++ )
				{
					Location scanSektor = new Location(sektor.getSystem(), sektor.getX()+x, sektor.getY()+y);
					zuPruefendeSektoren.add(scanSektor);
				}
			}
		}

		return Ship.getAlertStatus(user, zuPruefendeSektoren.toArray(new Location[zuPruefendeSektoren.size()]));
	}

	private Set<Location> findeBekannteOrte(User user)
	{
		Set<Location> result = new HashSet<>();
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
	public boolean isScannbar(Location location)
	{
		return this.scannableLocations.containsKey(location) || this.bekannteOrte.contains(location);
	}

    @Override
    public Ship getScanSchiffFuerSektor(Location location)
    {
        return this.scannableLocations.get(location);
    }

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
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
					String img = base.getOverlayImage(location, user, isScannbar(location));
					if( img != null ) {
						return new SectorImage(img, 0, 0);
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
					return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public SectorImage getSectorOverlayImage(Location location)
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
		return new SectorImage("data/starmap/fleet/fleet"+shipImage+".png", 0, 0);
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
						
						if(ship.getTypeData().hasFlag(ShipTypeFlag.SEHR_KLEIN) )
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
							if( mship.getTypeData().hasFlag(ShipTypeFlag.SEHR_KLEIN))
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
			
			if(isScannbar(location))
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
	
	private void buildScannableLocations(User user)
	{
		Map<Location, Nebel> nebulas = this.map.getNebulaMap();
		Ally ally = user.getAlly();

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

			this.sektorenMitBefreundetenSchiffen.add(position);

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

					boolean ok = false;

					//No nebula scan
					if(!nebulas.containsKey(loc))
					{
						ok = true;
					}
					else
					{
						if(ALLOW_NEBULA_SCANS || loc.equals(position))
						{
							Nebel nebula = nebulas.get(loc);
							if(nebula.allowsScan())
							{
								ok = true;
							}
						}
					}

					if( ok )
					{
						// Immer das naechstgelegene Schiff als Scanschiff nehmen um auch Effekte
						// anzeigen zu koennen, die nur im benachbarten Sektor entdeckt werden koennen
						Ship oldShip = scannableLocations.get(loc);
						scannableLocations.put(loc, naechstesSchiff(loc,oldShip,scanShip));
					}
				}
			}

            //Ships in sector always get priority for this sector to ensure best scan result for sector scans
			this.scannableLocations.put(scanShip.getLocation(), scanShip);
		}
	}

	private Ship naechstesSchiff(Location loc, Ship ship1, Ship ship2)
	{
		if( ship1 == null )
		{
			return ship2;
		}
		if( ship2 == null )
		{
			return ship1;
		}

		double distanz1 = ship1.getLocation().distanzZu(loc);
		double distanz2 = ship2.getLocation().distanzZu(loc);
		if( distanz1 <= distanz2 )
		{
			return ship1;
		}
		return ship2;
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
				if (!relations.isOnly(ship.getOwner(), Relation.FRIEND))
				{
					continue;
				}
				if (ally != null && !ally.getShowLrs() && ally.equals(ship.getOwner().getAlly()))
				{
					continue;
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
	public boolean isSchlachtImSektor(Location sektor)
	{
		if( !this.scannableLocations.containsKey(sektor) )
		{
			return false;
		}

		List<Battle> battles = this.map.getBattleMap().get(sektor);
		return battles != null && !battles.isEmpty();
	}

	@Override
	public boolean isRoterAlarmImSektor(Location sektor)
	{
		return this.sektorenMitRotemAlarm.contains(sektor);
	}
}
