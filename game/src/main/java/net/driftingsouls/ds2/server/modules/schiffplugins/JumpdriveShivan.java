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
package net.driftingsouls.ds2.server.modules.schiffplugins;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.Action;
import net.driftingsouls.ds2.server.framework.pipeline.controllers.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypeFlag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Schiffsmodul fuer die Anzeige der shivanischen Sprungantriebe.
 * @author Christopher Jung
 *
 */
@Component
public class JumpdriveShivan implements SchiffPlugin
{
	@Action(ActionType.DEFAULT)
	public String action(Parameters caller, StarSystem system, int x, int y, String subaction, boolean instant)
	{
		SchiffController controller = caller.controller;
		User user = (User)controller.getUser();
		Ship ship = caller.ship;

		String output = "";

		org.hibernate.Session db = controller.getDB();

		if( ship.getOwner().getId() < 0 )
		{
			instant = instant && user.isNPC();

			if( "set".equals(subaction) && (system != null && system.getID() != 0) )
			{
				final Location targetLoc = new Location(system.getID(),x,y);

				output += ship.getName()+" aktiviert den Sprungantrieb<br />\n";

				List<Ship> ships = new ArrayList<>();
				ships.add(ship);

				if( ship.getFleet() != null ) {
					output += "<table class=\"noBorder\">\n";

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=:fleet and owner=:owner and docked='' and id!=:id")
						.setEntity("fleet", ship.getFleet())
						.setEntity("owner", ship.getOwner())
						.setInteger("id", ship.getId())
						.list();

					for (Object aSList : sList)
					{
						Ship aship = (Ship) aSList;

						ShipTypeData st = aship.getTypeData();
						if (!st.hasFlag(ShipTypeFlag.JUMPDRIVE_SHIVAN))
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> " + aship.getName() + " (" + aship.getId() + "):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";

						output += "Das Schiff aktiviert den Sprungantrieb";

						ships.add(aship);

						output += "</span></td></tr>\n";
					}
				}

				if( instant )
				{
					for( Ship aship : ships )
					{
						aship.setLocation(targetLoc);
					}
				}
				else
				{
					for( Ship aship : ships )
					{
						Jump target = new Jump(aship, targetLoc);
						db.persist(target);
					}
				}
			}
			else if ( "newtarget".equals(subaction) && (system != null && system.getID() != 0) )
			{
				Jump jump = (Jump)db.createQuery("from Jump where ship=:ship")
					.setEntity("ship", ship)
					.uniqueResult();

				if( jump == null )
				{
					return output;
				}

				jump.setSystem(system.getID());
				jump.setX(x);
				jump.setY(y);

				output += ship.getName()+" &auml;ndert das Sprungziel.<br />\n";

				if( ship.getFleet() != null )
				{
					output += "<table class=\"noBorder\">\n";

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=:fleet and owner=:owner and docked='' and id!=:id")
						.setEntity("fleet", ship.getFleet())
						.setEntity("owner", ship.getOwner())
						.setInteger("id", ship.getId())
						.list();

					for (Object aSList : sList)
					{
						Ship aship = (Ship) aSList;

						ShipTypeData st = aship.getTypeData();
						if (!st.hasFlag(ShipTypeFlag.JUMPDRIVE_SHIVAN))
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> " + aship.getName() + " (" + aship.getId() + "):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff &auml;ndert das Sprungziel";

						jump = (Jump) db.createQuery("from Jump where ship=:ship")
								.setEntity("ship", ship)
								.uniqueResult();
						jump.setSystem(system.getID());
						jump.setX(x);
						jump.setY(y);

						output += "</span></td></tr>\n";
					}
				}
			}
			else if ( "cancel".equals(subaction) )
			{
				Jump jump = (Jump)db.createQuery("from Jump where ship=:ship")
					.setEntity("ship", ship)
					.uniqueResult();

				if( jump == null )
				{
					return output;
				}

				output += ship.getName()+" stoppt den Sprungantrieb<br />\n";

				db.delete(jump);

				if( ship.getFleet() != null )
				{
					output += "<table class=\"noBorder\">\n";

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=:fleet and owner=:owner and docked='' and id!=:id")
						.setEntity("fleet", ship.getFleet())
						.setEntity("owner", ship.getOwner())
						.setInteger("id", ship.getId())
						.list();

					for (Object aSList : sList)
					{
						Ship aship = (Ship) aSList;
						ShipTypeData st = aship.getTypeData();
						if (!st.hasFlag(ShipTypeFlag.JUMPDRIVE_SHIVAN))
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> " + aship.getName() + " (" + aship.getId() + "):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff stoppt den Sprungantrieb";

						db.createQuery("delete from Jump where ship=:ship")
								.setEntity("ship", aship)
								.executeUpdate();

						output += "</span></td></tr>\n";
					}
				}
			}
		}

		return output;
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller)
	{
		SchiffController controller = caller.controller;
		String pluginid = caller.pluginId;
		User user = (User)controller.getUser();
		Ship ship = caller.ship;

		org.hibernate.Session db = controller.getDB();

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.jumpdrive.shivan.html");

		Jump jump = (Jump)db.createQuery("from Jump where ship=:ship")
			.setEntity("ship", ship)
			.uniqueResult();

		t.setVar(	"global.pluginid",				pluginid,
					"ship.id",						ship.getId(),
					"schiff.jumpdrive.jumping",		jump == null ? 0 : jump.getSystem(),
					"schiff.jumpdrive.jumpingx",	jump == null ? 0 : jump.getX(),
					"schiff.jumpdrive.jumpingy",	jump == null ? 0 : jump.getY(),
					"schiff.jumpdrive.subaction",	"set",
					"user.npc",						user.isNPC());

		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
