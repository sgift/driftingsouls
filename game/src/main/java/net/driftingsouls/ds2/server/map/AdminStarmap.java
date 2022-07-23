package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.services.SingleUserRelationsService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;

/**
 * Die Adminsicht auf die Sternenkarte. Zeigt alle
 * Basen, Schiffe und Sprungpunkte an.
 */
public class AdminStarmap extends PublicStarmap
{
	private final User adminUser;
	/**
	 * Konstruktor.
	 *
	 * @param system Die ID des Systems
	 * @param ausschnitt Der gewaehlte Ausschnitt <code>[x, y, w, h]</code> oder <code>null</code>, falls kein Ausschnitt verwendet werden soll
	 */
	public AdminStarmap(StarSystem system, User adminUser, int[] ausschnitt)
	{
		super(system, ausschnitt);

		this.UserRelationsService = new SingleUserRelationsService(adminUser.getId());
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
			String img = base.getOverlayImage(location, adminUser, true, this.UserRelationsService.isMutualFriendTo(base.getOwnerId()));
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

	//@Override
	protected void buildFriendlyData()
	{
		var scanMap = new HashMap<Location, ScanData>();
		var ownShipSectors = new HashSet<Location>();
		var allyShipSectors = new HashSet<Location>();
		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			try(var scanDataSelect = db
				.selectFrom(FRIENDLY_SCAN_RANGES)) {
				var result = scanDataSelect.fetch();
				for (var record : result) {
					var scanData = new ScanData(this.map.getSystem(), record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorRange().intValue());

					//FRIENDLY_SCAN_RANGES contains values per sector for best scanner by user and best scanner by ally
					//So we check here which one really has the best scan range
					scanMap.compute(scanData.getLocation(), (k, v) -> {
						if(v == null) {
							return scanData;
						}

						if(scanData.getScanRange() > v.getScanRange()) {
							return scanData;
						} else {
							return v;
						}
					});

					if(scanData.getOwnerId() == adminUser.getId()) {
						ownShipSectors.add(scanData.getLocation());
					} else {
						allyShipSectors.add(scanData.getLocation());
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		this.scanMap = scanMap;
		this.ownShipSectors = ownShipSectors;
		this.allyShipSectors = allyShipSectors;
	}

	private String getShipImage(Location location)
	{
		//TODO: Fix admin view
		return null;
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
		List<BaseData> bases = map.getBaseMap().get(position);
		return bases != null && !bases.isEmpty() || this.getShipImage(position) != null || map.getRockPositions().contains(position);
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		return this.map.getBattlePositions().contains(sektor);
	}
}
