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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.Jump;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Schiffsmodul fuer die Anzeige der shivanischen Sprungantriebe.
 * @author Christopher Jung
 *
 */
public class JumpdriveShivan implements SchiffPlugin
{
	@Override
	public String action(Parameters caller)
	{
		SchiffController controller = caller.controller;
		User user = (User)controller.getUser();
		Ship ship = caller.ship;

		String output = "";

		org.hibernate.Session db = controller.getDB();

		controller.parameterNumber("system");
		StarSystem system = (StarSystem)db.get(StarSystem.class, controller.getInteger("system"));

		if( ship.getOwner().getId() < 0 )
		{
			controller.parameterNumber("x");
			controller.parameterNumber("y");
			int x = controller.getInteger("x");
			int y = controller.getInteger("y");
			controller.parameterString("subaction");
			String subaction = controller.getString("subaction");
			controller.parameterNumber("instant");
			boolean instant = controller.getInteger("instant") == 1 && user.isNPC();


			if( "set".equals(subaction) && (system != null && system.getID() != 0) )
			{
				final Location targetLoc = new Location(system.getID(),x,y);

				ShipTypeData mst = ship.getTypeData();
				if( instant && Math.floor(mst.getEps() / 2d) > ship.getEnergy() )
				{
					output += ship.getName()+" hat nicht genug Energie<br />\n";
					return output;
				}

				output += ship.getName()+" aktiviert den Sprungantrieb<br />\n";

				List<Ship> ships = new ArrayList<Ship>();
				ships.add(ship);

				if( ship.getFleet() != null ) {
					output += "<table class=\"noBorder\">\n";

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=? and owner=? and docked='' and id!=?")
						.setEntity(0, ship.getFleet())
						.setEntity(1, ship.getOwner())
						.setInteger(2, ship.getId())
						.list();

					for( Iterator<?> iter=sList.iterator(); iter.hasNext(); )
					{
						Ship aship = (Ship)iter.next();

						ShipTypeData st = aship.getTypeData();
						if( !st.hasFlag(ShipTypes.SF_JUMPDRIVE_SHIVAN) )
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+aship.getName()+" ("+aship.getId()+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						if( instant && Math.floor(st.getEps() / 2d) > aship.getEnergy() )
						{
							output += "Nicht genug Energie";
						}
						else
						{
							output += "Das Schiff aktiviert den Sprungantrieb";

							ships.add(aship);
						}

						output += "</span></td></tr>\n";
					}
				}

				if( instant )
				{
					for( Ship aship : ships )
					{
						aship.setEnergy(aship.getEnergy()-(int)Math.floor(aship.getTypeData().getEps()/2));
						aship.setSystem(system.getID());
						aship.setX(x);
						aship.setY(y);
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
				Jump jump = (Jump)db.createQuery("from Jump where shipid=?")
					.setEntity(0, ship)
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

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=? and owner=? and docked='' and id!=?")
						.setEntity(0, ship.getFleet())
						.setEntity(1, ship.getOwner())
						.setInteger(2, ship.getId())
						.list();

					for( Iterator<?> iter=sList.iterator(); iter.hasNext(); )
					{
						Ship aship = (Ship)iter.next();

						ShipTypeData st = aship.getTypeData();
						if( !st.hasFlag(ShipTypes.SF_JUMPDRIVE_SHIVAN) )
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+aship.getName()+" ("+aship.getId()+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff &auml;ndert das Sprungziel";

						jump = (Jump)db.createQuery("from Jump where shipid=?")
							.setEntity(0, ship)
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
				Jump jump = (Jump)db.createQuery("from Jump where shipid=?")
					.setEntity(0, ship)
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

					List<?> sList = db.createQuery("from Ship where id>0 and fleet=? and owner=? and docked='' and id!=?")
						.setEntity(0, ship.getFleet())
						.setEntity(1, ship.getOwner())
						.setInteger(2, ship.getId())
						.list();

					for( Iterator<?> iter=sList.iterator(); iter.hasNext(); )
					{
						Ship aship = (Ship)iter.next();
						ShipTypeData st = aship.getTypeData();
						if( !st.hasFlag(ShipTypes.SF_JUMPDRIVE_SHIVAN) )
						{
							continue;
						}

						output += "<tr>";
						output += "<td valign=\"top\" class=\"noBorderS\"><span style=\"color:orange;font-size:12px\"> "+aship.getName()+" ("+aship.getId()+"):</span></td><td class=\"noBorderS\"><span style=\"font-size:12px\">\n";
						output += "Das Schiff stoppt den Sprungantrieb";

						db.createQuery("delete from Jump where shipid=?")
							.setEntity(0, aship)
							.executeUpdate();

						output += "</span></td></tr>\n";
					}
				}
			}
		}

		return output;
	}

	@Override
	public void output(Parameters caller)
	{
		SchiffController controller = caller.controller;
		String pluginid = caller.pluginId;
		User user = (User)controller.getUser();
		Ship ship = caller.ship;

		org.hibernate.Session db = controller.getDB();

		TemplateEngine t = controller.getTemplateEngine();
		t.setFile("_PLUGIN_"+pluginid, "schiff.jumpdrive.shivan.html");

		Jump jump = (Jump)db.createQuery("from Jump where shipid=?")
			.setEntity(0, ship)
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
