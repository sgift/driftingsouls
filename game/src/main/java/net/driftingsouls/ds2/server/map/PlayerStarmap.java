package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.jooq.routines.GetEnemyShipsInSystem;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.repositories.ShipsRepository;
import net.driftingsouls.ds2.server.services.SingleUserRelationsService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.UserRelations.USER_RELATIONS;

/**
 * Eine Sicht auf eine bestimmte Sternenkarte.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Schiffe, Rechte, etc.
 *
 * @author Sebastian Gift
 */
public class PlayerStarmap extends PublicStarmap
{
	private final Map<Location, Integer> scannedLocationsToScannerId;
	private final Map<Location, Integer> scannedNebulaLocationsToScannerId;
	private final Set<Location> sectorsWithAttackingShips;
	private final Set<Location> bekannteOrte;
	private final User user;
	protected SingleUserRelationsService userRelationsService;


	/**
	 * Legt eine neue Sicht an.
	 *
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param systemId Die ID des zugrunde liegenden Sternensystems.
	 */
	public PlayerStarmap(User user, int systemId)
	{
		super(systemId);

		this.user = user;
		if(this.user == null)
		{
			throw new IllegalArgumentException("User may not be null.");
		}
		this.userRelationsService = new SingleUserRelationsService(user.getId());
		buildFriendlyData();

		this.scannedLocationsToScannerId = new HashMap<>();
		this.scannedNebulaLocationsToScannerId = new HashMap<>();
		buildScannedLocations();
		buildNonFriendSectors();

		this.bekannteOrte = findWellKnownLocations();
		this.sectorsWithAttackingShips = findVisibleSectorsWithAlerts();

	}


	@Override
	protected void buildFriendlyData()
	{
		var scanShips = ShipsRepository.getScanships(user.getId(), map.getSystem());
		var nebulaScanShips = ShipsRepository.getNebulaScanships(user.getId(), map.getSystem());

		for(var scanship : scanShips)
		{
			addScanShipToMap(scanship, scanMap,false);
		}
		for(var scanShip : nebulaScanShips)
		{
			addScanShipToMap(scanShip, scanMap, true);
			addScanShipToMap(scanShip, nebulaScanMap, true);

		}
	}

	private void addScanShipToMap(ScanData scanShip, HashMap<Location, ScanData> targetMap, boolean isNebulaScanner)
	{
		var scannerLocation = scanShip.getLocation();

		if (getNebula(scannerLocation) != null && !isNebulaScanner)
		{
			scanShip = new ScanData(scanShip.getLocation().getSystem(), scanShip.getLocation().getX(), scanShip.getLocation().getY(), scanShip.getShipId(), scanShip.getOwnerId(), (int)(scanShip.getScanRange() * 0.5));
		}

		if(!targetMap.containsKey(scannerLocation) || targetMap.get(scannerLocation).getScanRange() < scanShip.getScanRange())
		{
			targetMap.put(scannerLocation, scanShip);
		}

		if(scanShip.getOwnerId() == user.getId()) {
			ownShipSectors.add(scanShip.getLocation());
		} else {
			allyShipSectors.add(scanShip.getLocation());
		}
	}

	private Set<Location> findVisibleSectorsWithAlerts()
	{
		Set<Location> candidateSectors = new HashSet<>();
		for (Location sektor : this.scanMap.keySet())
		{
			for( int x=-1; x <= 1; x++ )
			{
				for( int y=-1; y <= 1; y++ )
				{
					Location scanSektor = new Location(sektor.getSystem(), sektor.getX()+x, sektor.getY()+y);
					candidateSectors.add(scanSektor);
				}
			}
		}

		if(candidateSectors.isEmpty()) {
			return Set.of();
		}

		var attackingSectors = ShipsRepository.getAttackingSectors(user.getId());

		// Only show alerts where we have a ship in the area
		retainSectors(candidateSectors, attackingSectors);

		return attackingSectors;
	}

