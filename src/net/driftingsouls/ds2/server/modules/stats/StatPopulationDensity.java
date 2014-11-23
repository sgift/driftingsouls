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

import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

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
		org.hibernate.Session db = context.getDB();
		User user = (User)context.getActiveUser();
		org.hibernate.Session database = context.getDB();

		Writer echo = context.getResponse().getWriter();

		echo.append("<h1>Siedlungsdichte:</h1>");
		echo.append("<table cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<thead><tr><th>System</th><th>Asteroiden</th><th>%</th><th>Bewohner</th><th>Bewohner / Asteroid</th><th>Bewohner / besiedelter Asteroid</th></tr></thead>\n");
		echo.append("<tbody>");

		long insbew=0;
		int instotal = 0;
		int insused = 0;

		List<?> systemStats = db.createQuery("SELECT system, count(*), " +
					"sum(CASE WHEN owner.id!=0 THEN bewohner ELSE 0 END), " +
					"sum(CASE WHEN owner.id!=0 THEN 1 ELSE 0 END) " +
					"FROM Base b " +
					"GROUP BY b.system ORDER BY b.system")
				.list();

		for( Object o : systemStats )
		{
			Object[] data = (Object[])o;
			String systemAddInfo = "";
			StarSystem system = (StarSystem)database.get(StarSystem.class, (Integer)data[0]);
			if( system == null ) {
				log.warn("Asteroiden im ungueltigen System "+data[0]+" vorhanden");
				continue;
			}
			if( !system.isVisibleFor(user) )
			{
				continue;
			}
			if( system.getAccess() == StarSystem.Access.ADMIN ) {
				systemAddInfo = "<span style=\"font-style:italic\">[admin]</span>";
			}
			else if( system.getAccess() == StarSystem.Access.NPC ) {
				systemAddInfo = "<span style=\"font-style:italic\">[npc]</span>";
			}

			if( systemAddInfo.length() > 0 ) {
				systemAddInfo += " ";
			}

			long bew = (Long)data[2];
			long used = (Long)data[3];
			long total = (Long)data[1];

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

			echo.append("<tr><td style=\"vertical-align:top\">").append(systemAddInfo).append(system.getName()).append(" (").append(Integer.toString(system.getID())).append(")</td>\n");

			echo.append("<td>").append(Common.ln(used)).append("/").append(Common.ln(total)).append("</td>").append("<td>").append(Common.ln(percentUsed)).append(" %</td>").append("<td>").append(Common.ln(bew)).append("</td>").append("<td>").append(Common.ln(bewPerAsti)).append("</td>").append("<td>").append(Common.ln(bewPerUsedAsti)).append("</td>\n");
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
		echo.append("<tr><td colspan=\"6\">" +
				"<hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");

		echo.append("<tr><td style=\"vertical-align:top\">Insgesamt</td>" + "<td>").append(Common.ln(insused)).append("/").append(Common.ln(instotal)).append("</td>").append("<td>").append(Common.ln(percentUsed)).append(" %</td>").append("<td>").append(Common.ln(insbew)).append("</td>").append("<td>").append(Common.ln(bewPerAsti)).append("</td>").append("<td>").append(Common.ln(bewPerUsedAsti)).append("</td>").append("</tr>\n");

		Long crew = (Long)db
				.createQuery("SELECT sum(crew) FROM Ship WHERE id>0 AND owner.id>:minid")
				.setInteger("minid",StatsController.MIN_USER_ID)
				.uniqueResult();

		crew = crew == null ? 0L : crew;

		long tw = crew + insbew;

		echo.append("<tr><td colspan=\"6\">" + "+ ").append(Common.ln(crew)).append(" Crew auf Schiffen = ").append(Common.ln(tw)).append(" Bev&ouml;lkerung insgesamt").append("</td></tr>\n");
		echo.append("</tbody></table><br /><br />");
	}
}
