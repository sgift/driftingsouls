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
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Waypoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;

/**
 * Schiffsmodul fuer die Anzeige der Navigation.
 * @author Christopher Jung
 *
 */
public class NavigationDefault implements SchiffPlugin {
	private static final Log log = LogFactory.getLog(NavigationDefault.class);

	@Action(ActionType.DEFAULT)
	public String action(Parameters caller, String setdest, int system, int x, int y, String com, boolean bookmark, int act, int count, int targetx, int targety) {
		Ship ship = caller.ship;

		String output = "";

		//Wird eine neue Beschreibung gesetzt?
		if( setdest.length() > 0 ) {
			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setDestSystem(system);
			einstellungen.setDestX(x);
			einstellungen.setDestY(y);
			einstellungen.setDestCom(com);
			einstellungen.setBookmark(bookmark);
			einstellungen.persistIfNecessary(ship);

			output += "Neues Ziel: "+system+":"+x+"/"+y+"<br />Beschreibung: "+Common._plaintitle(com);
			if( bookmark ) {
				output += "<br />[Bookmark]\n";
			}
			return output;
		}

		if( (act > 9) || (act < 1) || count <= 0 ) {
			return "Ung&uuml;ltige Flugparameter<br />\n";
		}

		//Wir wollen nur beim Autopiloten lowheat erzwingen
		boolean forceLowHeat = false;
		RouteFactory router = new RouteFactory();
		List<Waypoint> route;
		if( targetx == 0 || targety == 0 ) {
			route = router.getMovementRoute(act, count);
		}
		else {
			forceLowHeat = true;
			Location from = ship.getLocation();
			route = router.findRoute(from, new Location(from.getSystem(), targetx, targety));
		}

		//Das Schiff soll sich offenbar bewegen
		ship.move(route, forceLowHeat, false);
		output += Ship.MESSAGE.getMessage();

		return output;
	}

	private static class SectorImage
	{
		private final String image;
		private final int x;
		private final int y;
		private final int size;

		private SectorImage(String image)
		{
			this(image, 0, 0, 0);
		}

		private SectorImage(String image, int size, int x, int y)
		{
			this.image = image;
			this.x = x;
			this.y = y;
			this.size = size;
		}
	}

