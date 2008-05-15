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
import net.driftingsouls.ds2.server.framework.db.SQLQuery;

abstract class AbstractStatistic implements Statistic {
	private Context context = null;
	
	protected AbstractStatistic() {
		context = ContextMap.getContext();
	}
	
	final protected Context getContext() {
		return context;
	}
	
	final protected String getUserURL() {
		return "./ds?module=userprofile&amp;sess="+context.getSession()+"&amp;user=";
	}
	
	final protected String getAllyURL() {
		return "./ds?module=allylist&amp;sess="+context.getSession()+"&amp;action=details&amp;details=";
	}
	
	/**
	 * Generiert eine Statistik mit Platz, Namen und Anzahl
	 * @param name Der Name der Statistik
	 * @param tmp Ein SQL-Ergebnis mit den Feldern (Spieler/Ally) "id", (Spieler/Ally) "name" und "count", welches den Plaetzen nach sortiert ist (1. Platz zuerst)
	 * @param url Die fuer Links
	 */
	final protected void generateStatistic(String name, SQLQuery tmp, String url) {
		generateStatistic(name, tmp, url, true);
	}
	
	/**
	 * Generiert eine Statistik mit Platz, Namen und Anzahl
	 * @param name Der Name der Statistik
	 * @param tmp Ein SQL-Ergebnis mit den Feldern (Spieler/Ally) "id", (Spieler/Ally) "name" und "count", welches den Plaetzen nach sortiert ist (1. Platz zuerst)
	 * @param url Die fuer Links
	 * @param showCount Soll die Spalte "count" angezeigt werden?
	 */
	final protected void generateStatistic(String name, SQLQuery tmp, String url, boolean showCount) {
		StringBuffer echo = getContext().getResponse().getContent();
		
		echo.append("<table class=\"noBorderX\" cellspacing=\"1\" cellpadding=\"1\" width=\"100%\">\n");
		echo.append("<tr><td class=\"noBorderX\" colspan=\"4\" align=\"left\">"+name+"</td></tr>\n");
	
		int count = 0;
		while( tmp.next() ) {
	   		echo.append("<tr><td class=\"noBorderX\" style=\"width:40px\">"+(count+1)+".</td>\n");
			echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\""+url+tmp.getInt("id")+"\">"+Common._title(tmp.getString("name"))+" ("+tmp.getInt("id")+")</a></td>\n");
			if( showCount ) {
				echo.append("<td class=\"noBorderX\">&nbsp;-&nbsp;</td>\n");
				echo.append("<td class=\"noBorderX\">"+Common.ln(tmp.getInt("count"))+"</td></tr>\n");
			}
	   		
	   		count++;
		}
		
		echo.append("</table><br /><br />\n");
	}
}
