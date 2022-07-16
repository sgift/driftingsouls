package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipsModules.SHIPS_MODULES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.UserRelations.USER_RELATIONS;

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
	}

	@Override
	public boolean isScanned(Location location)
	{
		return true;
	}

	@Override
	public Ship getScanningShip(Location location)
	{
		return null; //TODO
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

		List<Ship> positionBrocken = map.getBrockenMap().get(location);
		if(positionBrocken != null && !positionBrocken.isEmpty())
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

	private String getShipImage(Location location)
	{
		String imageName = "";
		int ownShips;
		int enemyShips;

		try(var conn = DBUtil.getConnection()) {
			var db = DBUtil.getDSLContext(conn);
			var locationCondition = SHIPS.STAR_SYSTEM.eq(location.getSystem())
				.and(SHIPS.X.eq(location.getX()))
				.and(SHIPS.Y.eq(location.getY()));

			ownShips = Objects.requireNonNullElse(db.selectCount().from(SHIPS)
				.where(locationCondition.and(SHIPS.OWNER.eq(adminUser.getId())))
				.fetchOne(0, int.class), 0);

			Nebel nebula = this.map.getNebulaMap().get(location);
			if(nebula != null && !nebula.allowsScan() && ownShips == 0) {
				enemyShips = 0;
			} else {
				var relationBasedSelect = db.selectCount().from(USER_RELATIONS)
					.innerJoin(SHIPS)
					.on(SHIPS.OWNER.eq(USER_RELATIONS.ID)
						.and(SHIPS.OWNER.notEqual(adminUser.getId()))
						.and(locationCondition));

				var enemyShipSelect = relationBasedSelect
					.where(locationCondition.and(USER_RELATIONS.STATUS.eq(User.Relation.ENEMY.ordinal())));

				if(ownShips == 0) {
					enemyShipSelect = enemyShipSelect.and(SHIPS_MODULES.FLAGS.notContains(ShipTypeFlag.SEHR_KLEIN.getFlag()));
				}

				enemyShips = Objects.requireNonNullElse(enemyShipSelect.fetchOne(0, int.class), 0);
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}

		if(ownShips > 0)
		{
			imageName += "_fo";
		}

		if(enemyShips > 0)
		{
			imageName += "_fe";
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
		List<Ship> brocken = map.getBrockenMap().get(position);
		return bases != null && !bases.isEmpty() || this.getShipImage(position) != null || brocken != null && !brocken.isEmpty();
	}

	@Override
	public boolean isSchlachtImSektor(Location sektor)
	{
		List<Battle> battles = this.map.getBattleMap().get(sektor);
		return battles != null && !battles.isEmpty();
	}
}
