package net.driftingsouls.ds2.server.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
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
	private final Map<Location, Ship> scannableLocations;
	private final User user;
	
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 */
	public PlayerStarmap(Session db, User user, StarSystem system) {
		this(db, user, system, null);
	}
	
	/**
	 * Legt eine neue Sicht an.
	 * 
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zu Grunde liegenden Sternensystems.
	 * @param ausschnitt Der Ausschnitt (x, y, w, h) auf den die Sicht beschraenkt werden soll.
	 */
	public PlayerStarmap(Session db, User user, StarSystem system, int[] ausschnitt)
	{
		super(db, system);
		
		if( ausschnitt != null ) {
			this.map = new ClippedStarmap(db, user, this.map, ausschnitt);
		}
		this.user = user;
		if(this.user == null)
		{
			throw new IllegalArgumentException("User may not be null.");
		}
		
		this.scannableLocations = buildScannableLocations(user);
	}

	/**
	 * Gibt an, ob der Spieler einen bestimmten Sektor scannen kann.
	 * 
	 * @param location Der Sektor.
	 * @return <code>true</code>, wenn der Spieler den Sektor scannen kann, sonst <code>false</code>
	 */
	public boolean isScannable(Location location)
	{
		return this.scannableLocations.containsKey(location);
	}

    /**
     * @param location Der Sektor, der gescannt werden soll.
     *
     * @return Das Schiff, dass diesen Sektor scannt.
     */
    public Ship getSectorScanner(Location location)
    {
        return this.scannableLocations.get(location);
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
		final String baseImage = getBaseImage(location);
		final String shipImage = getShipImage(location);
		return baseImage + (shipImage != null ? shipImage : "") + ".png";
	}
	
	/**
	 * Gibt ein evt. abweichendes Basisbild des Sektors aus Sicht des Benutzers zurueck. Das Bild enthaelt
	 * keine Flottenmarkierungen. Falls kein abweichendes Basisbild existiert
	 * wird <code>null</code> zurueckgegeben.
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis oder <code>null</code>.
	 */
	public String getUserSectorBaseImage(Location location)
	{
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			Base base = positionBases.get(0);
			String img = base.getOverlayImage(location, user, isScannable(location));
			if( img != null ) {
				return img+".png";
			}
		}
		
		List<JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			if(scannableLocations.containsKey(location))
			{
				for(JumpNode node: positionNodes)
				{
					if(node.isHidden())
					{
						return "jumpnode/jumpnode";
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Gibt das Overlay-Bild des Sektors zurueck. Dieses
	 * enthaelt ausschliesslich spielerspezifische Markierungen
	 * und keinerlei Hintergrundelemente. Der Hintergrund
	 * des Bilds ist transparent.
	 * 
	 * Falls keine Overlay-Daten fuer den Sektor angezeigt werden sollen
	 * wird <code>null</code> zurueckgegeben.
	 * 
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis oder <code>null</code>
	 */
	public String getSectorOverlayImage(Location location)
	{
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
					Relations relations = user.getRelations();
					if((shipAlly != null && shipAlly.equals(userAlly)) || (relations.toOther.get(ship.getOwner()) == Relation.FRIEND && relations.fromOther.get(ship.getOwner()) == Relation.FRIEND))
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
						
						if( scannable && nebula != null && nebula.getMinScanableShipSize() > ship.getTypeData().getSize() )
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
		Relations relations = user.getRelations();
		Map<Location, Ship> scannableLocations = new HashMap<Location, Ship>();

		for(Map.Entry<Location, List<Ship>> sectorShips: this.map.getShipMap().entrySet())
		{
			Location position = sectorShips.getKey();
			List<Ship> ships = sectorShips.getValue();

			int scanRange = -1;
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

				int shipScanRange = ship.getEffectiveScanRange();
				if(shipScanRange > scanRange)
				{
					scanRange = shipScanRange;
                    scanShip = ship;
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
						scannableLocations.put(loc, scanShip);
					}
					else
					{
						if(loc.equals(position))
						{
							Nebel nebula = nebulas.get(position);
							if(!nebula.isEmp())
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

	/**
	 * Gibt zurueck, ob der Sektor einen fuer den Spieler theoretisch sichtbaren Inhalt besitzt.
	 * Es spielt dabei einzig der Inhalt des Sektors eine Rolle. Nicht gerpueft wird,
	 * ob sich ein entsprechendes Schiff in scanreichweite befindet.
	 * @param position Die Position
	 * @return <code>true</code>, falls der Sektor sichtbaren Inhalt aufweist.
	 */
	public boolean isHasSectorContent(Location position)
	{
		List<Base> bases = map.getBaseMap().get(position);
		if( bases != null && !bases.isEmpty() )
		{
			return true;
		}
		return this.getShipImage(position) != null;
	}
}
