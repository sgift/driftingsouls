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

import net.driftingsouls.ds2.server.ContextCommon;
import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.statistik.StatVerkaeufe;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.Ship;

import java.io.IOException;
import java.util.List;

/**
 * Ermoeglicht das Einfuegen von neuen Versteigerungen in die GTU.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Verkaufsdaten", permission = WellKnownAdminPermission.GTU_VERKAEUFE)
public class GtuVerkaeufe implements AdminPlugin
{
	@Override
	public void output(StringBuilder echo) throws IOException
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		int system = context.getRequest().getParameterInt("system");
		String type = context.getRequest().getParameterString("type");

		List<StarSystem> systems = Common.cast(db.createQuery("from StarSystem").list());

		if( type.length() == 0  )
		{
			echo.append("<div class='gfxbox' style='width:450px'>");
			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<table width=\"100%\">\n");
			echo.append("<tr><td style=\"width:60px\">System:</td><td>");
			echo.append("<select name=\"system\" size=\"1\">\n");

			for( StarSystem sys : systems )
			{
				echo.append("<option value=\"").append(sys.getID()).append("\">").append(sys.getName()).append(" (").append(sys.getID()).append(")</option>\n");
			}

			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td>Verkaufsort:</td><td>\n");
			echo.append("<select name=\"type\" size=\"1\">\n");
			echo.append("<option value=\"asti\">Basisverkauf</option>\n");
			echo.append("<option value=\"tradepost\">Handelsposten</option>\n");
			echo.append("<option>--------------</option>");
			List<String> places = Common.cast(db
				.createQuery("select distinct place from StatVerkaeufe where place not in ('asti','tradepost')")
				.list());
			for( String place : places )
			{
				if( !place.startsWith("p") )
				{
					continue;
				}
				Ship s = (Ship)db.get(Ship.class, Integer.valueOf(place.substring(1)));
				if( s == null )
				{
					continue;
				}
				echo.append("<option value='").append(place).append("'>").append(s.getName()).append(" (").append(s.getLocation().displayCoordinates(false)).append(")</option>");
			}
			echo.append("</select>\n");
			echo.append("</td></tr>\n");
			echo.append("<tr><td colspan=\"2\" style=\"text-align:center\">\n");
			echo.append("<input type=\"hidden\" name=\"namedplugin\" value=\"").append(getClass().getName()).append("\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"submit\" value=\"anzeigen\" style=\"width:100px\"/></td></tr>\n");
			echo.append("</table>\n");
			echo.append("</form>\n");
			echo.append("</div>");
		}
		else
		{
			int entryCount = 0;
			Cargo totalcargo = new Cargo();
			Cargo cargo = new Cargo();
			final int tick = context.get(ContextCommon.class).getTick();

			List<StatVerkaeufe> entries = Common.cast(db.createQuery("from StatVerkaeufe " +
					"where system= :system and place= :type")
				.setInteger("system", system)
				.setString("type", type)
				.list());
			for( StatVerkaeufe entry : entries )
			{
				Cargo ecargo = entry.getStats();

				totalcargo.addCargo(ecargo);
				entryCount++;

				if( (entry.getTick() != tick) && (entry.getTick() >= tick-7) )
				{
					cargo.addCargo(ecargo);
				}
			}

			echo.append("<div class='gfxbox' style='width:400px'>");
			echo.append("System: ").append(system).append(" - Type: ").append(type).append("<br /><br />");
			echo.append("<table width=\"100%\">\n");
			echo.append("<tr>\n");
			echo.append("<td>Item</td>");
			echo.append("<td>Durchschnitt pro Tick</td>\n");
			echo.append("<td>Zuletzt (7 Ticks)</td>\n");
			echo.append("</tr>\n");

			ResourceList reslist = totalcargo.getResourceList();
			for( ResourceEntry res : reslist )
			{
				echo.append("<tr>\n");
				echo.append("<td>\n");
				echo.append("<img src=\"").append(res.getImage()).append("\" alt=\"\" title=\"").append(res.getPlainName()).append("\" />").append(res.getName()).append("</td>");
				echo.append("<td>&#216;").append(Common.ln(Math.round(res.getCount1() / (double) entryCount))).append("\n</td>");
				echo.append("</td>\n");
				echo.append("<td>");
				echo.append(Common.ln(Math.round(cargo.getResourceCount(res.getId())/7d)));
				echo.append("</td></tr>\n");
			}
			echo.append("</table>");
			echo.append("</div>");
		}
	}
}
