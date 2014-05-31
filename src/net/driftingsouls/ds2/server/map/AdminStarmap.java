package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.ships.Ship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Adminsicht auf die Sternenkarte. Zeigt alle
 * Basen, Schiffe und Sprungpunkte an.
 */
public class AdminStarmap extends PublicStarmap
{
	private Map<Location,Ship> scannableLocations;
	private User adminUser;
	/**
	 * Konstruktor.
	 *
	 * @param system Die ID des Systems
	 * @param ausschnitt Der gewaehlte Ausschnitt <code>[x, y, w, h]</code> oder <code>null</code>, falls kein Ausschnitt verwendet werden soll
	 */
	public AdminStarmap(StarSystem system, User adminUser, int[] ausschnitt)
	{
		super(system, ausschnitt);

		this.adminUser = adminUser;
		this.scannableLocations = buildScannableLocations();
	}

	@Override
	public boolean isScannbar(Location location)
	{
		return this.scannableLocations.containsKey(location);
	}

	@Override
	public Ship getScanSchiffFuerSektor(Location location)
	{
		return this.scannableLocations.get(location);
	}

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
	{
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			Base base = positionBases.get(0);
			String img = base.getOverlayImage(location, adminUser, true);
			if( img != null ) {
				return new SectorImage(img, 0, 0);
			}
		}

		List<JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
		}

		return null;
	}

	@Override
	public SectorImage getSectorOverlayImage(Location location)
	{
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

		if(sectorShips != null && !sectorShips.isEmpty())
		{
			for(Ship ship: sectorShips)
			{
				User shipOwner = ship.getOwner();
				if(shipOwner.equals(adminUser))
				{
					ownShips++;
				}
				else
				{
					enemyShips++;
				}
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

		if( imageName.isEmpty() )
		{
			return null;
		}

		return imageName;
	}

	private Map<Location,Ship> buildScannableLocations()
	{
		Map<Location,Ship> locSet = new HashMap<>();
		for (Map.Entry<Location, List<Ship>> locationListEntry : this.map.getShipMap().entrySet())
		{
			locSet.put(locationListEntry.getKey(), locationListEntry.getValue().get(0));
		}

		return locSet;
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
		return bases != null && !bases.isEmpty() || this.getShipImage(position) != null;
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		List<Battle> battles = this.map.getBattleMap().get(sektor);
		return battles != null && !battles.isEmpty();
	}
}
