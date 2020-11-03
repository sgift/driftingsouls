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
package net.driftingsouls.ds2.server.uilibs;

import net.driftingsouls.ds2.server.WellKnownPermission;
import net.driftingsouls.ds2.server.config.Rassen;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.entities.UserFlag;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;

/**
 * Die Spielerliste.
 * @author Christopher Jung
 *
 */
public class PlayerList {
	/**
	 * Gibt die Spielerliste im angegebenen Kontext aus.
	 * @param context Der Kontext
	 * @throws IOException
	 */
	public void draw(Context context) throws IOException {
		String ord = context.getRequest().getParameter("ord");

		int comPopup = context.getRequest().getParameter("compopup") != null ?
				Integer.parseInt(context.getRequest().getParameter("compopup")) :
				0;

		User user = (User)context.getActiveUser();
		org.hibernate.Session db = context.getDB();

		String show = "";
		if( context.getRequest().getParameter("show") != null ) {
			show = "&show="+context.getRequest().getParameter("show");
		}

		String url = "./ds";
		if( context.getRequest().getParameter("module") != null ) {
			url += "?module="+context.getRequest().getParameter("module");
		}

		if( context.getRequest().getParameter("action") != null ) {
			url += "&action="+context.getRequest().getParameter("action");
		}
		url += show;

		Writer echo = context.getResponse().getWriter();
		echo.append("<table class=\"noBorderX\" cellpadding=\"2\" cellspacing=\"2\" width=\"100%\">\n");

		if( comPopup == 0 ) {
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=id\">ID</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=id\">Name</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=race\">Rasse</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=signup\">Dabei seit</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=ally\">Allianz</b></a></td>");
		}
		// Sollen wir nen Popup sein?
		else {
			echo.append("<tr>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=id\">ID</a></td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=id\">Name</a></td>\n");
		}

