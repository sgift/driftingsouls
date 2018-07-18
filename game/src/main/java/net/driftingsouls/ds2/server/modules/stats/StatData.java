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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;

/**
 * Zeigt allgemeine Daten zu DS und zum Server an.
 * @author Christopher Jung
 *
 */
public class StatData implements Statistic {
	private static final Log log = LogFactory.getLog(StatData.class);

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		Writer echo = context.getResponse().getWriter();

		echo.append("<h1>Diverse Daten:</h1>");
		echo.append("<table cellspacing=\"1\" cellpadding=\"3\">\n");

		echo.append("<tr><td align=\"left\">Einnahmen aus Versteigerungen</td><td class=\"noBorderX\"></td></tr>\n");

		echo.append("<tr><td align=\"left\">&nbsp;&nbsp;&nbsp;GTU</td>\n");
		Object[] einnahmen = (Object[])db
				.createQuery("SELECT sum(ceil(preis*gtuGew/100)),sum(preis) FROM StatGtu")
				.uniqueResult();
		echo.append("<td align=\"left\">").append(Common.ln((Long) einnahmen[0])).append(" RE</td></tr>\n");

		echo.append("<tr><td align=\"left\">&nbsp;&nbsp;&nbsp;Spieler</td>\n");
		echo.append("<td align=\"left\">").append(Common.ln((Long) einnahmen[1] - (Long) einnahmen[0])).append(" RE</td></tr>\n");

		echo.append("<tr><td align=\"left\">Gesamtverm&ouml;gen aller Spieler:</td>\n");
		BigInteger totalre = (BigInteger)db.createQuery("SELECT sum(konto) FROM User WHERE id > 0").uniqueResult();
		echo.append("<td align=\"left\">").append(Common.ln(totalre)).append(" RE</td></tr>\n");

		echo.append("<tr><td colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");

		Object[] shipstats = (Object[])db
				.createQuery("SELECT shipCount,crewCount FROM StatShips ORDER BY tick DESC")
				.setMaxResults(1)
				.uniqueResult();
		echo.append("<tr><td align=\"left\">Schiffe in Spielerhand:</td>\n");
		echo.append("<td align=\"left\">").append(Common.ln((Long) shipstats[0])).append("</td></tr>\n");

		echo.append("<tr><td align=\"left\">Crew auf diesen Schiffen:</td>\n");
		echo.append("<td align=\"left\">").append(Common.ln((Long) shipstats[1])).append("</td></tr>\n");

		echo.append("<tr><td colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");

		echo.append("<tr><td align=\"left\">Registrierte Spieler:</td>\n");
		long usercount = (Long)db
				.createQuery("SELECT count(*) FROM User WHERE id>:minid")
				.setInteger("minid",StatsController.MIN_USER_ID)
				.uniqueResult();
		echo.append("<td align=\"left\">").append(Common.ln(usercount)).append("</td></tr>\n");

		echo.append("<tr><td align=\"left\">PMs in der Datenbank:</td>\n");
		long pmcount = (Long)db.createQuery("SELECT count(*) FROM PM").uniqueResult();
		echo.append("<td align=\"left\">").append(Common.ln(pmcount)).append("</td></tr>\n");

		echo.append("<tr><td colspan=\"2\"><hr noshade=\"noshade\" size=\"1\" style=\"color:#cccccc\" /></td></tr>\n");

		if( new File("/proc/uptime").canRead() ) {
			try {
				try (BufferedReader f = new BufferedReader(new FileReader("/proc/uptime")))
				{
					String line = f.readLine();
					if (line != null && !line.equals(""))
					{
						String[] uptime = line.split(" ");

						double uptime_sec = Double.parseDouble(uptime[0]);
						long uptime_days = (long) (uptime_sec / 86400);
						uptime_sec -= uptime_days * 86400;

						long uptime_hours = (long) (uptime_sec / 3600);
						uptime_sec -= uptime_hours * 3600;

						long uptime_min = (long) (uptime_sec / 60);
						uptime_sec -= uptime_min * 60;

						echo.append("<tr><td align=\"left\">Uptime des Servers:</td>\n");
						echo.append("<td align=\"left\">").append(Long.toString(uptime_days)).append(" Tage ").append(Long.toString(uptime_hours)).append(" Stunden ").append(Long.toString(uptime_min)).append(" Minuten</td></tr>\n");
					}
				}
			}
			catch( IOException e ) {
				log.warn(e,e);
			}
		}

		if( new File("/proc/loadavg").canRead() ) {
			try {
				try (BufferedReader f = new BufferedReader(new FileReader("/proc/loadavg")))
				{
					String line = f.readLine();
					if (line != null && !line.equals(""))
					{
						String[] load = line.split(" ");

						echo.append("<tr><td align=\"left\">Auslastung:</td>\n");
						echo.append("<td align=\"left\">").append(load[0]).append(" ").append(load[1]).append(" ").append(load[2]).append("</td></tr>\n");
					}
				}
			}
			catch( IOException e ) {
				log.warn(e,e);
			}
		}

		echo.append("</table><br /><br />");

	}
}
