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

import net.driftingsouls.ds2.server.WellKnownAdminPermission;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.io.IOException;
import java.util.List;

/**
 * Zeigt an, was wie oft versteigert wurde und welche Durchschnittspreise erziehlt wurden.
 * @author Christopher Jung
 *
 */
@AdminMenuEntry(category="GTU", name="Preisliste", permission = WellKnownAdminPermission.GTU_PRICES)
public class GtuPrices implements AdminPlugin {

	@Override
	public void output(StringBuilder echo) throws IOException {
		Context context = ContextMap.getContext();

		org.hibernate.Session db = context.getDB();

		echo.append("<div class='gfxbox' style='width:700px'>");
		echo.append("<table>\n");
		echo.append("<thead>");
		echo.append("<tr><td>Menge</td><td>Typ</td><td>Verkauft</td><td>Durchschnittspreis</td></tr>\n");
		echo.append("</thead>");

		// Resourcen
		echo.append("<tbody>");
		echo.append("<tr><td colspan=\"4\">Artefakte</td></tr>\n");

		List<?> rt = db
			.createQuery("select type,sum(preis)/count(preis) as avprice, count(preis) as menge " +
					"from StatGtu " +
					"where mType=2 " +
					"group by type order by sum(preis)/count(preis) desc" )
			.list();
		for( Object obj : rt )
		{
			Object[] row = (Object[])obj;

			echo.append("<tr><td>");
			Cargo cargo = new Cargo( Cargo.Type.AUTO, (String)row[0]);
			cargo.setOption( Cargo.Option.SHOWMASS, false );
			ResourceList reslist = cargo.getResourceList();
			if( reslist.size() == 0 )
			{
				continue;
			}
			ResourceEntry res = reslist.iterator().next();
			double avprice = ((Number)row[1]).doubleValue();
			long menge = ((Number)row[2]).longValue();

			echo.append("<tr><td>");
			echo.append(res.getCount1()).append("x</td><td><img src=\"").append(res.getImage()).append("\" alt=\"\" />").append(res.getName());
			echo.append("</td>\n");
			echo.append("<td>").append(Common.ln(menge)).append("x</td>\n");
			echo.append("<td>").append(Common.ln(avprice)).append(" RE\n");
			if( res.getCount1() > 1 ) {
				echo.append(" &#216;").append(Common.ln(avprice / res.getCount1())).append("/E\n");
			}
			echo.append("</td></tr>\n");
		}

		//Schiffe
		echo.append("<tr><td colspan=\"4\">Schiffe</td></tr>\n");
		List<?> st = db
			.createQuery("select st,sum(sg.preis)/count(sg.preis) as avprice, count(sg.preis) as menge "+
					"from ShipType st, StatGtu sg "+
					"where st.id=sg.type and sg.mType=1 "+
					"group by sg.type order by sum(sg.preis)/count(sg.preis) desc" )
			.list();
		for( Object obj : st )
		{
			Object[] row = (Object[])obj;
			ShipType type = (ShipType)row[0];

			echo.append("<tr><td></td><td><a class=\"forschinfo\" " + "href=\"./ds?module=schiffinfo&sess=$sess&ship=").append(type.getId()).append("\">").append(type.getNickname()).append("</a> (").append(type.getId()).append(")</td>\n");
			echo.append("<td>").append(row[2]).append("x</td>\n");
			echo.append("<td>").append(Common.ln((Number) row[1])).append(" RE</td></tr>\n");
		}

		echo.append("</tbody>");
		echo.append("</table>");
		echo.append("</div>");
	}
}
