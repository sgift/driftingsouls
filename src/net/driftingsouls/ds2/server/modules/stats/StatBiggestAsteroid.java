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
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die Liste der groessten Asteroiden an
 * @author Christopher Jung
 *
 */
public class StatBiggestAsteroid extends AbstractStatistic implements Statistic {
	/**
	 * Konstruktor
	 * 
	 */
	public StatBiggestAsteroid() {
		// EMPTY
	}
	
	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		SQLQuery tmp = db.query("SELECT b.id,b.owner,b.name astiname,b.bewohner count,u.name " +
				"FROM bases b JOIN users u ON b.owner=u.id " +
				"WHERE b.owner>"+StatsController.MIN_USER_ID+" " +
				"ORDER BY count DESC LIMIT "+size);
			
		String url = getUserURL();
	
		StringBuffer echo = getContext().getResponse().getContent();
		
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" colspan=\"6\" align=\"left\">Die gr&ouml;&szlig;ten Asteroiden:</td></tr>\n");
	
		int count = 0;
		while( tmp.next() ) {
	   		echo.append("<tr><td class=\"noBorderX\" style=\"width:40px\">"+(count+1)+".</td>\n");
			echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+url+tmp.getInt("id")+"\">"+Common._title(tmp.getString("name"))+" ("+tmp.getInt("id")+")</a></td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
			echo.append("<td class=\"noBorderX\">"+tmp.getString("astiname")+" ("+tmp.getInt("id")+")</td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
			echo.append("<td class=\"noBorderX\">"+Common.ln(tmp.getInt("count"))+"</td></tr>\n");
	   		
	   		count++;
		}
		
		echo.append("</table><br /><br />\n");
		
		tmp.free();
	}
}
