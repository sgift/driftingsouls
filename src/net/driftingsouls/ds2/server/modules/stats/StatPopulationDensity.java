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

import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Zeigt die Bevoelkerungsdichte in einzelnen Systemen sowie insgesamt an.
 * @author Christopher Jung
 *
 */
public class StatPopulationDensity implements Statistic {
	private static final Log log = LogFactory.getLog(StatPopulationDensity.class);

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();
		User user = (User)context.getActiveUser();
		org.hibernate.Session database = context.getDB();

		Writer echo = context.getResponse().getWriter();
	
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">Besiedlungsdichte:</td></tr>\n");
	
		long insbew=0;
		int instotal = 0;
		int insused = 0;
		
		SQLQuery systemStats = db.query("SELECT system, count(*) total, " +
				"sum(CASE WHEN owner!=0 THEN bewohner ELSE 0 END) bew, " +
				"sum(CASE WHEN owner!=0 THEN 1 ELSE 0 END) used " +
				"FROM bases " +
				"GROUP BY system ORDER BY system");
	
		while( systemStats.next() ) {
			String systemAddInfo = "";
			StarSystem system = (StarSystem)database.get(StarSystem.class, systemStats.getInt("system"));
			if( system == null ) {
				log.warn("Asteroiden im ungueltigen System "+systemStats.getInt("system")+" vorhanden");
				continue;
			}
			if( (system.getAccess() == StarSystem.AC_ADMIN) && user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS) ) {
				systemAddInfo = "<span style=\"font-style:italic\">[admin]</span>";
			}
			else if( (system.getAccess() == StarSystem.AC_NPC) && (user.hasFlag(User.FLAG_VIEW_SYSTEMS) || user.hasFlag(User.FLAG_VIEW_ALL_SYSTEMS) ) ) {
				systemAddInfo = "<span style=\"font-style:italic\">[hidden]</span>";		
			} 
			else if( (system.getAccess() == StarSystem.AC_ADMIN) || (system.getAccess() == StarSystem.AC_NPC) ) {
				continue;
			}
			if( systemAddInfo.length() > 0 ) {
				systemAddInfo += " ";
			}
		
			int total = 0;
			int used = 0;
			long bew = 0;
	
			bew = systemStats.getLong("bew");
			used = systemStats.getInt("used");
			total = systemStats.getInt("total");
			
			insbew += bew;
	   		instotal += total;
	   		insused += used;
	
			int percentUsed = 0;
			if( total > 0 ) {
				percentUsed = (int)(used*100d/total);
			}
			
			long bewPerAsti = 0;
			if( total > 0 ) {
				bewPerAsti = (long)(bew/(double)total);
			}
					
			long bewPerUsedAsti = 0;
			if( used > 0 ) {
				bewPerUsedAsti = (long)(bew/(double)used);
			}
			
			echo.append("<tr><td class=\"noBorderX\" style=\"vertical-align:top\">"+
					systemAddInfo+system.getName()+" ("+system.getID()+")</td>\n");
			
			echo.append("<td class=\"noBorderX\">" +
					"<span class=\"nobr\">"+Common.ln(used)+"/"+Common.ln(total)+" - "+Common.ln(percentUsed)+" %  besiedelt; </span>" +
					"<span class=\"nobr\">"+Common.ln(bew)+" Bewohner;</span>" +
					"<span class=\"nobr\"> "+Common.ln(bewPerAsti)+" / Asteroid; </span>" +
					"<span class=\"nobr\">"+Common.ln(bewPerUsedAsti)+" / besiedelter Asteroid</span>" +
					"<br /><br /></td>\n");
			echo.append("</tr>");
		}
		
		int percentUsed = 0;
		long bewPerAsti = 0;
		long bewPerUsedAsti = 0;
		
		if( instotal > 0 ) {
			percentUsed = (int)(insused*100d/instotal);
			bewPerAsti = (long)(insbew/(double)instotal);
		}
		if( insused > 0 ) {
			bewPerUsedAsti = (long)(insbew/(double)insused);
		}
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\">" +
				"<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" style=\"vertical-align:top\">Insgesamt</td>" +
				"<td class=\"noBorderX\">&nbsp;" +
				"<span class=\"nobr\">"+Common.ln(insused)+"/"+Common.ln(instotal)+" - "+Common.ln(percentUsed)+" %  besiedelt; </span>" +
				"<span class=\"nobr\">"+Common.ln(insbew)+" Bewohner; </span>" +
				"<span class=\"nobr\">"+Common.ln(bewPerAsti)+" / Asteroid; </span>" +
				"<span class=\"nobr\">"+Common.ln(bewPerUsedAsti)+" / besiedelter Asteroid</span>" +
				"<br /><br /></td></tr>\n");
	
		long crew = db.first("SELECT sum(crew) sum FROM ships WHERE id>0 AND owner>"+StatsController.MIN_USER_ID).getLong("sum");
		long tw = crew + insbew;
	
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\">" +
				"+ "+Common.ln(crew)+" Crew auf Schiffen = "+Common.ln(tw)+" Bev&ouml;lkerung insgesamt" +
				"</td></tr>\n");
		echo.append("</table><br /><br />");
	}

	@Override
	public boolean generateAllyData() {
		return false;
	}
	
	@Override
	public int getRequiredData() {
		return 0;
	}
}
