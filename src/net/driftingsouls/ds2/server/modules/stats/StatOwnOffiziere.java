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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ship;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Zeigt die eigenen Offiziere und deren Aufenthaltsort.
 * @author Christopher Jung
 *
 */
public class StatOwnOffiziere implements Statistic {
	private static final Log log = LogFactory.getLog(StatOwnOffiziere.class);

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		org.hibernate.Session db = context.getDB();
		Writer echo = context.getResponse().getWriter();

		List<?> offiziere = db.createQuery("from Offizier where userid=:user order by ing+nav+waf+sec+com desc")
			.setInteger("user", user.getId())
			.list();

		if( offiziere.size() == 0 ) {
			echo.append("<div align=\"center\">Sie verf&uuml;gen &uuml;ber keine Offiziere</div>\n");

			return;
		}

		echo.append("<table class=\"noBorderX\" cellspacing=\"2\" cellpadding=\"3\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">Offizier</td><td class=\"noBorderX\">Auf</td><td class=\"noBorderX\">Technik</td><td class=\"noBorderX\">Navigation</td><td class=\"noBorderX\">Waffen</td><td class=\"noBorderX\">Sicherheit</td><td class=\"noBorderX\">Kommando</td><td class=\"noBorderX\">Spezial</td></tr>\n");

		Map<Integer,String> ships = new HashMap<Integer,String>();
		Map<Integer,String> bases = new HashMap<Integer,String>();

		for( Iterator<?> iter=offiziere.iterator(); iter.hasNext(); ) {
			Offizier offizier = (Offizier)iter.next();

		   	echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\"><img src=\""+offizier.getPicture()+"\" alt=\"Rang "+offizier.getRang()+"\" /> <a class=\"forschinfo\" href=\""+Common.buildUrl("default", "module", "choff", "off", offizier.getID())+"\">"+Common._title(offizier.getName())+"</a> ("+offizier.getID()+")</td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");

			String[] dest = offizier.getDest();
			int destid = Integer.parseInt(dest[1]);

			if( dest[0].equals("s") ) {
				if( !ships.containsKey(destid) ) {
					if( destid > 0 ) {
						Ship ship = (Ship)db.get(Ship.class, destid);
						if( ship == null || ship.getId() <= 0 ) {
							log.warn("Offizier '"+offizier.getID()+"' befindet sich auf einem ungueltigen Schiff: "+destid);
							ships.put(destid, "");
						}
						else {
							ships.put(destid, ship.getName());
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
					Base base = (Base)db.get(Base.class, destid);
					if( base != null )
					{
						bases.put(destid, base.getName());
					}
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

		echo.append("</table><div><br /><br /></div>\n");
	}
}
