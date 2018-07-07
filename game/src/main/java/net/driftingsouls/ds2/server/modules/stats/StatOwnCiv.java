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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.ShipType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt allgemeine Daten ueber den Account des Spielers an.
 * @author Christopher Jung
 *
 */
public class StatOwnCiv implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		org.hibernate.Session db = context.getDB();

		Writer echo = context.getResponse().getWriter();
		echo.append("<h1>Meine Zivilisation:</h1>");
		echo.append("<table cellspacing=\"3\" cellpadding=\"3\" width=\"100%\">\n");

		long crew = 0;

		echo.append("<tr><td valign=\"top\">Schiffe:</td><td>\n");

		List<?> tmp = db
				.createQuery("SELECT st,count(*),sum(s.crew) " +
					"FROM Ship s join s.shiptype st " +
					"WHERE s.id>0 AND s.owner=:user GROUP BY st order by st.nickname")
				.setEntity("user", user)
				.list();

		for (Object o : tmp)
		{
			Object[] data = (Object[])o;
			ShipType st = (ShipType)data[0];
			echo.append(Common.ln((Long) data[1])).append(" ").append(st.getNickname()).append("<br />\n");
			crew += (Long)data[2];
		}

		Long population = (Long)db
				.createQuery("SELECT sum(bewohner) FROM Base WHERE owner=:user")
				.setEntity("user", user)
				.uniqueResult();

		if( population == null )
		{
			population = 0L;
		}

		echo.append("</td></tr>\n");
		echo.append("<tr><td>Bev&ouml;lkerung:</td><td>").append(Common.ln(crew + population)).append("</td></tr>\n");
		echo.append("<tr><td>Bewohner:</td><td>").append(Common.ln(population)).append("</td></tr>\n");
		echo.append("<tr><td>Crew:</td><td>").append(Common.ln(crew)).append("</td></tr>\n");
		echo.append("</table><br /><br />\n");
	}
}