	@Action(ActionType.DEFAULT)
	public void output(Parameters caller)
    {
		String pluginid = caller.pluginId;
		Ship data = caller.ship;
		ShipTypeData datatype = caller.shiptype;
		SchiffController controller = caller.controller;
		User user = (User)controller.getUser();
		org.hibernate.Session db = controller.getDB();

		TemplateEngine t = caller.t;
		t.setFile("_PLUGIN_"+pluginid, "schiff.navigation.default.html");

		t.setVar(	"global.pluginid",					pluginid,
					"ship.id",							data.getId(),
					"schiff.navigation.docked",			data.isDocked() || data.isLanded(),
					"schiff.navigation.docked.extern",	!data.isLanded(),
					"schiff.navigation.bookmarked",		data.getEinstellungen().isBookmark(),
					"schiff.navigation.dest.x",			data.getEinstellungen().getDestX(),
					"schiff.navigation.dest.y",			data.getEinstellungen().getDestY(),
					"schiff.navigation.dest.system",	data.getEinstellungen().getDestSystem(),
					"schiff.navigation.dest.text",		data.getEinstellungen().getDestCom() );

		if(data.isDocked() || data.isLanded() )
		{
			Ship mastership = data.getBaseShip();

			if(mastership != null)
			{
				t.setVar(	"schiff.navigation.docked.master.name",	mastership.getName(),
							"schiff.navigation.docked.master.id",	mastership.getId() );
			}
			else
			{
				log.error("Illegal docked entry for ship " + data.getId());
			}
		}
		else if( datatype.getCost() == 0 )
		{
			t.setVar("schiff.navigation.showmessage","Dieses Objekt hat keinen Antrieb");
		}
		else
		{
			int x = data.getX();
			int y = data.getY();
			int sys = data.getSystem();

			SectorImage[][] sectorimgs = new SectorImage[3][3];
			List<?> jnlist = db.createQuery("from JumpNode where system=:sys and (x between :xstart and :xend) and (y between :ystart and :yend)")
				.setInteger("sys", sys)
				.setInteger("xstart", x-1)
				.setInteger("xend", x+1)
				.setInteger("ystart", y-1)
				.setInteger("yend", y+1)
				.list();

			for (Object aJnlist : jnlist)
			{
				JumpNode jn = (JumpNode) aJnlist;
				sectorimgs[jn.getX() - x + 1][jn.getY() - y + 1] = new SectorImage("./data/starmap/jumpnode/jumpnode.png");
			}

			List<Base> baselist = Common.cast(db.createQuery("select distinct b from Base b where b.system=:sys and floor(sqrt(pow(:x-b.x,2)+pow(:y-b.y,2))) <= b.size+1")
				.setInteger("sys", sys)
				.setInteger("x", x)
				.setInteger("y", y)
				.list());

			for (int ny = 0; ny <= 2; ny++)
			{
				for (int nx = 0; nx <= 2; nx++)
				{
					Location curLoc = new Location(sys, x + nx - 1, y + ny - 1);

					int cx = nx;
					int cy = ny;
					baselist.stream()
						.filter(b -> b.getOwner() != user)
						.filter(b -> curLoc.sameSector(0, b.getLocation(), b.getSize()))
						.forEach(aBase -> {
							int[] offset = aBase.getBaseImageOffset(curLoc);
							sectorimgs[cx][cy] = new SectorImage(aBase.getBaseImage(aBase.getLocation()), aBase.getSize(), offset[0], offset[1]);
						});

					baselist.stream()
							.filter(b -> b.getOwner() == user)
							.filter(b -> curLoc.sameSector(0, b.getLocation(), b.getSize()))
							.forEach(aBase -> sectorimgs[cx][cy] = new SectorImage("./data/starmap/asti_own/asti_own.png"));
				}
			}

			List<?> nebellist = db.createQuery("from Nebel where loc.system=:sys and (loc.x between :xstart and :xend) and (loc.y between :ystart and :yend)")
				.setInteger("sys", sys)
				.setInteger("xstart", x-1)
				.setInteger("xend", x+1)
				.setInteger("ystart", y-1)
				.setInteger("yend", y+1)
				.list();
			for (Object aNebellist : nebellist)
			{
				Nebel nebel = (Nebel) aNebellist;
				sectorimgs[nebel.getX() - x + 1][nebel.getY() - y + 1] = new SectorImage(nebel.getImage());
			}

			int tmp = 0;
			t.setVar("schiff.navigation.size",37);

			Location[] locs = new Location[8];
			for( int ny = 0, index=0; ny <= 2; ny++ ) {
				for( int nx = 0; nx <= 2; nx++ ) {
					if( nx == 1 && ny == 1 ) {
						continue;
					}
					locs[index++] = new Location(sys,x+nx-1,y+ny-1);
				}
			}

			Set<Location> alertStatus = Ship.getAlertStatus(user, locs);

			boolean newrow;

			t.setBlock("_NAVIGATION","schiff.navigation.nav.listitem","schiff.navigation.nav.list");
			for( int ny = 0; ny <= 2; ny++ ) {
				newrow = true;
				for( int nx = 0; nx <= 2; nx++ ) {
					tmp++;

					SectorImage img = sectorimgs[nx][ny];
					if( img == null )
					{
						img = new SectorImage("./data/starmap/space/space.png");
					}

					int sizeX = (img.size*2+1)*37;
					int sizeY = (img.size*2+1)*37;

					Location sector = new Location(sys, x + nx - 1, y + ny - 1);
					t.setVar(	"schiff.navigation.nav.direction",		tmp,
								"schiff.navigation.nav.location",		sector.displayCoordinates(true),
								"schiff.navigation.nav.sectorimage",	img.image,
								"schiff.navigation.nav.sectorimage.x",	img.x*37,
								"schiff.navigation.nav.sectorimage.y",	img.y*37,
								"schiff.navigation.nav.sectorimage.w",	sizeX,
								"schiff.navigation.nav.sectorimage.h",	sizeY,
								"schiff.navigation.nav.newrow",			newrow,
								"schiff.navigation.nav.warn",			((1 != nx || 1 != ny) && alertStatus.contains(sector)) );

					t.parse( "schiff.navigation.nav.list", "schiff.navigation.nav.listitem", true );
					newrow = false;
				}
			}
		}
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
