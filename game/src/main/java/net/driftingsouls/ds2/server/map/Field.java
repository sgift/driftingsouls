package net.driftingsouls.ds2.server.map;

import java.util.Collections;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;

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
	private final List<Ship> ships;
	private final List<Base> bases;
	private final List<JumpNode> nodes;
	private final List<Battle> battles;
	private final List<Jump> subraumspalten;
	private final Nebel nebula;
	private final Location position;
	private final List<Ship> brocken;

	Field(Session db, Location position)
	{
		ships = Common.cast(db.createQuery("from Ship where star_system=:system and x=:x and y=:y and shiptype.shipClass !=:shipClasses)")
							  .setParameter("system", position.getSystem())
							  .setParameter("x", position.getX())
								.setParameter("y", position.getY())
								.setParameter("shipClasses", ShipClasses.FELSBROCKEN)
								.list());
		brocken = Common.cast(db.createQuery("from Ship where star_system=:system and x=:x and y=:y and shiptype.shipClass =:shipClasses)")
							  .setParameter("system", position.getSystem())
							  .setParameter("x", position.getX())
								.setParameter("y", position.getY())
								.setParameter("shipClasses", ShipClasses.FELSBROCKEN)
							  .list());
		bases = Common.cast(db.createQuery("from Base where star_system=:system and x=:x and y=:y")
				  .setParameter("system", position.getSystem())
				  .setParameter("x", position.getX())
				  .setParameter("y", position.getY())
				  .list());
		nodes = Common.cast(db.createQuery("from JumpNode where star_system=:system and x=:x and y=:y")
				.setParameter("system", position.getSystem())
				.setParameter("x", position.getX())
				.setParameter("y", position.getY())
				.list());
		nebula = (Nebel)db.get(Nebel.class, new MutableLocation(position));

		battles = Common.cast(db.createQuery("from Battle where star_system=:system and x=:x and y=:y")
				.setParameter("system", position.getSystem())
				.setParameter("x", position.getX())
				.setParameter("y", position.getY())
				.list());

		subraumspalten = Common.cast(db.createQuery("from Jump where star_system=:system and x=:x and y=:y")
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

	Nebel getNebula()
	{
		return this.nebula;
	}

	List<Ship> getShips()
	{
		return Collections.unmodifiableList(ships);
	}

	List<Ship> getBrocken()
	{
		return Collections.unmodifiableList(brocken);
	}

	List<Base> getBases()
	{
		return Collections.unmodifiableList(bases);
	}

	List<JumpNode> getNodes()
	{
		return Collections.unmodifiableList(nodes);
	}

	List<Battle> getBattles()
	{
		return Collections.unmodifiableList(battles);
	}

	List<Jump> getSubraumspalten()
	{
		return Collections.unmodifiableList(subraumspalten);
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

		Nebel.Typ type = nebula.getType();
		return type.getMinScansize() <= ship.getTypeData().getSize();
	}
}
