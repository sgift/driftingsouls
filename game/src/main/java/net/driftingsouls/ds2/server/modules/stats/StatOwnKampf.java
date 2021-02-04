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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.services.ShipService;
import net.driftingsouls.ds2.server.services.UserService;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Zeigt in Schlachten zerstoerte und verlorene Schiffe an.
 * @author Christopher Jung
 *
 */
@Service
public class StatOwnKampf implements Statistic {
	@PersistenceContext
	private EntityManager em;

	private final UserService userService;
	private final ShipService shipService;

	public StatOwnKampf(UserService userService, ShipService shipService) {
		this.userService = userService;
		this.shipService = shipService;
	}

	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();

		Writer echo = context.getResponse().getWriter();

		/////////////////////////////
		// Zerstoerte Schiffe
		/////////////////////////////

		int counter = 0;

		int destpos = context.getRequest().getParameterInt("destpos");

		int destcount = em.createQuery("SELECT count(distinct tick) FROM ShipLost WHERE destOwner=:user", Integer.class)
				.setParameter("user", user)
				.getSingleResult();
		if( destcount > 0 ) {
			if( destpos > destcount ) {
				destpos = destcount - 10;
			}

			if( destpos < 0 ) {
				destpos = 0;
			}

			echo.append("Zerst&ouml;rte Schiffe:<br />");
			echo.append("<table class=\"noBorderX\" cellpadding=\"3\" width=\"100%\">\n");
			List<Integer> t = em.createQuery("SELECT distinct tick FROM ShipLost WHERE destOwner=:user ORDER BY tick DESC", Integer.class)
					.setParameter("user", user)
					.setMaxResults(10)
					.setFirstResult(destpos)
					.getResultList();
			for( int tick: t )
			{
				List<Object[]> s = em.createQuery("SELECT distinct count(*),type,owner FROM ShipLost WHERE destOwner=:user AND tick=:tick GROUP BY type,owner", Object[].class)
						.setParameter("user", user)
						.setParameter("tick", tick)
						.getResultList();

				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;


				echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; text-align:center\">");
				echo.append(Common.getIngameTime(tick)).append("<br />");

				for( Object[] data : s )
				{
					ShipTypeData shiptype = shipService.getShipType( (Integer)data[1] );

					long count = (Long)data[0];

					echo.append(Long.toString(count)).append(" ");
					if( shiptype != null ) {
						echo.append("<a target=\"_blank\" onclick='ShiptypeBox.show(").append(Integer.toString(shiptype.getTypeId())).append(");return false;' ").append("href=\"./ds?module=schiffinfo&ship=").append(Integer.toString(shiptype.getTypeId())).append("\">").append(shiptype.getNickname()).append("</a>");
					}
					else
					{
						echo.append(data[1].toString());
					}

					User auser = em.find(User.class, data[2]);
					if( auser != null ) {
						echo.append(" von: ").append(userService.getProfileLink(auser)).append("<br />");
					}
					else {
						echo.append(" von: Unbekannter Spieler (").append(String.valueOf(data[2])).append(")<br />");
					}
				}
				echo.append("</td>\n");

				if( (counter % 5) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
			}

			while( counter % 5 != 0 ) {
				echo.append("<td class=\"noBorderX\">&nbsp;</td>");
				counter++;
			}

			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( destpos-10 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "show", 4, "destpos", destpos - 10)).append("\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">\n");
			if( destpos+10 < destcount ) {
				echo.append("<a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "show", 4, "destpos", destpos + 10)).append("\">vor</a>\n");
			}
			else {
				echo.append("vor");
			}
			echo.append("</td></tr>\n");
			echo.append("</table>\n");
		}

		/////////////////////////////
		// Verlorene Schiffe
		/////////////////////////////

		counter = 0;

		int lostpos = context.getRequest().getParameterInt("lostpos");

		int lostcount = em.createQuery("SELECT count(distinct tick) FROM ShipLost WHERE owner=:user", Integer.class)
				.setParameter("user", user)
				.getSingleResult();
		if( lostcount > 0 ) {
			if( lostpos > lostcount ) {
				lostpos = lostcount - 10;
			}

			if( lostpos < 0 ) {
				lostpos = 0;
			}

			if( destcount > 0 ) {
				echo.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />\n");
			}

			echo.append("<br />Verlorene Schiffe:<br />");
			echo.append("<table class=\"noBorderX\" cellpadding=\"3\" width=\"100%\">\n");
			List<Integer> t = em.createQuery("SELECT distinct tick FROM ShipLost WHERE owner=:user ORDER BY tick DESC", Integer.class)
					.setParameter("user", user)
					.setMaxResults(10)
					.setFirstResult(lostpos)
					.getResultList();
			for( int tick: t )
			{
				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;

				echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; text-align:center\">");
				echo.append(Common.getIngameTime(tick)).append("<br />");


				List<Object[]> s = em.createQuery("SELECT distinct count(*),type,destOwner FROM ShipLost WHERE owner=:user AND tick=:tick GROUP BY type,destOwner", Object[].class)
						.setParameter("user", user)
						.setParameter("tick", tick)
						.getResultList();

				for( Object[] data : s )
				{
					ShipTypeData shiptype = shipService.getShipType( (Integer)data[1] );

					long count = (Long)data[0];

					echo.append(Long.toString(count)).append(" ");
					if( shiptype != null ) {
						echo.append("<a target=\"_blank\" onclick='ShiptypeBox.show(").append(Integer.toString(shiptype.getTypeId())).append(");return false;' ").append("href=\"./ds?module=schiffinfo&ship=").append(Integer.toString(shiptype.getTypeId())).append("\">").append(shiptype.getNickname()).append("</a>");
					}
					else
					{
						echo.append(data[1].toString());
					}

					User auser = em.find(User.class, data[2]);

					if( auser != null ) {
						echo.append(" durch: ").append(userService.getProfileLink(auser)).append("<br />");
					}
					else {
						echo.append(" durch: Unbekannter Spieler (").append(String.valueOf(data[2])).append(")<br />");
					}
				}
				echo.append("</td>\n");

				if( (counter % 5) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
			}

			while( counter % 5 != 0 ) {
				echo.append("<td class=\"noBorderX\">&nbsp;</td>");
				counter++;
			}
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( lostpos-10 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "show", 4, "lostpos", lostpos - 10)).append("\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">");
			if( lostpos+10 < lostcount ) {
				echo.append("<a class=\"forschinfo\" href=\"").append(Common.buildUrl("default", "show", 4, "lostpos", lostpos + 10)).append("\">vor</a>\n");
			}
			else {
				echo.append("vor");
			}
			echo.append("</td></tr>\n");
			echo.append("</table>\n");
		}

		if( (destcount == 0) && (lostcount == 0) ) {
			echo.append("<div align=\"center\">Sie haben weder Schiffe zerst&ouml;rt noch Schiffe verloren</div>\n");
		}
	}
}
