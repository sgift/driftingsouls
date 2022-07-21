package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.NonFriendlyShipLocations.NON_FRIENDLY_SHIP_LOCATIONS;

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
		//TODO: Currently admins cannot see into emp nebula since we hardcoded that in the view
		String imageName = "";

		if(isScanned(location))
		{
			boolean scanningShipInSector;
			boolean alliedShips;
			int maxEnemyShipSize;
			int maxNeutralShipSize;

			try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
				var db = DBUtil.getDSLContext(conn);
				var locationCondition = NON_FRIENDLY_SHIP_LOCATIONS.STAR_SYSTEM.eq(location.getSystem())
					.and(NON_FRIENDLY_SHIP_LOCATIONS.X.eq(location.getX()))
					.and(NON_FRIENDLY_SHIP_LOCATIONS.Y.eq(location.getY()));

				scanningShipInSector = scanMap.containsKey(location);

				alliedShips = db.fetchExists(DSL.selectOne().from(FRIENDLY_SCAN_RANGES)
					.where(locationCondition.and(FRIENDLY_SCAN_RANGES.OWNER.notEqual(FRIENDLY_SCAN_RANGES.TARGET_ID))));


				var neutralShipSelect = db.select(NON_FRIENDLY_SHIP_LOCATIONS.MAX_SIZE)
					.from(NON_FRIENDLY_SHIP_LOCATIONS)
					.where(locationCondition.and(NON_FRIENDLY_SHIP_LOCATIONS.STATUS.eq(User.Relation.ENEMY.ordinal())).and(NON_FRIENDLY_SHIP_LOCATIONS.TARGET_ID.eq(adminUser.getId())));

				var enemyShipSelect = db.select(NON_FRIENDLY_SHIP_LOCATIONS.MAX_SIZE)
					.from(NON_FRIENDLY_SHIP_LOCATIONS)
					.where(locationCondition.and(NON_FRIENDLY_SHIP_LOCATIONS.STATUS.eq(User.Relation.ENEMY.ordinal())).and(NON_FRIENDLY_SHIP_LOCATIONS.TARGET_ID.eq(adminUser.getId())));

				//TODO: Honor ShipTypeFlag.SEHR_KLEIN again

				try(neutralShipSelect) {
					var possibleSize = neutralShipSelect.limit(1).fetchAny(NON_FRIENDLY_SHIP_LOCATIONS.MAX_SIZE);
					maxNeutralShipSize = Objects.requireNonNullElse(possibleSize, -1);
				}

				try(enemyShipSelect) {
					var possibleSize = enemyShipSelect.fetchAny(NON_FRIENDLY_SHIP_LOCATIONS.MAX_SIZE);
					maxEnemyShipSize = Objects.requireNonNullElse(possibleSize, -1);
				}
			} catch (SQLException ex) {
				throw new RuntimeException(ex);
			}

			if(scanningShipInSector)
			{
				imageName += "_fo";
			}

			if(alliedShips)
			{
				imageName += "_fa";
			}

			int minSize = 0;
			if(maxEnemyShipSize > minSize)
			{
				imageName += "_fe";
			} else if(maxNeutralShipSize > minSize) {
				// We only show neutral ships if there are no enemies
				// enemy ships are more important, and we don't want to clutter the UI
				imageName += "_fn";
			}
		}

		if( imageName.isEmpty() )
		{
			return null;
		}

		return imageName;
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
		return bases != null && !bases.isEmpty() || this.getShipImage(position) != null || map.getRockPositions().contains(position);
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		return this.map.getBattlePositions().contains(sektor);
	}
}