		// Ein Admin bekommt mehr zu sehen...
		if( (comPopup == 0) && (user != null) && context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) ) {
			echo.append("<td class=\"noBorderX\" align=\"center\"><a class=\"forschinfo\" href=\"").append(url).append("&ord=inakt\">Inaktiv</a></td>");
			echo.append("<td class=\"noBorderX\" align=\"center\">Astis</td>\n");
			echo.append("<td class=\"noBorderX\" align=\"center\">Schiffe</td>\n");
		}
		echo.append("</tr>\n");

		HashMap<Integer,Integer> asticount = null;
		HashMap<Integer,Integer> shipcount = null;

		String query;
		if( (user == null) || !context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) ) {
			query = "select u from User u left join fetch u.ally a where locate('hide',u.flags)=0 order by ";
		}
		else {
			// Asteroiden/Schiffe zaehlen
			asticount = new HashMap<>();
			shipcount = new HashMap<>();

			List<?> basecounts = db.createQuery("select owner.id,count(*) from Base group by owner.id").list();
			for (Object basecount : basecounts)
			{
				final Object[] data = (Object[]) basecount;
				final Integer owner = (Integer) data[0];
				final Number count = (Number) data[1];
				asticount.put(owner, count.intValue());
			}

			List<?> shipcounts = db.createQuery("select owner.id,count(*) from Ship group by owner.id").list();
			for (Object shipcount1 : shipcounts)
			{
				final Object[] data = (Object[]) shipcount1;
				final Integer owner = (Integer) data[0];
				final Number count = (Number) data[1];
				shipcount.put(owner, count.intValue());
			}

			query = "select u from User u left join fetch u.ally a order by ";
		}

		if( (ord == null) || "".equals(ord) ) {
			query += "u.id";
		}
		else if( "id".equals(ord) || "name".equals("ord") || "race".equals(ord) || "signup".equals(ord) || "ally".equals(ord) ) {
			query += "u."+ord;
		}
		else if( "inakt".equals(ord) && (user != null) && context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) ) {
			query += "u.inakt";
		}
		else {
			query += "u.id";
		}

		User.Relations relationlist = null;
		if( user != null ) {
			relationlist = user.getRelations();
		}

		List<User> userlist = Common.cast(db.createQuery(query).list());
		for( User aUser : userlist ) {
			String race = "???";
			if( Rassen.get().rasse(aUser.getRace()) != null ) {
				race = Rassen.get().rasse(aUser.getRace()).getName();
			}

			String ally = "&nbsp;";
			if( aUser.getAlly() != null ) {
				if( user != null ) {
					ally = "<a class=\"profile\" href=\""+Common.buildUrl("details", "module", "allylist", "details", aUser.getAlly().getId()) +"\">"+Common._title(aUser.getAlly().getName())+"</a>";
				}
				else {
					ally = Common._title(aUser.getAlly().getName());
				}
			}

			echo.append("<tr>\n");

			// ID
			echo.append("<td class=\"noBorderX\">").append(Integer.toString(aUser.getId())).append("</td>\n");

			if( comPopup == 0 ) {
				// Diplomatie
				echo.append("<td class=\"noBorderX\"><span class=\"nobr\">\n");

				if( (user != null) && (aUser.getId() != user.getId()) ) {
					switch (relationlist.beziehungZu(aUser))
					{
						case ENEMY:
							echo.append("<img src=\"./data/interface/diplomacy/enemy1.png\" alt=\"\" title=\"Feindlich\" />");
							break;
						case NEUTRAL:
							echo.append("<img src=\"./data/interface/diplomacy/neutral1.png\" alt=\"\" />");
							break;
						case FRIEND:
							echo.append("<img src=\"./data/interface/diplomacy/friend1.png\" alt=\"\" title=\"Feundlich\" />");
							break;
					}

					switch (relationlist.beziehungVon(aUser))
					{
						case ENEMY:
							echo.append("<img src=\"./data/interface/diplomacy/enemy2.png\" alt=\"\" title=\"Feindlich\" />");
							break;
						case NEUTRAL:
							echo.append("<img src=\"./data/interface/diplomacy/neutral2.png\" alt=\"\" />");
							break;
						case FRIEND:
							echo.append("<img src=\"./data/interface/diplomacy/friend2.png\" alt=\"\" title=\"Feundlich\" />");
							break;
					}
				}

				echo.append("</td>\n");

				// Spielername
				if( context.getActiveUser() != null ) {
					echo.append("<td class=\"noBorderX\"><a class=\"profile\" href=\"").append(Common.buildUrl("default", "module", "userprofile", "user", aUser.getId())).append("\">").append(Common._title(aUser.getName())).append("</a>");
				}
				else {
					echo.append("<td class=\"noBorderX\">").append(Common._title(aUser.getName()));
				}
				if( aUser.hasFlag(UserFlag.HIDE) ) {
					echo.append(" <span style=\"color:red;font-style:italic\" title=\"hidden\">[h]</span>");
				}
				if( (user != null) && context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) && aUser.hasFlag(UserFlag.ORDER_MENU) ) {
					echo.append(" <span style=\"color:red;font-style:italic\" title=\"order menu\">[om]</span>");
				}
				echo.append("</span></td>\n");

				// Rasse
				echo.append("<td class=\"noBorderX\">").append(race).append("</td>\n");

				// Signup
				if( aUser.getSignup() != 0 ) {
					echo.append("<td class=\"noBorderX\" align=\"center\">").append(Common.date("j.n.Y H:i", aUser.getSignup())).append("</td>\n");
				}
				else {
					echo.append("<td class=\"noBorderX\" align=\"center\">-</td>\n");
				}

				// Ally
				echo.append("<td class=\"noBorderX\">").append(ally).append("</td>\n");

				// Die Spezial-Admin-Infos anzeigen
				if( (user != null) && context.hasPermission(WellKnownPermission.STATISTIK_ERWEITERTE_SPIELERLISTE) ) {
					echo.append("<td class=\"noBorderX\">").append(Integer.toString(aUser.getInactivity())).append("</td>\n");
					if( !asticount.containsKey(aUser.getId()) ) {
						asticount.put(aUser.getId(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">").append(Integer.toString(asticount.get(aUser.getId()))).append("</td>\n");

					if( !shipcount.containsKey(aUser.getId()) ) {
						shipcount.put(aUser.getId(), 0);
					}
					echo.append("<td class=\"noBorderX\" style=\"text-align:center\">").append(Common.ln(shipcount.get(aUser.getId()))).append("</td>\n");
				}
			}
			else {
				echo.append("<td class=\"noBorderX\"><a style=\"font-size:14px;color:#c7c7c7\" href=\"javascript:playerPM(").append(Integer.toString(aUser.getId())).append(");\">").append(Common._title(aUser.getName())).append("</a></td>\n");
			}

			echo.append("</tr>\n");
		}

		echo.append("</table>\n");
	}
}
