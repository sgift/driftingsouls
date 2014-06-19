/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.modules.admin;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.MutableLocation;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.bases.Building;
import net.driftingsouls.ds2.server.battles.Battle;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.admin.editoren.EditorForm8;
import net.driftingsouls.ds2.server.modules.admin.editoren.EntityEditor;
import net.driftingsouls.ds2.server.ships.Ship;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Aktualisierungstool fuer die Systeme.
 *
 */
@AdminMenuEntry(category = "Systeme", name = "System", permission = WellKnownAdminPermission.EDIT_SYSTEM)
public class EditSystem implements EntityEditor<StarSystem>
{
	@Override
	public Class<StarSystem> getEntityType()
	{
		return StarSystem.class;
	}

	@Override
	public void configureFor(@Nonnull EditorForm8<StarSystem> form)
	{
		form.allowAdd();
		form.allowDelete();
		form.field("Name", String.class, StarSystem::getName, StarSystem::setName);
		form.field("Breite", Integer.class, StarSystem::getWidth, StarSystem::setWidth);
		form.field("Höhe", Integer.class, StarSystem::getHeight, StarSystem::setHeight);
		form.field("Allow Military", Boolean.class, StarSystem::isMilitaryAllowed, StarSystem::setMilitaryAllowed);
		form.field("Max Colonies (-1 = keine Begrenzung)", Integer.class, StarSystem::getMaxColonies, StarSystem::setMaxColonies);
		form.field("In Sternenkarte sichtbar", Boolean.class, StarSystem::isStarmapVisible, StarSystem::setStarmapVisible);
		form.field("OrderLocations(Form: x/y|x/y)", String.class, StarSystem::getOrderLocationString, StarSystem::setOrderLocations);
		form.field("GTU Dropzone(Form: x/y)", String.class, StarSystem::getDropZoneString, (s, v) -> s.setDropZone(!"".equals(v) ? Location.fromString(v) : new Location(0, 0, 0)));
		form.field("Zugriffsrechte", StarSystem.Access.class, StarSystem::getAccess, StarSystem::setAccess);
		form.field("Beschreibung", String.class, StarSystem::getDescription, StarSystem::setDescription);
		form.field("Ressourcenvorkommen", String.class, StarSystem::getSpawnableRess, StarSystem::setSpawnableRess);


		form.postUpdateTask("Schiffe an Systemgrenzen anpassen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Ship where system = :system and (x>:width or y>:height)")
						.setInteger("system", s.getID())
						.setInteger("width", s.getWidth())
						.setInteger("height", s.getHeight())
						.list()),
				this::aktualisiereSchiffe
		);
		form.postUpdateTask("Schlachten an Systemgrenzen anpassen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Battle where system = :system and (x>:width or y>:height)")
						.setInteger("system", s.getID())
						.setInteger("width", s.getWidth())
						.setInteger("height", s.getHeight())
						.list()),
				this::aktualisiereSchlachten
		);
		form.postUpdateTask("Basen an Systemgrenzen anpassen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Base where system = :system and (x>:width or y>:height)")
						.setInteger("system", s.getID())
						.setInteger("width", s.getWidth())
						.setInteger("height", s.getHeight())
						.list()),
				this::aktualisiereBasen
		);
		form.postUpdateTask("Ueberfluessige Nebel entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select loc from Nebel where loc.system = :system and (loc.x>:width or loc.y>:height)")
						.setInteger("system", s.getID())
						.setInteger("width", s.getWidth())
						.setInteger("height", s.getHeight())
						.list()),
				this::entferneNebel
		);



		form.preDeleteTask("Nebel entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select loc from Nebel where loc.system = :system")
						.setInteger("system", s.getID())
						.list()),
				this::entferneNebel
		);

		form.preDeleteTask("Schlachten beenden",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Battle where system = :system")
						.setInteger("system", s.getID())
						.list()),
				this::schlachtBeenden
		);

		form.preDeleteTask("Schiffe entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Ship where system = :system")
						.setInteger("system", s.getID())
						.list()),
				this::destroyShip
		);

		form.preDeleteTask("Jumps entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Jump where system = :system")
						.setInteger("system", s.getID())
						.list()),
				this::destroyJump
		);

		form.preDeleteTask("Sprungpunkte entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from JumpNode where system = :system or systemOut = :system")
						.setInteger("system", s.getID())
						.list()),
				this::destroyJumpNode
		);

		form.preDeleteTask("Basen entfernen",
				(s) -> Common.cast(ContextMap.getContext().getDB().createQuery("select id from Base where system = :system")
						.setInteger("system", s.getID())
						.list()),
				this::destroyBase
		);
	}

	private void destroyBase(StarSystem oldsystem, StarSystem system, Serializable baseid)
	{
		Session db = ContextMap.getContext().getDB();
		Base base = (Base)db.get(Base.class, baseid);

		Integer[] bebauung = base.getBebauung();
		for (Integer aBebauung : bebauung)
		{
			if (aBebauung == 0)
			{
				continue;
			}

			Building building = Building.getBuilding(aBebauung);
			building.cleanup(ContextMap.getContext(), base, aBebauung);
		}

		db.delete(base);
	}

	private void schlachtBeenden(StarSystem oldsystem, StarSystem system, Serializable battleid)
	{
		Session db = ContextMap.getContext().getDB();
		Battle battle = (Battle)db.get(Battle.class, battleid);

		battle.load(battle.getCommander(0), null, null, 0);
		battle.endBattle(0, 0);
	}

	private void destroyJump(StarSystem oldsystem, StarSystem system, Serializable jumpid)
	{
		Session db = ContextMap.getContext().getDB();
		Jump jumpNode = (Jump) db.get(Jump.class, jumpid);
		db.delete(jumpNode);
	}

	private void destroyJumpNode(StarSystem oldsystem, StarSystem system, Serializable jumpnodeid)
	{
		Session db = ContextMap.getContext().getDB();
		JumpNode jumpNode = (JumpNode) db.get(JumpNode.class, jumpnodeid);
		db.delete(jumpNode);
	}

	private void destroyShip(StarSystem oldsystem, StarSystem system, Serializable shipid)
	{
		Ship ship = (Ship) ContextMap.getContext().getDB().get(Ship.class, shipid);
		ship.destroy();
	}

	private void entferneNebel(StarSystem oldsystem, StarSystem system, MutableLocation loc)
	{
		if( loc.getX() > system.getWidth() || loc.getY() > system.getHeight() )
		{
			Nebel nebel = (Nebel) ContextMap.getContext().getDB().get(Nebel.class, loc);
			ContextMap.getContext().getDB().delete(nebel);
		}
	}

	private void aktualisiereBasen(StarSystem oldsystem, StarSystem system, Integer id)
	{
		Base base = (Base) ContextMap.getContext().getDB().get(Base.class, id);
		if (base.getX() > system.getWidth())
		{
			base.setX(system.getWidth());
		}
		if (base.getY() > system.getHeight())
		{
			base.setY(system.getHeight());
		}
	}

	private void aktualisiereSchlachten(StarSystem oldsystem, StarSystem system, Integer id)
	{
		Battle battle = (Battle) ContextMap.getContext().getDB().get(Battle.class, id);
		if (battle.getX() > system.getWidth())
		{
			battle.setX(system.getWidth());
		}
		if (battle.getY() > system.getHeight())
		{
			battle.setY(system.getHeight());
		}
	}

	private void aktualisiereSchiffe(StarSystem oldsystem, StarSystem system, Integer id)
	{
		Ship ship = (Ship) ContextMap.getContext().getDB().get(Ship.class, id);
		if (ship.getX() > system.getWidth())
		{
			ship.setX(system.getWidth());
		}
		if (ship.getY() > system.getHeight())
		{
			ship.setY(system.getHeight());
		}
	}
}
