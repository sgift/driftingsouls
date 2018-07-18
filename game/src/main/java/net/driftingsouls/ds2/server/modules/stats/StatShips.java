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
package net.driftingsouls.ds2.server.modules.stats;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt an, wie oft ein Schiff in DS vorkommt.
 * @author Christopher Jung
 *
 */
public class StatShips implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Writer echo = context.getResponse().getWriter();

		echo.append("<h1>Schiffe in Spielerhand:</h1>");
		echo.append("<table cellspacing=\"1\" cellpadding=\"1\">\n");
		List<?> tmp = db.createQuery("SELECT st,count(*) " +
				"FROM Ship s join s.shiptype st " +
				"WHERE s.owner.id>:minid " +
				"GROUP BY st " +
				"ORDER BY st.nickname")
				.setInteger("minid", StatsController.MIN_USER_ID)
				.list();
		for( Object o : tmp )
		{
			Object[] data = (Object[])o;
			ShipType st = (ShipType)data[0];
      		echo.append("<tr><td align='right'>").append(Common.ln((Long) data[1])).append("</td><td>").append(st.getNickname()).append(" <a class=\"forschinfo\" onclick='ShiptypeBox.show(").append(Integer.toString(st.getId())).append(");return false;' href=\"./ds?module=schiffinfo&ship=").append(Integer.toString(st.getId())).append("\">(?)</a>").append("</td></tr>\n");
		}
		echo.append("</table><br /><br />");
	}
}
