package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.services.SingleUserRelationsService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Die Adminsicht auf die Sternenkarte. Zeigt alle
 * Basen, Schiffe und Sprungpunkte an.
 */
public class AdminStarmap extends PlayerStarmap
{
	private final User adminUser;
	public AdminStarmap(int systemId, User adminUser)
	{
		super(adminUser, systemId);

		this.userRelationsService = new SingleUserRelationsService(adminUser.getId());
		this.adminUser = adminUser;
		buildFriendlyData();
	}

	@Override
	public boolean isScanned(Location location)
	{
		return true;
	}

	@Override
	public int getScanningShip(Location location)
	{
		if(scanMap.containsKey(location)) {
			return scanMap.get(location).getShipId();
		}

		return -1;
	}

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
	{
		List<BaseData> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			BaseData base = positionBases.get(0);
			boolean isBaseOwnerEnemy = this.userRelationsService.beziehungVon(base.getOwnerId()) == User.Relation.ENEMY ||
				this.userRelationsService.beziehungZu(base.getOwnerId()) == User.Relation.ENEMY;
			String img = base.getOverlayImage(location, adminUser, true, this.userRelationsService.isMutualFriendTo(base.getOwnerId()), isBaseOwnerEnemy);
			if( img != null ) {
				return new SectorImage(img, 0, 0);
			}
		}

		List<Starmap.JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
		}

		if(map.getRockPositions().contains(location))
		{
			return new SectorImage("data/starmap/base/brocken.png", 0, 0);
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

	@Override
	protected void retainSectors(Set<Location> candidateSectors, HashSet<Location> attackingSectors){}

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
		List<BaseData> bases = map.getBaseMap().get(position);
		return bases != null && !bases.isEmpty() || this.getShipImage(position) != null || map.getRockPositions().contains(position);
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		return this.map.getBattlePositions().contains(sektor);
	}
}
