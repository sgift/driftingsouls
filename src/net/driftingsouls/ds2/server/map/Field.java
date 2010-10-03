package net.driftingsouls.ds2.server.map;

import java.util.Collections;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;

import org.hibernate.Session;

/**
 * Ein Feld auf einer Sternenkarte.
 * Das Feld wird intern von diversen Sichten verwendet, um
 * auf die enthaltenen Objekte zuzugreifen.
 * 
 * @author Drifting-Souls Team
 */
class Field
{
	Field(Session db, Location position)
	{
		ships = Common.cast(db.createQuery("from Ship where system=:system and x=:x and y=:y")
							  .setParameter("system", position.getSystem())
							  .setParameter("x", position.getX())
							  .setParameter("y", position.getY())
							  .list());
		this.position = position;
	}
	
	boolean isNebula()
	{
		return nebula != null;
	}
	
	List<Ship> getShips()
	{
		return Collections.unmodifiableList(ships);
	}
	
	List<Base> getBases()
	{
		return Collections.unmodifiableList(bases);
	}
	
	List<JumpNode> getNodes()
	{
		return Collections.unmodifiableList(nodes);
	}
	
	Location getPosition()
	{
		return this.position;
	}
	
	boolean isScannableInLrs(Ship ship)
	{
		if(!isNebula())
		{
			return true;
		}
		else
		{
			Nebel.Types type = Nebel.Types.getType(nebula.getType());
			return type.getMinScansize() <= ship.getTypeData().getSize();
		}
	}
	
	private List<Ship> ships;
	private List<Base> bases;
	private List<JumpNode> nodes;
	private Nebel nebula;
	private Location position;
}
