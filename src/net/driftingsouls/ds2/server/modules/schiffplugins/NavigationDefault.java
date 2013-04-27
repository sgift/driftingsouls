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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.Nebel;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Configuration;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.modules.SchiffController;
import net.driftingsouls.ds2.server.ships.RouteFactory;
import net.driftingsouls.ds2.server.ships.SchiffEinstellungen;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.Waypoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Schiffsmodul fuer die Anzeige der Navigation.
 * @author Christopher Jung
 *
 */
@Configurable
public class NavigationDefault implements SchiffPlugin {
	private static final Log log = LogFactory.getLog(NavigationDefault.class);

	private Configuration config;

    /**
     * Injiziert die DS-Konfiguration.
     * @param config Die DS-Konfiguration
     */
    @Autowired
    public void setConfiguration(Configuration config)
    {
    	this.config = config;
    }

    @Override
	public String action(Parameters caller) {
		Ship ship = caller.ship;
		SchiffController controller = caller.controller;

		String output = "";

		controller.parameterString("setdest");
		String setdest = controller.getString("setdest");

		//Wird eine neue Beschreibung gesetzt?
		if( setdest.length() > 0 ) {
			controller.parameterNumber("system");
			controller.parameterNumber("x");
			controller.parameterNumber("y");
			controller.parameterString("com");
			controller.parameterNumber("bookmark");

			int system = controller.getInteger("system");
			int x = controller.getInteger("x");
			int y = controller.getInteger("y");
			String com = controller.getString("com");
			int bookmark = controller.getInteger("bookmark");

			SchiffEinstellungen einstellungen = ship.getEinstellungen();
			einstellungen.setDestSystem(system);
			einstellungen.setDestX(x);
			einstellungen.setDestY(y);
			einstellungen.setDestCom(com);
			einstellungen.setBookmark(bookmark != 0);
			einstellungen.persistIfNecessary(ship);

			output += "Neues Ziel: "+system+":"+x+"/"+y+"<br />Beschreibung: "+Common._plaintitle(com);
			if( bookmark != 0 ) {
				output += "<br />[Bookmark]\n";
			}
			return output;
		}

		controller.parameterNumber("act");
		controller.parameterNumber("count");
		controller.parameterNumber("targetx");
		controller.parameterNumber("targety");
		int act = controller.getInteger("act");
		int count = controller.getInteger("count");
		int targetx = controller.getInteger("targetx");
		int targety = controller.getInteger("targety");

		if( (act > 9) || (act < 1) || count <= 0 ) {
			return "Ung&uuml;ltige Flugparameter<br />\n";
		}

		//Wir wollen nur beim Autopiloten lowheat erzwingen
		boolean forceLowHeat = false;
		RouteFactory router = new RouteFactory();
		List<Waypoint> route = null;
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

    @Override
	public void output(Parameters caller)
    {
		String pluginid = caller.pluginId;
		Ship data = caller.ship;
		ShipTypeData datatype = caller.shiptype;
		SchiffController controller = caller.controller;
		User user = (User)controller.getUser();
		org.hibernate.Session db = controller.getDB();

		TemplateEngine t = controller.getTemplateEngine();
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
				sectorimgs[jn.getX() - x + 1][jn.getY() - y + 1] = new SectorImage("jumpnode/jumpnode");
			}

			List<?> baselist = db.createQuery("select distinct b from Base b where b.system=:sys and floor(sqrt(pow(:x-b.x,2)+pow(:y-b.y,2))) <= b.size+1")
				.setInteger("sys", sys)
				.setInteger("x", x)
				.setInteger("y", y)
				.list();
			for (Object aBaselist : baselist)
			{
				Base aBase = (Base) aBaselist;

				if ((aBase.getSize() == 0) && (sectorimgs[aBase.getX() - x + 1][aBase.getY() - y + 1] == null))
				{
					if (aBase.getOwner() == user)
					{
						sectorimgs[aBase.getX() - x + 1][aBase.getY() - y + 1] = new SectorImage("asti_own/asti_own");
					}
					else
					{
						int[] offset = aBase.getBaseImageOffset(aBase.getLocation());
						sectorimgs[aBase.getX() - x + 1][aBase.getY() - y + 1] = new SectorImage(aBase.getBaseImage(aBase.getLocation()), aBase.getSize(), offset[0], offset[1]);
					}
				}
				else if (aBase.getSize() > 0)
				{
					Location loc = aBase.getLocation();
					for (int by = aBase.getY() - aBase.getSize(); by <= aBase.getY() + aBase.getSize(); by++)
					{
						for (int bx = aBase.getX() - aBase.getSize(); bx <= aBase.getX() + aBase.getSize(); bx++)
						{
							Location curLoc = new Location(aBase.getSystem(), bx, by);
							if (!loc.sameSector(aBase.getSize(), curLoc, 0))
							{
								continue;
							}
							if (Math.abs(x - bx) > 1 || Math.abs(y - by) > 1)
							{
								continue;
							}
							int[] offset = aBase.getBaseImageOffset(curLoc);
							sectorimgs[bx - x + 1][by - y + 1] = new SectorImage(aBase.getBaseImage(curLoc), aBase.getSize(), offset[0], offset[1]);
						}
					}
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
			final String url = config.get("URL");

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

			boolean[] alertStatus = Ship.getAlertStatus(user, locs);

			boolean newrow;

			t.setBlock("_NAVIGATION","schiff.navigation.nav.listitem","schiff.navigation.nav.list");
			for( int ny = 0, index=0; ny <= 2; ny++ ) {
				newrow = true;
				for( int nx = 0; nx <= 2; nx++ ) {
					tmp++;

					SectorImage img = sectorimgs[nx][ny];
					if( img == null )
					{
						img = new SectorImage("space/space");
					}

					int sizeX = (img.size*2+1)*37;
					int sizeY = (img.size*2+1)*37;

					t.setVar(	"schiff.navigation.nav.direction",		tmp,
								"schiff.navigation.nav.location",		new Location(sys, x+nx-1, y+ny-1).displayCoordinates(true),
								"schiff.navigation.nav.sectorimage",	url + "data/starmap/"+ img.image+".png",
								"schiff.navigation.nav.sectorimage.x",	img.x*37,
								"schiff.navigation.nav.sectorimage.y",	img.y*37,
								"schiff.navigation.nav.sectorimage.w",	sizeX,
								"schiff.navigation.nav.sectorimage.h",	sizeY,
								"schiff.navigation.nav.newrow",			newrow,
								"schiff.navigation.nav.warn",			(1 != nx || 1 != ny ? alertStatus[index++] : false) );

					t.parse( "schiff.navigation.nav.list", "schiff.navigation.nav.listitem", true );
					newrow = false;
				}
			}
		}
		t.parse(caller.target,"_PLUGIN_"+pluginid);
	}

}
