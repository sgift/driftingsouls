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
 * Zeigt die Mitgliederanzahl der Allianzen an
 * @author Christopher Jung
 *
 */
public class StatMemberCount implements Statistic {

	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		StringBuffer echo = context.getResponse().getContent();
		
		String url = "./main.php?module=allylist&amp;sess="+context.getSession()+"&amp;action=details&amp;details=";
	
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" colspan=\"4\" align=\"left\">Die gr&ouml;&szlig;ten Allianzen:</td></tr>\n");
		
		int count = 0;
		
		SQLQuery tmp = db.query("SELECT a.id, a.name, count(u.id) membercount FROM ally a JOIN users u ON a.id=u.ally WHERE a.id>",StatsController.MIN_USER_ID," GROUP BY a.id ORDER BY membercount DESC LIMIT "+size);
		while( tmp.next() ) {
	   		echo.append("<tr><td class=\"noBorderX\" style=\"width:40px\">"+(count+1)+".</td>\n");
			echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+url+tmp.getInt("id")+"\">"+Common._title(tmp.getString("name"))+" ("+tmp.getInt("id")+")</td>");
			echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
			echo.append("<td class=\"noBorderX\">"+Common.ln(tmp.getInt("membercount"))+"</td></tr>\n");
		}
		tmp.free();
		echo.append("</table><br /><br />\n");
	}

}
