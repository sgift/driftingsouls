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

import java.io.IOException;
import java.io.Writer;

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

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
		Database db = context.getDatabase();

		Writer echo = context.getResponse().getWriter();
		echo.append("<table class=\"noBorderX\" cellspacing=\"3\" cellpadding=\"3\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\"colspan=\"2\" align=\"left\">Meine Zivilisation:</td></tr>\n");

		long crew = 0;

		echo.append("<tr><td class=\"noBorderX\" valign=\"top\">Schiffe:</td><td class=\"noBorderX\">\n");

		SQLQuery tmp = db.query("SELECT t1.type,t2.nickname,count(*) count,sum(t1.crew) sum " +
				"FROM ships t1 JOIN ship_types t2 ON t1.type=t2.id " +
				"WHERE t1.id>0 AND t1.owner=",user.getId()," GROUP BY t1.type,t2.nickname");
		while( tmp.next() ) {
			echo.append(Common.ln(tmp.getInt("count"))+" "+tmp.getString("nickname")+"<br />\n");
   			crew += tmp.getInt("sum");
		}
		tmp.free();

		long population = db.first("SELECT sum(bewohner) sum FROM bases WHERE owner=",user.getId()).getInt("sum");

		echo.append("</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\">Bev&ouml;lkerung:</td><td class=\"noBorderX\">"+Common.ln(crew+population)+"</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\">Bewohner:</td><td class=\"noBorderX\">"+Common.ln(population)+"</td></tr>\n");
		echo.append("<tr><td class=\"noBorderX\">Crew:</td><td class=\"noBorderX\">"+Common.ln(crew)+"</td></tr>\n");
		echo.append("</table><br /><br />\n");
	}
}
