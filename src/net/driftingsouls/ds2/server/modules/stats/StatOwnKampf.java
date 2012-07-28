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

import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLQuery;
import net.driftingsouls.ds2.server.modules.StatsController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

/**
 * Zeigt in Schlachten zerstoerte und verlorene Schiffe an.
 * @author Christopher Jung
 *
 */
public class StatOwnKampf implements Statistic {
	@Override
	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		Database database = context.getDatabase();

		Writer echo = context.getResponse().getWriter();

		/////////////////////////////
		// Zerstoerte Schiffe
		/////////////////////////////

		int counter = 0;

		int destpos = context.getRequest().getParameterInt("destpos");

		int destcount = database.first("SELECT count(distinct tick) as count FROM ships_lost WHERE destowner=",user.getId()).getInt("count");
		if( destcount > 0 ) {
			if( destpos > destcount ) {
				destpos = destcount - 10;
			}

			if( destpos < 0 ) {
				destpos = 0;
			}

			echo.append("Zerst&ouml;rte Schiffe:<br />");
			echo.append("<table class=\"noBorderX\" cellpadding=\"3\" width=\"100%\">\n");
			SQLQuery t = database.query("SELECT distinct tick FROM ships_lost WHERE destowner=",user.getId()," ORDER BY tick DESC LIMIT ",destpos,",10");
			while( t.next() ) {
				int tick = t.getInt("tick");
				SQLQuery s = database.query("SELECT distinct count(*) as count,type,owner FROM ships_lost WHERE destowner=",user.getId()," AND tick=",tick," GROUP BY type,owner");

				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;


				echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; text-align:center\">");
				echo.append(Common.getIngameTime(tick)+"<br />");

				while( s.next() )
				{
					ShipTypeData shiptype = Ship.getShipType( s.getInt("type") );

					int count = s.getInt("count");

					echo.append(count+" ");
					if( shiptype != null ) {
						echo.append("<a target=\"_blank\" onclick='ShiptypeBox.show("+s.getInt("type")+");return false;' " +
								"href=\"./ds?module=schiffinfo&ship="+s.getInt("type")+"\">" +
								shiptype.getNickname()+"</a>");
					}
					else
					{
						echo.append(s.getString("type"));
					}

					User auser = (User)context.getDB().get(User.class, s.getInt("owner"));
					if( auser != null ) {
						echo.append(" von: "+auser.getProfileLink()+"<br />");
					}
					else {
						echo.append("von: Unbekannter Spieler ("+s.getInt("owner")+")<br />");
					}
				}
				echo.append("</td>\n");

				if( (counter % 5) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
				s.free();
			}
			t.free();

			while( counter % 5 != 0 ) {
				echo.append("<td class=\"noBorderX\">&nbsp;</td>");
				counter++;
			}

			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( destpos-10 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "destpos", destpos-10)+"\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">\n");
			if( destpos+10 < destcount ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "destpos", destpos+10)+"\">vor</a>\n");
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

		int lostcount = database.first("SELECT count(distinct tick) as count FROM ships_lost WHERE owner=",user.getId()).getInt("count");
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
			SQLQuery t = database.query("SELECT distinct tick FROM ships_lost WHERE owner=",user.getId()," ORDER BY tick DESC LIMIT ",lostpos,",10");
			while( t.next() ) {
				int tick = t.getInt("tick");
				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;

				echo.append("<td class=\"noBorderX\" style=\"vertical-align:top; text-align:center\">");
				echo.append(Common.getIngameTime(tick)+"<br />");


				SQLQuery s = database.query("SELECT count(*) as count,type,destowner FROM ships_lost WHERE owner=",user.getId()," AND tick=",tick," GROUP BY type,destowner");

				while( s.next() )
				{
					ShipTypeData shiptype = Ship.getShipType( s.getInt("type") );
					int count = s.getInt("count");

					echo.append(count+" ");
					if( shiptype != null ) {
						echo.append("<a target=\"_blank\" onclick='ShiptypeBox.show("+s.getInt("type")+");return false;' " +
								"href=\"./ds?module=schiffinfo&ship="+s.getInt("type")+"\">" +
								shiptype.getNickname()+"</a>");
					}
					else
					{
						echo.append(s.getString("type"));
					}

					User auser = (User)context.getDB().get(User.class, s.getInt("destowner"));

					if( auser != null ) {
						echo.append(auser.getProfileLink()+"<br />");
					}
					else {
						echo.append("Unbekannter Spieler ("+s.getInt("destowner")+")<br />");
					}
				}
				echo.append("</td>\n");

				if( (counter % 5) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
				s.free();
			}
			t.free();
			while( counter % 5 != 0 ) {
				echo.append("<td class=\"noBorderX\">&nbsp;</td>");
				counter++;
			}
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( lostpos-10 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "lostpos", lostpos-10)+"\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">");
			if( lostpos+10 < lostcount ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "lostpos", lostpos+10)+"\">vor</a>\n");
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