	protected void retainSectors(Set<Location> candidateSectors, HashSet<Location> attackingSectors)
	{
		attackingSectors.retainAll(candidateSectors);
	}


	private Set<Location> findWellKnownLocations()
	{
		Set<Location> wellKnownLocations = new HashSet<>();
		for (Starmap.JumpNode jumpNode : this.map.getNodes())
		{
			if( jumpNode.isHidden() )
			{
				continue;
			}
			wellKnownLocations.add(new Location(map.getSystem(), jumpNode.getX(), jumpNode.getY()));
		}

		for (Map.Entry<Location, List<BaseData>> loc : this.map.getBaseMap().entrySet())
		{
			for (BaseData base : loc.getValue())
			{
				//User owner = base.getOwner();
				if( base.getOwnerId() == user.getId() )
				{
					wellKnownLocations.add(loc.getKey());
				}
				else if( user.getAlly() != null && user.getAlly().getShowAstis() && user.getAlly().getId() == base.getOwnerAllyId() )
				{
					wellKnownLocations.add(loc.getKey());
				}
				else
				{
					if( this.userRelationsService.isMutualFriendTo(base.getOwnerId()))
					{
						wellKnownLocations.add(loc.getKey());
					}
				}
			}
		}

		return wellKnownLocations;
	}

	@Override
	public boolean isScanned(Location location)
	{
		return this.scannedLocationsToScannerId.containsKey(location) || this.scannedNebulaLocationsToScannerId.containsKey(location) || this.bekannteOrte.contains(location);
	}

    @Override
    public int getScanningShip(Location location)
    {
        var scanShipId = this.scannedLocationsToScannerId.getOrDefault(location, -1);
		if(scanShipId == -1) {
			scanShipId = this.scannedNebulaLocationsToScannerId.getOrDefault(location, -1);
		}

		return scanShipId;
    }

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
	{
		boolean scanned = scannedLocationsToScannerId.containsKey(location) || scannedNebulaLocationsToScannerId.containsKey(location);
		List<BaseData> positionBases = map.getBaseMap().get(location);
		if(positionBases != null)
		{
			for (BaseData base : positionBases)
			{
				boolean areMutualFriends = this.userRelationsService.isMutualFriendTo(base.getOwnerId());
				boolean isBaseOwnerEnemy = this.userRelationsService.beziehungVon(base.getOwnerId()) == User.Relation.ENEMY ||
					this.userRelationsService.beziehungZu(base.getOwnerId()) == User.Relation.ENEMY;

				if( scanned || base.getOwnerId() == this.user.getId() ||
						(user.getAlly() != null && user.getAlly().getShowAstis() && user.getAlly().getId() == base.getOwnerAllyId()) ||
						areMutualFriends )
				{
					boolean isNebula = map.isNebula(location);
					boolean revealAsteroid = bekannteOrte.contains(location) || (!isNebula && scannedLocationsToScannerId.containsKey(location)) || shipInSector(location) ;
					String img = base.getOverlayImage(location, user, revealAsteroid, areMutualFriends, isBaseOwnerEnemy);
					if( img != null ) {
						return new SectorImage(img, 0, 0);
					}
				}
			}
		}

		List<Starmap.JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			for(Starmap.JumpNode node: positionNodes)
			{
				if(!node.isHidden() || scannedLocationsToScannerId.containsKey(location) || scannedNebulaLocationsToScannerId.containsKey(location))
				{
					return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
				}
			}
		}

		if(!map.isNebula(location) || scannedNebulaLocationsToScannerId.containsKey(location))
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
		if( !this.scannedLocationsToScannerId.containsKey(location) && !this.scannedNebulaLocationsToScannerId.containsKey(location))
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

