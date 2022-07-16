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
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipsModules.SHIPS_MODULES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.UserRelations.USER_RELATIONS;

/**
 * Eine Sicht auf eine bestimmte Sternenkarte.
 * Die Sicht ist fuer jeden Spieler anders und beruecksichtigt Schiffe, Rechte, etc.
 *
 * @author Sebastian Gift
 */
public class PlayerStarmap extends PublicStarmap
{
	private final Set<Location> scannedLocations;
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
		this.relations = user.getRelations();

		this.scannedLocations = new HashSet<>();
		this.scannedNebulaLocations = new HashSet<>();
		this.sektorenMitBefreundetenSchiffen = new HashSet<>();
		buildScannedLocations();

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
		return this.scannedLocations.contains(location) || this.bekannteOrte.contains(location);
	}

    @Override
    public Ship getScanningShip(Location location)
    {
        return null; //TODO
    }

	@Override
	public SectorImage getUserSectorBaseImage(Location location)
	{
		boolean scanned = scannedLocations.contains(location) || scannedNebulaLocations.contains(location);
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
					boolean revealAsteroid = bekannteOrte.contains(location) || (!isNebula && scannedLocations.contains(location))|| (isNebula && (scannedNebulaLocations.contains(location) || shipInSector(location))) ;
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
				if(!node.isHidden() || scannedLocations.contains(location) || scannedNebulaLocations.contains(location))
				{
					return new SectorImage("data/starmap/jumpnode/jumpnode.png", 0, 0);
				}
			}
		}

		if(!map.isNebula(location) || scannedNebulaLocations.contains(location))
		{
			List<Ship> positionBrocken = map.getBrockenMap().get(location);
			if(positionBrocken != null && !positionBrocken.isEmpty())
			{
				return new SectorImage("data/starmap/base/brocken.png", 0, 0);
			}
		}

		return null;
	}

	@Override
	public SectorImage getSectorOverlayImage(Location location)
	{
		if( !this.scannedLocations.contains(location) )
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
		return this.map.getScanMap().get(location) != null;
	}

	private String getShipImage(Location location)
	{
		String imageName = "";

		if(isScanned(location))
		{
			int ownShips;
			int alliedShips;
			int enemyShips;

			try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
				var db = DBUtil.getDSLContext(conn);
				var locationCondition = SHIPS.STAR_SYSTEM.eq(location.getSystem())
					.and(SHIPS.X.eq(location.getX()))
					.and(SHIPS.Y.eq(location.getY()));

				ownShips = Objects.requireNonNullElse(db.selectCount().from(SHIPS)
					.where(locationCondition.and(SHIPS.OWNER.eq(user.getId())))
					.fetchOne(0, int.class), 0);

				Nebel nebula = this.map.getNebulaMap().get(location);
				if(nebula != null && !nebula.allowsScan() && ownShips == 0) {
					alliedShips = 0;
					enemyShips = 0;
				} else {
					var relationBasedSelect = db.selectCount().from(USER_RELATIONS)
						.innerJoin(SHIPS)
						.on(SHIPS.OWNER.eq(USER_RELATIONS.ID)
							.and(SHIPS.OWNER.notEqual(user.getId()))
							.and(locationCondition));

					try(var alliedShipSelect = relationBasedSelect
						.where(locationCondition.and(USER_RELATIONS.STATUS.eq(Relation.FRIEND.ordinal())))) {
						alliedShips = Objects.requireNonNullElse(alliedShipSelect.fetchOne(0, int.class), 0);
					}


					var enemyShipSelect = relationBasedSelect
						.where(locationCondition.and(USER_RELATIONS.STATUS.eq(Relation.ENEMY.ordinal())));

					// No ships in sector? Then we need to filter ships with the tiny flag
					if(ownShips == 0) {
						enemyShipSelect = db.selectCount().from(USER_RELATIONS)
							.innerJoin(SHIPS)
							.on(SHIPS.TYPE.eq(SHIP_TYPES.ID)
								.and(DSL.position(ShipTypeFlag.SEHR_KLEIN.getFlag(), SHIP_TYPES.FLAGS).eq(0)))
							.leftJoin(SHIPS_MODULES)
							.on(SHIPS.MODULES.eq(SHIPS_MODULES.ID)
								.and(DSL.position(ShipTypeFlag.SEHR_KLEIN.getFlag(), SHIPS_MODULES.FLAGS).eq(0)))
							.where(locationCondition.and(USER_RELATIONS.STATUS.eq(Relation.ENEMY.ordinal())));
					}

					var finalWrapper = enemyShipSelect;
					try(finalWrapper) {
						enemyShips = Objects.requireNonNullElse(finalWrapper.fetchOne(0, int.class), 0);
					}
				}
			} catch (SQLException ex) {
				throw new RuntimeException(ex);
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

	private void buildScannedLocations()
	{
		Map<Location, Nebel> nebulas = this.map.getNebulaMap();

		try(var conn = DBUtil.getConnection(ContextMap.getContext().getEM())) {
			var db = DBUtil.getDSLContext(conn);
			for (Map.Entry<Location, ScanData> entry : this.map.getScanMap().entrySet()) {
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
								scannedLocations.add(loc);
							}
						}
					}

					//There was at least one friendly ship with sensors in the sector, so we need to find out if
					//there's a nebula scanner here
					int nebulaScanRange;
					try(var nebulaScanSelect = db.select(DSL.max(DSL.if_(DSL.coalesce(SHIPS_MODULES.SENSORRANGE, 0).greaterThan(SHIP_TYPES.SENSORRANGE), SHIPS_MODULES.SENSORRANGE, SHIP_TYPES.SENSORRANGE))).from(SHIPS)
						.innerJoin(SHIP_TYPES)
						.on(SHIPS.TYPE.eq(SHIP_TYPES.ID)
							.and(DSL.position(ShipTypeFlag.NEBELSCAN.getFlag(), SHIP_TYPES.FLAGS).greaterThan(0)))
						.leftJoin(SHIPS_MODULES)
						.on(SHIPS.MODULES.eq(SHIPS_MODULES.ID)
							.and(DSL.position(ShipTypeFlag.NEBELSCAN.getFlag(), SHIPS_MODULES.FLAGS).greaterThan(0)))) {
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
				} else {
					//Our ship may have zero scan range, but we can still see our sector
					this.scannedLocations.add(position);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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
		List<Ship> brocken = map.getBrockenMap().get(position);
		return nodes != null && !nodes.isEmpty() || this.getShipImage(position) != null || brocken != null && !brocken.isEmpty();
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		if( !this.scannedLocations.contains(sektor) )
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

	public Set<Location> getSektorenMitRotemAlarm()
	{
		return this.sektorenMitRotemAlarm;
	}
}
