package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.entities.Nebel;
import org.hibernate.Session;

import java.util.List;
import java.util.Map;

/**
 * Die Sicht eines Administrators auf ein Feld der Sternenkarte.
 */
public class AdminFieldView implements FieldView
{
	private final Field field;
	private final Session db;
	private final Location location;
	private final PublicStarmap starmap;

	/**
	 * Legt eine neue Sicht an.
	 *
	 * @param db Ein aktives Hibernate Sessionobjekt.
	 * @param position Der gesuchte Sektor.
	 */
	public AdminFieldView(Session db, Location position, PublicStarmap starmap)
	{
		this.field = new Field(db, position);
		this.db = db;
		this.location = position;
		this.starmap = starmap;
	}

	@Override
	public List<StationaryObjectData> getBases()
	{
		return List.of();
	}
	@Override
	public List<StationaryObjectData> getBrocken()
	{
		return List.of();
	}


	@Override
	public Map<UserData, Map<ShipTypeData, List<ShipData>>> getShips()
	{
		return Map.of();
	}

	@Override
	public List<NodeData> getJumpNodes()
	{
		return List.of();
	}

	@Override
	public int getJumpCount()
	{
		return 0;
	}

	@Override
	public Nebel.Typ getNebel()
	{
		return starmap.getNebula(location);
	}

	@Override
	public List<BattleData> getBattles()
	{
		return List.of();
	}

	@Override
	public boolean isRoterAlarm()
	{
		return false;
	}

	public Location getLocation()
	{
		return location;
	}
}