	protected String getShipImage(Location location)
	{
		String imageName = "";

		boolean baseInSector = map.getBaseMap().getOrDefault(location, List.of()).stream()
			.filter(base -> base.getOwnerId() != -1)
			.anyMatch(base -> base.getOwnerId() == user.getId());

		int maxEnemyShipSize;
		int maxNeutralShipSize;

		if(baseInSector || isScanned(location))
		{
			boolean scanningShipInSector = scanMap.containsKey(location);
			Nebel.Typ nebula = this.map.getNebulaMap().get(location);
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
				} else {
					maxNeutralShipSize = -1;
				}


				if(enemyShipSelect)
				{
					maxEnemyShipSize = enemyShipMap.get(location).getSize();
				} else {
					maxEnemyShipSize = -1;
				}
			}
		} else {
			maxEnemyShipSize = -1;
			maxNeutralShipSize = -1;
		}

		if(ownShipSectors.contains(location))
		{
			imageName += "_fo";
		}

		if(allyShipSectors.contains(location))
		{
			imageName += "_fa";
		}

		Nebel.Typ nebula = this.map.getNebulaMap().get(location);
		int minSize;
		if(nebula != null) {
			minSize = nebula.getMinScansize();
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
		Map<Location, Nebel.Typ> nebulas = this.map.getNebulaMap();
		for (Map.Entry<Location, ScanData> entry : scanMap.entrySet()) {
			Location position = entry.getKey();
			ScanData scanData = entry.getValue();

			int scanRange = scanData.getScanRange();
			if (scanRange > 0) {
				//Find sectors scanned from ship
				findScannedPositions(nebulas, position, scanData, scanRange, scannedLocationsToScannerId);

				var nebulaScanData = nebulaScanMap.get(position);
				// Nebula scanners can only exist on position which have at least one friendly scanner
				if (nebulaScanData != null) {
					int nebulaScanRange = scanData.getScanRange();
					findScannedPositions(nebulas, position, scanData, nebulaScanRange, scannedNebulaLocationsToScannerId);
				}
			}
		}
	}

	private void findScannedPositions(Map<Location, Nebel.Typ> nebulae, Location position, ScanData scanData, int scanRange, Map<Location, Integer> scannedPositions) {
		for (int y = position.getY() - scanRange; y <= position.getY() + scanRange; y++) {
			for (int x = position.getX() - scanRange; x <= position.getX() + scanRange; x++) {
				Location loc = new Location(map.getSystem(), x, y);

				if (!position.sameSector(scanRange, loc, 0)) {
					continue;
				}

				var nebula = nebulae.get(loc);

				if(nebula != null && scannedPositions.containsKey(loc))
				{
					if(scanData.getLocation() == loc)
					{
						scannedPositions.put(loc, scanData.getShipId());
					}
				}
				else if (nebula == null || nebula.allowsScan()) {
					scannedPositions.put(loc, scanData.getShipId());
				}
			}
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
					for(var record: row) {
						var scanData = new NonFriendScanData(
							this.map.getSystem(),
							record.get(SHIPS.X),
							record.get(SHIPS.Y),
							record.get("max_size", Long.class).intValue(),
							Objects.requireNonNullElse(record.get(USER_RELATIONS.STATUS), 0) == 1
						);

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
		List<BaseData> bases = map.getBaseMap().get(position);
		if( bases != null && !bases.isEmpty()  )
		{
			return true;
		}

		List<Starmap.JumpNode> nodes = map.getNodeMap().get(position);
		return nodes != null && !nodes.isEmpty() || this.getShipImage(position) != null || map.getRockPositions().contains(position);
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		if( !this.scannedLocationsToScannerId.containsKey(sektor) && !this.scannedNebulaLocationsToScannerId.containsKey(sektor) )
		{
			return false;
		}

		return map.getBattlePositions().contains(sektor);
	}

	@Override
	public boolean isRoterAlarmImSektor(Location sektor)
	{
		return this.sectorsWithAttackingShips.contains(sektor);
	}

	public Set<Location> getSectorsWithAttackingShips()
	{
		return this.sectorsWithAttackingShips;
	}
}
