package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.User.Relation;
import net.driftingsouls.ds2.server.entities.User.Relations;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetEnemyShipsInSystem;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.Ship;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.driftingsouls.ds2.server.entities.jooq.tables.FriendlyScanRanges.FRIENDLY_SCAN_RANGES;

/**
 * Eine Sicht auf eine bestimmte Sternenkarte.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Schiffe, Rechte, etc.
 *
 * @author Sebastian Gift
 */
public class PlayerStarmap extends PublicStarmap
{
	private final Map<Location, Integer> scannedLocationsToScannerId;
	private final Set<Location> scannedNebulaLocations;
	private final Set<Location> sektorenMitBefreundetenSchiffen;
	private final Set<Location> sektorenMitRotemAlarm;
	private final Set<Location> bekannteOrte;
	private final User user;
	private final Relations relations;


	/**
	 * Legt eine neue Sicht an.
	 *
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param system Die ID des zugrunde liegenden Sternensystems.
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

		buildFriendlyData();

		this.relations = user.getRelations();

		this.scannedLocationsToScannerId = new HashMap<>();
		this.scannedNebulaLocations = new HashSet<>();
		this.sektorenMitBefreundetenSchiffen = new HashSet<>();
		buildScannedLocations();
		buildNonFriendSectors();

		this.bekannteOrte = findeBekannteOrte(user);
		this.sektorenMitRotemAlarm = findeSektorenMitRotemAlarm(user);
	}


	@Override
	protected void buildFriendlyData()
	{
		var scanMap = new HashMap<Location, ScanData>();
		var nebelScanMap = new HashMap<Location, ScanData>();

		var ownShipSectors = new HashSet<Location>();
		var allyShipSectors = new HashSet<Location>();
		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			try(var scanDataSelect = db
				.selectFrom(FRIENDLY_SCAN_RANGES)
				.where(FRIENDLY_SCAN_RANGES.TARGET_ID.eq(user.getId())
					.and(FRIENDLY_SCAN_RANGES.STAR_SYSTEM.eq(map.getSystem())))) {
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

					if(scanData.getOwnerId() == user.getId()) {
						ownShipSectors.add(scanData.getLocation());
					} else {
						allyShipSectors.add(scanData.getLocation());
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			try(var scanDataSelect = db
					.selectFrom(FRIENDLY_NEBEL_SCAN_RANGES)
					.where(FRIENDLY_NEBEL_SCAN_RANGES.TARGET_ID.eq(user.getId())
							.and(FRIENDLY_NEBEL_SCAN_RANGES.STAR_SYSTEM.eq(map.getSystem())))) {
				var result = scanDataSelect.fetch();
				for (var record : result) {
					var scanData = new ScanData(this.map.getSystem(), record.getX(), record.getY(), record.getId(), record.getOwner(), record.getSensorRange().intValue());

					//FRIENDLY_SCAN_RANGES contains values per sector for best scanner by user and best scanner by ally
					//So we check here which one really has the best scan range
					nebelScanMap.compute(scanData.getLocation(), (k, v) -> {
						if(v == null) {
							return scanData;
						}

						if(scanData.getScanRange() > v.getScanRange()) {
							return scanData;
						} else {
							return v;
						}
					});
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		this.nebelScanMap = nebelScanMap;
		this.scanMap = scanMap;
		this.ownShipSectors = ownShipSectors;
		this.allyShipSectors = allyShipSectors;
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

		return Ship.getAlertStatus(user, zuPruefendeSektoren.toArray(new Location[0]));
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
	public boolean isScanned(Location location)
	{
		return this.scannedLocationsToScannerId.containsKey(location) || this.bekannteOrte.contains(location);
	}

    @Override
    public int getScanningShip(Location location)
    {
        return this.scannedLocationsToScannerId.getOrDefault(location, -1);
    }

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
	{
		boolean scanned = scannedLocationsToScannerId.containsKey(location) || scannedNebulaLocations.contains(location);
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			for (Base base : positionBases)
			{
				if( scanned || base.getOwner().getId() == this.user.getId() ||
						(user.getAlly() != null && user.getAlly().getShowAstis() && user.getAlly().equals(base.getOwner().getAlly())) ||
						relations.isOnly(base.getOwner(), Relation.FRIEND) )
				{
					boolean isNebula = map.isNebula(location);
					boolean revealAsteroid = bekannteOrte.contains(location) || (!isNebula && scannedLocationsToScannerId.containsKey(location))|| (isNebula && (scannedNebulaLocations.contains(location) || shipInSector(location))) ;
					String img = base.getOverlayImage(location, user, revealAsteroid);
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
				if(!node.isHidden() || scannedLocationsToScannerId.containsKey(location) || scannedNebulaLocations.contains(location))
				{
					return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
				}
			}
		}

		if(!map.isNebula(location) || scannedNebulaLocations.contains(location))
		{
			if(map.getRockPositions().contains(location))
			{
				return new SectorImage("data/starmap/base/brocken.png", 0, 0);
			}
		}

		return null;
	}

	@Override
	public SectorImage getSectorOverlayImage(Location location)
	{
		if( !this.scannedLocationsToScannerId.containsKey(location) )
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

	private boolean shipInSector(Location location)
	{
		return scanMap.containsKey(location);
	}

	private String getShipImage(Location location)
	{
		String imageName = "";

		boolean baseInSector = map.getBaseMap().getOrDefault(location, List.of()).stream()
			.filter(base -> base.getOwner() != null)
			.anyMatch(base -> base.getOwner().getId() == user.getId());

		int maxEnemyShipSize = -1;
		int maxNeutralShipSize = -1;

		if(baseInSector || isScanned(location) || scannedNebulaLocations.contains((location)))
		{
			boolean scanningShipInSector = scanMap.containsKey(location);
			Nebel nebula = this.map.getNebulaMap().get(location);
			if(!baseInSector && !scanningShipInSector && nebula != null && !nebula.allowsScan()) {
				maxNeutralShipSize = -1;
				maxEnemyShipSize = -1;
			}
			else {
				var neutralShipSelect = neutralShipMap.containsKey(location);
				var enemyShipSelect = enemyShipMap.containsKey(location);

				//TODO: Honor ShipTypeFlag.SEHR_KLEIN again

				if(neutralShipSelect)
				{
					maxNeutralShipSize = neutralShipMap.get(location).getSize();
				}
				if(enemyShipSelect)
				{
					maxEnemyShipSize = enemyShipMap.get(location).getSize();
				}
			}
		}

		if(ownShipSectors.contains(location))
		{
			imageName += "_fo";
		}

		if(allyShipSectors.contains(location))
		{
			imageName += "_fa";
		}

		Nebel nebula = this.map.getNebulaMap().get(location);
		int minSize;
		if(nebula != null) {
			minSize = nebula.getType().getMinScansize();
		} else {
			minSize = 0;
		}

		if(maxEnemyShipSize > minSize)
		{
			imageName += "_fe";
		} else if(maxNeutralShipSize > minSize) {
			// We only show neutral ships if there are no enemies
			// enemy ships are more important, and we don't want to clutter the UI
			imageName += "_fn";
		}


		if( imageName.isEmpty() )
		{
			return null;
		}

		return imageName;
	}

	private void buildScannedLocations()
	{
		Map<Location, Nebel> nebulas = this.map.getNebulaMap();

		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			for (Map.Entry<Location, ScanData> entry : scanMap.entrySet()) {
				Location position = entry.getKey();
				ScanData scanData = entry.getValue();

				this.sektorenMitBefreundetenSchiffen.add(position);

				int scanRange = scanData.getScanRange();
				if (scanRange > 0) {
					//Find sectors scanned from ship
					for (int y = position.getY() - scanRange; y <= position.getY() + scanRange; y++) {
						for (int x = position.getX() - scanRange; x <= position.getX() + scanRange; x++) {
							Location loc = new Location(map.getSystem(), x, y);

							if (!position.sameSector(scanRange, loc, 0)) {
								continue;
							}

							var nebula = nebulas.get(loc);
							if (nebula == null || nebula.allowsScan()) {
								scannedLocationsToScannerId.put(loc, scanData.getShipId());
							}
						}
					}

					/* TODO: Handle Nebula scan
					//There was at least one friendly ship with sensors in the sector, so we need to find out if
					//there's a nebula scanner here
					int nebulaScanRange;
					try(var nebulaScanSelect = db.select(DSL.max(DSL.if_(DSL.coalesce(SHIPS_MODULES.SENSORRANGE, 0).greaterThan(SHIP_TYPES.SENSORRANGE), SHIPS_MODULES.SENSORRANGE, SHIP_TYPES.SENSORRANGE))).from(SHIPS)
						.innerJoin(SHIP_TYPES)
							.on(SHIPS.TYPE.eq(SHIP_TYPES.ID)
								.and(DSL.position(ShipTypeFlag.NEBELSCAN.getFlag(), SHIP_TYPES.FLAGS).greaterThan(0)))
						.leftJoin(SHIPS_MODULES)
							.on(SHIPS.MODULES.eq(SHIPS_MODULES.ID)
								.and(SHIPS_MODULES.FLAGS.contains(ShipTypeFlag.NEBELSCAN.getFlag())))) {
						nebulaScanRange = Objects.requireNonNullElse(nebulaScanSelect.fetchOne(0, int.class), 0);
					}

					if(nebulaScanRange > 0) {
						for (int y = position.getY() - nebulaScanRange; y <= position.getY() + nebulaScanRange; y++) {
							for (int x = position.getX() - nebulaScanRange; x <= position.getX() + nebulaScanRange; x++) {
								Location loc = new Location(map.getSystem(), x, y);

								if (!position.sameSector(scanRange, loc, 0)) {
									continue;
								}

								var nebula = nebulas.get(loc);
								if (nebula == null) {
									continue;
								}

								if (nebula.allowsScan()) {
									scannedNebulaLocations.add(loc);
								}
							}
						}
					}

					 */
				}
			}

			for (Map.Entry<Location, ScanData> entry : nebelScanMap.entrySet()) {
				Location position = entry.getKey();
				ScanData scanData = entry.getValue();

				int scanRange = scanData.getScanRange();
				for (int y = position.getY() - scanData.getScanRange(); y <= position.getY() + scanData.getScanRange(); y++) {
					for (int x = position.getX() - scanData.getScanRange(); x <= position.getX() + scanData.getScanRange(); x++) {
						Location loc = new Location(map.getSystem(), x, y);

						if (!position.sameSector(scanRange, loc, 0)) {
							continue;
						}

						var nebula = nebulas.get(loc);
						if (nebula == null) {
							continue;
						}

						if (nebula.allowsScan()) {
							scannedNebulaLocations.add(loc);
						}
					}
				}
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildNonFriendSectors()
	{
		var newEnemyShipMap = new HashMap<Location, NonFriendScanData>();
		var newNeutralShipMap = new HashMap<Location, NonFriendScanData>();

		var routine = new GetEnemyShipsInSystem();
		routine.setUserid(user.getId());
		routine.setInStarSystem(map.getSystem());

		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			routine.execute(db.configuration());
			try{
				var result = routine.getResults();
				for(var row : result)
				{
					for (int i=0;i<row.size();i++) {
						var scanData = new NonFriendScanData(this.map.getSystem(),
								(int)row.getValue(i, "x"),
								(int)row.getValue(i, "y"),
								(int)(row.getValue(i, "nebeltype")!=null ? row.getValue(i, "nebeltype") : 0),
								(int) (long)row.getValue(i, "max_size"),
								(int)(row.getValue(i, "status") != null ? row.getValue(i, "status") : 0) ==1);
						if (scanData.getIsEnemy())
						{
							newEnemyShipMap.put(scanData.getLocation(), scanData);
						}
						else {
							newNeutralShipMap.put(scanData.getLocation(), scanData);
						}
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		catch(SQLException e)
		{
			throw new RuntimeException(e);
		}

		neutralShipMap = newNeutralShipMap;
		enemyShipMap = newEnemyShipMap;
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
		return nodes != null && !nodes.isEmpty() || this.getShipImage(position) != null || map.getRockPositions().contains(position);
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		if( !this.scannedLocationsToScannerId.containsKey(sektor) )
		{
			return false;
		}

		return map.getBattlePositions().contains(sektor);
	}

	@Override
	public boolean isRoterAlarmImSektor(Location sektor)
	{
		return this.sektorenMitRotemAlarm.contains(sektor);
	}

	public Set<Location> getSektorenMitRotemAlarm()
	{
		return this.sektorenMitRotemAlarm;
	}
}
