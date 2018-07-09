/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Generiert eine Karte eines Systems.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="Objekte", name="Karte", permission = WellKnownAdminPermission.BASES_MAP)
public class BasesMap implements AdminPlugin 
{
	private static final Logger LOG = LogManager.getLogger(BasesMap.class);

    @Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();

		int user = context.getRequest().getParameterInt("user");
		int sysid = context.getRequest().getParameterInt("system");
		int otherastis = context.getRequest().getParameterInt("otherastis");
		int scale = context.getRequest().getParameterInt("scale");
		if( scale == 0 ) 
		{
			scale = 2;
		}

		if( context.getRequest().getParameterInt("doImage") != 0 )
		{
			outputImage(user, sysid, otherastis, scale);
			return;
		}
		
		org.hibernate.Session db = context.getDB();
		
		echo.append("Karte:\n");
		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<table class=\"noBorder\" width=\"300\">\n");
		echo.append("<tr><td class=\"noBorderS\">User:</td><td class=\"noBorderS\"><input type=\"text\" name=\"user\" size=\"10\" value=\"").append(user).append("\" /></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">System:</td><td class=\"noBorderS\"><input type=\"text\" name=\"system\" size=\"10\" value=\"").append(sysid).append("\" /></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\">Scale:</td><td class=\"noBorderS\"><input type=\"text\" name=\"scale\" size=\"10\" value=\"").append(scale).append("\" /></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\" colspan=\"2\"><input type=\"checkbox\" name=\"otherastis\" id=\"form_otherastis\" value=\"1\" ").append(otherastis != 0 ? "checked=\"checked\"" : "").append(" /><label for=\"form_otherastis\">Asteroiden anderer Spieler anzeigen</label></td></tr>\n");
		echo.append("<tr><td class=\"noBorderS\" colspan=\"2\" align=\"center\">\n");
		echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<input type=\"submit\" value=\"anzeigen\" style=\"width:100px\"/></td></tr>\n");
		echo.append("</table>\n");
		echo.append("</form>\n");
		echo.append("<br />\n");
		
		if( sysid != 0 ) 
		{
			StarSystem system = (StarSystem)db.get(StarSystem.class, sysid);
			if( system == null )
			{
				return;
			}
			
			echo.append("<img src=\"./ds?module=admin&namedplugin=").append(this.getClass().getName())
					.append("&action=binary&doImage=1&user=").append(user)
					.append("&system=").append(sysid)
					.append("&otherastis=").append(otherastis)
					.append("&scale=").append(scale)
					.append("\" alt=\"\" />\n");
		}
	}

	private void outputImage(int user, int sysid, int otherastis, int scale)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		StarSystem system = (StarSystem)db.get(StarSystem.class, sysid);
		BufferedImage image = new BufferedImage(system.getWidth()*scale, system.getHeight()*scale, BufferedImage.TYPE_INT_RGB);
		Color black = new Color(0, 0, 0);
		Color blue = new Color(0, 0, 255);
		Color green = new Color(0, 255, 0);
		Color red = new Color(255, 0, 0);
		Color yellow = new Color(255, 255, 0);
		Color grey = new Color(128, 128, 128);

		Graphics2D g = image.createGraphics();
		g.setColor(black);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());

		g.setColor(green);

		List<Object[]> bases = Common.cast(db.createQuery("select x,y from Base where owner= :user and system= :system")
				.setInteger("user", user)
				.setInteger("system", system.getID())
				.list());
		for( Object[] base : bases )
		{
			g.fillRect((Integer)base[0]*scale, (Integer)base[1]*scale, scale, scale);
		}

		if( otherastis != 0 )
		{
			g.setColor(grey);
			bases = Common.cast(db.createQuery("select x,y from Base where owner!= :user and system= :system")
					.setInteger("user", user)
					.setInteger("system", system.getID())
					.list());
			for( Object[] base : bases )
			{
				g.fillRect((Integer)base[0]*scale, (Integer)base[1]*scale, scale, scale);
			}
		}

		g.setColor(red);
		List<Object[]> nebel = Common.cast(db.createQuery("select loc.x,loc.y from Nebel where loc.system= :system")
				.setInteger("system", system.getID())
				.list());
		for( Object[] aNebel : nebel )
		{
			g.fillRect((Integer)aNebel[0]*scale, (Integer)aNebel[1]*scale, scale, scale);
		}

		g.setColor(yellow);
		List<Object[]> jns = Common.cast(db.createQuery("select x,y from JumpNode where system= :system")
				.setInteger("system", system.getID())
				.list());
		for( Object[] jn : jns )
		{
			g.fillRect((Integer)jn[0]*scale, (Integer)jn[1]*scale, scale, scale);
		}

		g.setColor(blue);
		Location[] locs = system.getOrderLocations();
		for (Location loc : locs)
		{
			g.fillRect(loc.getX() * scale, loc.getY() * scale, scale, scale);
		}
		g.dispose();

		try
		{
			ImageIO.write(image, "png", context.getResponse().getOutputStream());
		}
		catch( IOException e )
		{
			LOG.error("Konnte png nicht schreiben", e);
		}
	}
}
