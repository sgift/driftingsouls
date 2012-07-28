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

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt an, wie oft ein Schiff in DS vorkommt.
 * @author Christopher Jung
 *
 */
public class StatShips implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		Writer echo = context.getResponse().getWriter();

		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Schiffe:</td></tr>\n");
		SQLQuery tmp = db.query("SELECT st.id,st.nickname,count(*) count " +
				"FROM ship_types st JOIN ships s ON s.type=st.id " +
				"WHERE s.owner>",StatsController.MIN_USER_ID," " +
				"GROUP BY st.id,st.nickname " +
				"ORDER BY st.nickname");
		while( tmp.next() ) {
      		echo.append("<tr><td class=\"noBorderX\">"+
      				Common.ln(tmp.getInt("count"))+" "+tmp.getString("nickname")+
      				" <a class=\"forschinfo\" onclick='ShiptypeBox.show("+tmp.getInt("id")+");return false;' href=\"./ds?module=schiffinfo&ship="+tmp.getInt("id")+"\">(?)</a>" +
      			"</td></tr>\n");
		}
		tmp.free();
		echo.append("</table><br /><br />");
	}
}
