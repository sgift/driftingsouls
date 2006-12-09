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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an
 * @author Christopher Jung
 *
 */
public class StatData implements Statistic, Loggable {

	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		Database db = context.getDatabase();

		StringBuffer echo = context.getResponse().getContent();
	
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"3\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">Diverse Daten:</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Einnahmen aus Versteigerungen</td><td class=\"noBorderX\"></td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">&nbsp;&nbsp;&nbsp;GTU</td>\n");
		SQLResultRow einnahmen = db.first("SELECT sum(ceil(preis*gtugew/100)) gtu,sum(preis) total FROM stats_gtu");
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(einnahmen.getLong("gtu"))+" RE</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">&nbsp;&nbsp;&nbsp;Spieler</td>\n");
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(einnahmen.getLong("total")-einnahmen.getLong("gtu"))+" RE</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Gesamtverm&ouml;gen aller Spieler:</td>\n");	
		BigInteger totalre = db.first("SELECT sum(konto) sum FROM users WHERE id > 0").getBigInteger("sum");
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(totalre)+" RE</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");
		
		SQLResultRow shipstats = db.first("SELECT shipcount,crewcount FROM stats_ships ORDER BY tick DESC");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Schiffe in Spielerhand:</td>\n");	
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(shipstats.getLong("shipcount"))+"</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Crew auf diesen Schiffen:</td>\n");	
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(shipstats.getBigInteger("crewcount"))+"</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">Registrierte Spieler:</td>\n");
		int usercount = db.first("SELECT count(*) count FROM users WHERE id>",StatsController.MIN_USER_ID).getInt("count");
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(usercount)+"</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" align=\"left\">PMs in der Datenbank:</td>\n");
		long pmcount = db.first("SELECT count(*) count FROM transmissionen").getLong("count");
		echo.append("<td class=\"noBorderX\" align=\"left\">"+Common.ln(pmcount)+"</td></tr>\n");
		
		echo.append("<tr><td class=\"noBorderX\" colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");
		
		if( new File("/proc/uptime").canRead() ) {
			try {
				BufferedReader f = new BufferedReader(new FileReader("/proc/uptime"));
				String[] uptime = f.readLine().split(" ");
				f.close();
				
				double uptime_sec = Double.parseDouble(uptime[0]);
				long uptime_days = (long)(uptime_sec / 86400);
				uptime_sec -= uptime_days*86400;
				
				long uptime_hours = (long)(uptime_sec / 3600);
				uptime_sec -= uptime_hours*3600;
				
				long uptime_min = (long)(uptime_sec / 60);
				uptime_sec -= uptime_min*60;
				
				echo.append("<tr><td class=\"noBorderX\" align=\"left\">Uptime des Servers:</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"left\">"+uptime_days+" Tage "+uptime_hours+" Stunden "+uptime_min+" Minuten</td></tr>\n");
			}
			catch( IOException e ) {
				LOG.warn(e,e);
			}
		}
		
		if( new File("/proc/loadavg").canRead() ) {
			try {
				BufferedReader f = new BufferedReader(new FileReader("/proc/loadavg"));
				String[] load = f.readLine().split(" ");
				f.close();
			
				echo.append("<tr><td class=\"noBorderX\" align=\"left\">Auslastung:</td>\n");
				echo.append("<td class=\"noBorderX\" align=\"left\">"+load[0]+" "+load[1]+" "+load[2]+"</td></tr>\n");
			}
			catch( IOException e ) {
				LOG.warn(e,e);
			}
		}
		
		echo.append("</table><br /><br />");

	}

}
