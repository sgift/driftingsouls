package net.driftingsouls.ds2.server.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.Ally;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.hibernate.Session;

/**
 * Eine Sicht auf eine bestimmte Sternenkarte.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Schiffe, Rechte, etc.
 * 
 * @author Sebastian Gift
 */
public class PlayerStarmap
{
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 */
	public PlayerStarmap(Session db, User user, int system)
	{
		this.map = (Starmap)db.get(Starmap.class, system);
		if(this.map == null)
		{
			throw new IllegalArgumentException("The given system " + system + " does not exist.");
		}
		this.map.init();
		
		this.user = user;
		if(this.user == null)
		{
			throw new IllegalArgumentException("User may not be null.");
		}
		
		this.scannableLocations = buildScannableLocations(user, map.getShipMap(), map.getNebulaMap());
	}
	
	/**
	 * @return Alle fuer den Spieler oeffentlich sichtbaren Jumpnodes.
	 */
	public List<JumpNode> getPublicNodes()
	{
		List<JumpNode> publicNodes = new ArrayList<JumpNode>();
		Collection<JumpNode> nodes = map.getJumpNodes();
		for(JumpNode node: nodes)
		{
			if(!node.isHidden())
			{
				publicNodes.add(node);
			}
		}
		
		return publicNodes;
	}
	
	/**
	 * Gibt an, ob der Spieler einen bestimmten Sektor scannen kann.
	 * 
	 * @param location Der Sektor.
	 * @return <code>true</code>, wenn der Spieler den Sektor scannen kann, sonst <code>false</code>
	 */
	public boolean isScannable(Location location)
	{
		return this.scannableLocations.contains(location);
	}
	
	/**
	 * Gibt das Bild des Sektors zurueck.
	 * Das Bild kann fuer verschiedene Spieler anders sein.
	 * 
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis.
	 */
	public String getSectorImage(Location location)
	{
		return getBaseImage(location) + getShipImage(location);
	}
	
	private boolean isNebula(Location location)
	{
		return map.isNebula(location);
	}
	
	private String getShipImage(Location location)
	{
		String imageName = "";
		//Fleet attachment
		List<Ship> sectorShips = map.getShipMap().get(location);
		int ownShips = 0;
		int alliedShips = 0;
		int enemyShips = 0;
		int unscannableEnemyShips = 0;

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
					Relations relations = user.getRelations();
					if((shipAlly != null && shipAlly.equals(userAlly)) || (relations.toOther.get(ship.getOwner()) == Relation.FRIEND && relations.fromOther.get(ship.getOwner()) == Relation.FRIEND))
					{
						alliedShips++;
					}
					else
					{
						if(ship.getTypeData().hasFlag(ShipTypes.SF_SEHR_KLEIN))
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
		
		return imageName += ".png";
	}
	
	/**
	 * @return Das Bild passend zum Grundtyp des Sektors, ohne Dateiendung (z.B. "space/space")
	 */
	private String getBaseImage(Location location)
	{
		if(isNebula(location))
		{
			return map.getNebulaMap().get(location).getImage();
		}
		else
		{
			List<Base> positionBases = map.getBaseMap().get(location);
			if(positionBases != null && !positionBases.isEmpty())
			{
				Base base = positionBases.get(0);
				return base.getImage(location, user, isScannable(location));
			}
			else 
			{
				List<JumpNode> positionNodes = map.getNodeMap().get(location);
				if(positionNodes != null && !positionNodes.isEmpty())
				{
					if(scannableLocations.contains(location))
					{
						return "jumpnode/jumpnode";
					}
					else
					{
						for(JumpNode node: positionNodes)
						{
							if(!node.isHidden())
							{
								return "jumpnode/jumpnode";
							}
						}
						
						return "space/space";
					}
				}
				else
				{
					return "space/space";
				}
			}
		}
	}
	
	private Set<Location> buildScannableLocations(User user, Map<Location, List<Ship>> locatedShips, Map<Location, Nebel> nebulas)
	{
		Ally ally = user.getAlly();
		Relations relations = user.getRelations();
		Set<Location> scannableLocations = new HashSet<Location>();

		for(Map.Entry<Location, List<Ship>> sectorShips: locatedShips.entrySet())
		{
			Location position = sectorShips.getKey();
			List<Ship> ships = sectorShips.getValue();

			int scanRange = -1;
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
						if(relations.toOther.get(ship.getOwner()) != Relation.FRIEND || relations.fromOther.get(ship.getOwner()) != Relation.FRIEND)
						{
							continue;
						}
					}
				}
				
				if(ship.getOwner().isInVacation())
				{
					continue;
				}

				int shipScanRange = ship.getTypeData().getSensorRange();
				if(shipScanRange > scanRange)
				{
					scanRange = shipScanRange;
				}
			}
			
			//No ship found
			if(scanRange == -1)
			{
				continue;
			}
			
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
						scannableLocations.add(loc);
					}
					else
					{
						if(loc.equals(position))
						{
							Nebel nebula = nebulas.get(position);
							if(!nebula.isEmp())
							{
								scannableLocations.add(loc);
							}
						}
					}
				}
			}
		}
		return scannableLocations;
	}
	
	private final Starmap map;
	private final Set<Location> scannableLocations;
	private final User user;
}
