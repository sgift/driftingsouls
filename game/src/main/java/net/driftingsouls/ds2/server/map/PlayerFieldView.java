package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.db.DBUtil;
import net.driftingsouls.ds2.server.repositories.BasesRepository;
import net.driftingsouls.ds2.server.repositories.ShipsRepository;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import org.jooq.Records;
import org.jooq.impl.DSL;

import javax.persistence.EntityManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.driftingsouls.ds2.server.entities.jooq.tables.Ally.ALLY;
import static net.driftingsouls.ds2.server.entities.jooq.tables.BaseTypes.BASE_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Bases.BASES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Battles.BATTLES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumpnodes.JUMPNODES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Jumps.JUMPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipFleets.SHIP_FLEETS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.ShipTypes.SHIP_TYPES;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Ships.SHIPS;
import static net.driftingsouls.ds2.server.entities.jooq.tables.Users.USERS;

/**
 * Eine Sicht auf ein bestimmtes Sternenkartenfeld.
 * Die Sicht geht davon aus, dass der Spieler das Feld sehen darf.
 * Es findet aus Performancegruenden keine(!) Abfrage ab, um das sicherzustellen.
 *
 * @author Drifting-Souls Team
 */
public class PlayerFieldView implements FieldView
{
	protected final User user;
	protected final Location location;
	protected final PublicStarmap starmap;
	protected final EntityManager em;

    /**
	 * Legt eine neue Sicht an.
	 *
	 * @param user Der Spieler fuer den die Sicht gelten soll.
	 * @param position Der gesuchte Sektor.
     * @param starmap Die Sternenkarte des Systems.
	 */
	public PlayerFieldView(User user, Location position, PublicStarmap starmap, EntityManager em)
	{
		this.user = user;
        this.location = position;
		this.starmap = starmap;
		this.em = em;
	}

	/**
	 * Gibt die Liste aller Basen in dem Feld zurueck.
	 * @return Die Basenliste
	 */
	@Override
	public List<StationaryObjectData> getBases()
	{
		return BasesRepository.getBasesHeaderInfo(location, user.getId(), isNotScanned());
	}
	private boolean isNotScanned()
	{
		return !starmap.isScanned(location);
	}

	/**
	 * @return Die Schiffe, die der Spieler sehen kann.
	 */
	@Override
	public Map<UserData, Map<ShipTypeData, List<ShipData>>> getShips()
	{
        if(isNotScanned())
        {
            return new TreeMap<>();
        }

		int minSize = getMinSize();

		if(!starmap.isScanned(location) || (getNebel() != null && getNebel().isEmp())) minSize = Integer.MAX_VALUE;

		return ShipsRepository.getShipsInMapSector(this.location, user.getId(), minSize);
	}

	protected int getMinSize()
	{
		int minSize;
		if(!starmap.ownShipSectors.contains(location) && !starmap.allyShipSectors.contains(location) && getNebel() != null) {
			minSize = getNebel().getMinScansize();
		} else {
			minSize = 0;
		}

		if(!starmap.isScanned(location) || (getNebel() != null && getNebel().isEmp())) minSize = Integer.MAX_VALUE;

		return minSize;
	}

	@Override
	public List<NodeData> getJumpNodes()
	{
		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var condition = JUMPNODES.STAR_SYSTEM.eq(location.getSystem())
				.and(JUMPNODES.X.eq(location.getX()))
				.and(JUMPNODES.Y.eq(location.getY()));
			if(isNotScanned()) {
				condition = condition.and(JUMPNODES.HIDDEN.eq(0));
			}

			var select = db.select(JUMPNODES.ID, JUMPNODES.NAME, JUMPNODES.GCPCOLONISTBLOCK)
				.from(JUMPNODES)
				.where(condition);

			return select.fetch().map(Records.mapping(NodeData::new));

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getJumpCount()
	{
		if(isNotScanned())
		{
			return 0;
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);


			@SuppressWarnings("ConstantConditions")
			int count = db.select(DSL.count()).from(JUMPS).where(
					JUMPS.STAR_SYSTEM.eq(location.getSystem())
					.and(JUMPS.X.eq(location.getX()))
					.and(JUMPS.Y.eq(location.getY()))
				)
				.fetchOne(DSL.count());
			return count;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<BattleData> getBattles()
	{
		if(isNotScanned())
		{
			return new ArrayList<>();
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var attacker = USERS.as("attacker");
			var defender = USERS.as("defender");
			var attackerAlly = ALLY.as("attacker_ally");
			var defenderAlly = ALLY.as("defender_ally");

			var select = db.select(BATTLES.ID,
					attacker.RACE, attacker.ID, attacker.NICKNAME, attacker.PLAINNAME, attackerAlly.ID, attackerAlly.NAME, attackerAlly.PLAINNAME,
					defender.RACE, defender.ID, defender.NICKNAME, defender.PLAINNAME, defenderAlly.ID, defenderAlly.NAME, defenderAlly.PLAINNAME)
				.from(BATTLES)
				.join(attacker)
				.on(BATTLES.COMMANDER1.eq(attacker.ID))
				.join(defender)
				.on(BATTLES.COMMANDER2.eq(defender.ID))
				.leftJoin(attackerAlly)
				.on(attacker.ALLY.eq(attackerAlly.ID))
				.leftJoin(defenderAlly)
				.on(defender.ALLY.eq(defenderAlly.ID))
				.where(BATTLES.STAR_SYSTEM.eq(location.getSystem())
					.and(BATTLES.X.eq(location.getX()))
					.and(BATTLES.Y.eq(location.getY())));

			return select.fetch(Records.mapping(BattleData::new));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<StationaryObjectData> getBrocken()
	{
		if( isNotScanned() || !starmap.hasRocks(location) )
		{
			return new ArrayList<>();
		}

		try(var conn = DBUtil.getConnection(em)) {
			var db = DBUtil.getDSLContext(conn);

			var rockSelect = db.select(SHIPS.ID, SHIPS.NAME, SHIPS.OWNER, USERS.NICKNAME, SHIP_TYPES.PICTURE, SHIPS.TYPE, SHIP_TYPES.NICKNAME)
				.from(SHIPS)
				.join(SHIP_TYPES)
				.on(SHIP_TYPES.CLASS.eq(ShipClasses.FELSBROCKEN.ordinal()).and(SHIP_TYPES.ID.eq(SHIPS.TYPE)))
				.join(USERS)
				.on(SHIPS.OWNER.eq(USERS.ID))
				.where(
					SHIPS.STAR_SYSTEM.eq(location.getSystem())
					.and(SHIPS.X.eq(location.getX()))
					.and(SHIPS.Y.eq(location.getY())));

			return rockSelect.fetch(Records.mapping(StationaryObjectData::new));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isRoterAlarm()
	{
		if(isNotScanned()) return false;
		return starmap.isRoterAlarmImSektor(location);
	}

	@Override
	public Nebel.Typ getNebel()
	{
		return starmap.getNebula(location);
	}

	public Location getLocation()
	{
		return location;
	}
}
