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

import java.util.HashMap;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.Loggable;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.modules.StatsController;

/**
 * Zeigt die eigenen Offiziere und deren Aufenthaltsort
 * @author Christopher Jung
 *
 */
public class StatOwnOffiziere implements Statistic, Loggable {

	public void show(StatsController contr, int size) {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		Database db = context.getDatabase();

		StringBuffer echo = context.getResponse().getContent();
	
		SQLQuery offi = db.query("SELECT * ",
					"FROM offiziere ",
					"WHERE userid=",user.getId()," ",
					"ORDER BY ing+nav+waf+sec+com DESC");
					
		if( offi.numRows() == 0 ) {
			echo.append("<div align=\"center\">Sie verf&uuml;gen &uuml;ber keine Offiziere</div>\n");
			
			return;	
		}
					
		echo.append("<table class=\"noBorderX\" cellspacing=\"2\" cellpadding=\"3\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">Offizier</td><td class=\"noBorderX\">Auf</td><td class=\"noBorderX\">Technik</td><td class=\"noBorderX\">Navigation</td><td class=\"noBorderX\">Waffen</td><td class=\"noBorderX\">Sicherheit</td><td class=\"noBorderX\">Kommando</td><td class=\"noBorderX\">Spezial</td></tr>\n");
		
		Map<Integer,String> ships = new HashMap<Integer,String>();
		Map<Integer,String> bases = new HashMap<Integer,String>();
		
		while( offi.next() ) {
			Offizier offizier = new Offizier(offi.getRow());
		   	echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\"><img src=\""+offizier.getPicture()+"\" alt=\"Rang "+offizier.getRang()+"\" /> <a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "choff", "off", offizier.getID())+"\">"+Common._title(offizier.getName())+"</a> ("+offizier.getID()+")</td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");
	
			String[] dest = offizier.getDest();
			int destid = Integer.parseInt(dest[1]);
	
			if( dest[0].equals("s") ) {
				if( !ships.containsKey(destid) ) {
					if( destid > 0 ) {
						SQLResultRow ship = db.first("SELECT name FROM ships WHERE id>0 AND id=",destid);
						if( ship.isEmpty() ) {
							LOG.warn("Offizier '"+offizier.getID()+"' befindet sich auf einem ungueltigen Schiff: "+destid);
							ships.put(destid, "");
						}
						else {
							ships.put(destid, ship.getString("name"));
						}
					}
					else {
						ships.put(destid, "[Respawn "+destid+"]");
					}
				}
				String shipname = ships.get(destid);
				echo.append("<td class=\"noBorderX\"><a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "schiff", "ship", dest[1])+"\">"+shipname+"</a></td>\n");
			}
			else {
				if( !bases.containsKey(destid) ) {
					bases.put(destid, db.first("SELECT name FROM bases WHERE id=",destid).getString("name"));
				}
				String basename = bases.get(destid);
				echo.append("<td class=\"noBorderX\"><a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "base", "col", dest[1])+"\">"+basename+"</a> "+(dest[0].equals("t") ? "(A)" : "")+"</td>\n");
			}
			
			echo.append("<td class=\"noBorderX\">"+offizier.getAbility(Offizier.Ability.ING)+"</td>\n");
			echo.append("<td class=\"noBorderX\">"+offizier.getAbility(Offizier.Ability.NAV)+"</td>\n");
			echo.append("<td class=\"noBorderX\">"+offizier.getAbility(Offizier.Ability.WAF)+"</td>\n");
			echo.append("<td class=\"noBorderX\">"+offizier.getAbility(Offizier.Ability.SEC)+"</td>\n");
			echo.append("<td class=\"noBorderX\">"+offizier.getAbility(Offizier.Ability.COM)+"</td>\n");
			echo.append("<td class=\"noBorderX\">"+offizier.getSpecial().getName()+"</td>\n");
			echo.append("</tr>\n");
		}
		offi.free();
	
		echo.append("</table><div><br /><br /></div>\n");
	}

}
