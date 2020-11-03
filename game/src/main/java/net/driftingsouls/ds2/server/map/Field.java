package net.driftingsouls.ds2.server.map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

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
	private final List<Ship> brocken;

	Field(EntityManager em, Location position) {
		ships = em.createQuery("from Ship where system=:system and x=:x and y=:y and shiptype.shipClass != :shipClasses", Ship.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.setParameter("shipClasses", ShipClasses.FELSBROCKEN)
			.getResultList();
		brocken = em.createQuery("from Ship where system=:system and x=:x and y=:y and shiptype.shipClass = :shipClasses", Ship.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.setParameter("shipClasses", ShipClasses.FELSBROCKEN)
			.getResultList();
		bases = em.createQuery("from Base where system=:system and x=:x and y=:y", Base.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.getResultList();
		nodes = em.createQuery("from JumpNode where system=:system and x=:x and y=:y", JumpNode.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.getResultList();
		nebula = em.find(Nebel.class, new MutableLocation(position));

		battles = em.createQuery("from Battle where system=:system and x=:x and y=:y", Battle.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.getResultList();

		subraumspalten = em.createQuery("from Jump where system=:system and x=:x and y=:y", Jump.class)
			.setParameter("system", position.getSystem())
			.setParameter("x", position.getX())
			.setParameter("y", position.getY())
			.getResultList();
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
}
