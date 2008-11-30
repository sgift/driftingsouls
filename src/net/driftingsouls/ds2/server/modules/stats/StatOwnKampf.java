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
 * Zeigt in Schlachten zerstoerte und verlorene Schiffe an
 * @author Christopher Jung
 *
 */
public class StatOwnKampf implements Statistic {

	public void show(StatsController contr, int size) throws IOException {
		Context context = ContextMap.getContext();
		User user = (User)context.getActiveUser();
		Database db = context.getDatabase();

		Writer echo = context.getResponse().getWriter();
	
		/////////////////////////////
		// Zerstoerte Schiffe
		/////////////////////////////
	
		int counter = 0;
	
		int destpos = context.getRequest().getParameterInt("destpos");
	
		int destcount = db.first("SELECT count(*) count FROM ships_lost WHERE destowner=",user.getId()).getInt("count");
		if( destcount > 0 ) {
			if( destpos > destcount ) {
				destpos = destcount - 8;
			}
	
			if( destpos < 0 ) {
				destpos = 0;
			}
	
			echo.append("Zerst&ouml;rte Schiffe:<br />");
			echo.append("<table class=\"noBorderX\" cellpadding=\"3\" width=\"100%\">\n");
			SQLQuery s = db.query("SELECT name,type,time,owner FROM ships_lost WHERE destowner=",user.getId()," ORDER BY time DESC LIMIT ",destpos,",8");
			while( s.next() ) {
				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;
				
				ShipTypeData shiptype = Ship.getShipType( s.getInt("type") );
				
				echo.append("<td class=\"noBorderX\" style=\"width:100px; vertical-align:top; text-align:center\">");
				echo.append(Common._plaintitle(s.getString("name"))+"<br />");
				
				if( shiptype != null ) {
					echo.append("<a onmouseover=\"return overlib('"+Common._plaintitle(shiptype.getNickname())+"',TIMEOUT,0,DELAY,400,WIDTH,100);\" " +
							"onmouseout=\"return nd();\" target=\"_blank\" " +
							"href=\"./ds?module=schiffinfo&ship="+s.getInt("type")+"\">" +
							"<img border=\"0\" src=\""+shiptype.getPicture()+"\"></a><br />");
				}
				
				User auser = (User)context.getDB().get(User.class, s.getInt("owner"));
				if( auser != null ) {
					echo.append(auser.getProfileLink()+"<br />");
				}
				else {
					echo.append("Unbekannter Spieler ("+s.getInt("owner")+")<br />");
				}
				echo.append(Common.date("d.m.Y H:i:s",s.getLong("time")));
				echo.append("</td>\n");
				
				if( (counter % 4) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
			}
			s.free();
			
			while( counter % 4 != 0 ) {
				echo.append("<td class=\"noBorderX\" style=\"width:100px\">&nbsp;</td>");
				counter++;
			}
			
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( destpos-8 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "destpos", destpos-8)+"\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");	
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">\n");
			if( destpos+8 < destcount ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "destpos", destpos+8)+"\">vor</a>\n");
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
	
		int lostcount = db.first("SELECT count(*) count FROM ships_lost WHERE owner=",user.getId()).getInt("count");
		if( lostcount > 0 ) {
			if( lostpos > lostcount ) {
				lostpos = lostcount - 8;
			}
	
			if( lostpos < 0 ) {
				lostpos = 0;
			}
			
			if( destcount > 0 ) {
				echo.append("<hr style=\"height:1px; border:0px; background-color:#606060; color:#606060\" />\n");	
			}
	
			echo.append("<br />Verlorene Schiffe:<br />");
			echo.append("<table class=\"noBorderX\" cellpadding=\"3\" width=\"100%\">\n");
			SQLQuery s = db.query("SELECT name,type,time,owner,destowner FROM ships_lost WHERE owner=",user.getId()," ORDER BY time DESC LIMIT ",lostpos,",8");
			while( s.next() ) {
				if( counter == 0 ) {
					echo.append("<tr>");
				}
				counter++;
				
				ShipTypeData shiptype = Ship.getShipType( s.getInt("type") );
				if( shiptype != null ) {
					echo.append("<td class=\"noBorderX\" style=\"width:100px; text-align:center; vertical-align:top\">");
					echo.append(Common._plaintitle(s.getString("name"))+"<br />");
					echo.append("<a onmouseover=\"return overlib('"+Common._plaintitle(shiptype.getNickname())+"',TIMEOUT,0,DELAY,400,WIDTH,100);\" " +
							"onmouseout=\"return nd();\" target=\"_blank\" " +
							"href=\"./ds?module=schiffinfo&ship="+s.getInt("type")+"\">" +
							"<img border=\"0\" src=\""+shiptype.getPicture()+"\"></a><br />");
				}
				User auser = (User)context.getDB().get(User.class, s.getInt("destowner"));

				if( auser != null ) {
					echo.append(auser.getProfileLink()+"<br />");
				}
				else {
					echo.append("Unbekannter Spieler ("+s.getInt("destowner")+")<br />");
				}
				
				echo.append(Common.date("d.m.Y H:i:s",s.getLong("time")));
				echo.append("</td>\n");
				
				if( (counter % 4) == 0 ) {
					echo.append("</tr>\n<tr>");
				}
			}
			s.free();
			while( counter % 4 != 0 ) {
				echo.append("<td class=\"noBorderX\" style=\"width:100px\">&nbsp;</td>");
				counter++;
			}
			echo.append("</tr>\n");
			echo.append("<tr><td class=\"noBorderX\" align=\"left\" colspan=\"2\">\n");
			if( lostpos-8 >= 0 ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "lostpos", lostpos-8)+"\">zur&uuml;ck</a>\n");
			}
			else {
				echo.append("zur&uuml;ck");	
			}
			echo.append("</td>");
			echo.append("<td class=\"noBorderX\" align=\"right\" colspan=\"3\">");
			if( lostpos+8 < lostcount ) {
				echo.append("<a class=\"forschinfo\" href=\""+Common.buildUrl("default", "show", 4, "lostpos", lostpos+8)+"\">vor</a>\n");
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

	public boolean generateAllyData() {
		return false;
	}
	
	public int getRequiredData() {
		return 0;
	}
}
