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

import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ship;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

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

		List<?> offiziere = db.createQuery("from Offizier where owner=:user order by ing+nav+waf+sec+com desc")
			.setInteger("user", user.getId())
			.list();

		if( offiziere.size() == 0 ) {
			echo.append("<div align=\"center\">Sie verf&uuml;gen &uuml;ber keine Offiziere</div>\n");

			return;
		}

		echo.append("<table class=\"noBorderX\" cellspacing=\"2\" cellpadding=\"3\">\n");
		echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">Offizier</td><td class=\"noBorderX\">Auf</td><td class=\"noBorderX\">Technik</td><td class=\"noBorderX\">Navigation</td><td class=\"noBorderX\">Waffen</td><td class=\"noBorderX\">Sicherheit</td><td class=\"noBorderX\">Kommando</td><td class=\"noBorderX\">Spezial</td></tr>\n");

		for (Object anOffiziere : offiziere)
		{
			Offizier offizier = (Offizier) anOffiziere;

			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\"><img src=\"").append(offizier.getPicture()).append("\" alt=\"Rang ").append(Integer.toString(offizier.getRang())).append("\" /> <a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "module", "choff", "off", offizier.getID())).append("\">").append(Common._title(offizier.getName())).append("</a> (").append(Integer.toString(offizier.getID())).append(")</td>\n");
			echo.append("<td class=\"noBorderX\">&nbsp;</td>\n");

			if (offizier.getStationiertAufSchiff() != null)
			{
				Ship ship = offizier.getStationiertAufSchiff();
				String shipname = ship.getName();
				if (ship.getId() < 0)
				{
					shipname = "[Respawn " + ship.getId() + "]";
				}

				echo.append("<td class=\"noBorderX\"><a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "module", "schiff", "ship", ship.getId())).append("\">").append(shipname).append("</a>").append(offizier.isTraining() ? "(A)" : "").append("</td>\n");
			}
			else if (offizier.getStationiertAufBasis() != null)
			{
				String basename = offizier.getStationiertAufBasis().getName();
				echo.append("<td class=\"noBorderX\"><a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "module", "base", "col", offizier.getStationiertAufBasis().getId())).append("\">").append(basename).append("</a> ").append(offizier.isTraining() ? "(A)" : "").append("</td>\n");
			}
			else
			{
				echo.append("<td class=\"noBorderX\">desertiert</td>\n");
			}

			echo.append("<td class=\"noBorderX\">").append(Integer.toString(offizier.getAbility(Offizier.Ability.ING))).append("</td>\n");
			echo.append("<td class=\"noBorderX\">").append(Integer.toString(offizier.getAbility(Offizier.Ability.NAV))).append("</td>\n");
			echo.append("<td class=\"noBorderX\">").append(Integer.toString(offizier.getAbility(Offizier.Ability.WAF))).append("</td>\n");
			echo.append("<td class=\"noBorderX\">").append(Integer.toString(offizier.getAbility(Offizier.Ability.SEC))).append("</td>\n");
			echo.append("<td class=\"noBorderX\">").append(Integer.toString(offizier.getAbility(Offizier.Ability.COM))).append("</td>\n");
			echo.append("<td class=\"noBorderX\">").append(offizier.getSpecial().getName()).append("</td>\n");
			echo.append("</tr>\n");
		}

		echo.append("</table><div><br /><br /></div>\n");
	}
}
